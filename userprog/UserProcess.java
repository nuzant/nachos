package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.HashMap;
import java.io.EOFException;
import java.util.LinkedList;
import java.lang.Integer;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
    PIDLock.acquire();
	
	fileTable[0] = UserKernel.console.openForReading();
	fileRefRecord.reference(fileTable[0].getName());
	fileTable[1] = UserKernel.console.openForWriting();
	fileRefRecord.reference(fileTable[1].getName());
	
	this.PID = maxPID++;
	PIDLock.release();
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
	
	if (vaddr < 0 || vaddr >= numPages * pageSize)
	    return 0;
	    
	virtualMemoryLock.acquire(); 
	//The read is divided into three parts, to avoid the consistency from reading partial pages
	int amount = Math.min(length, numPages * pageSize - vaddr);
	byte[] mainMemory = Machine.processor().getMemory();
	
	int remainingAmount = amount;
	int successAmount = 0;
	
	//First read
	int addressOffset = Machine.processor().offsetFromAddress(vaddr);
	int numFirstRead = Math.min(remainingAmount, pageSize - addressOffset);
	int ppn = translate(Machine.processor().pageFromAddress(vaddr), readMode);
	if (ppn == -1)	{virtualMemoryLock.release(); return successAmount;}
	int paddr = Machine.processor().makeAddress(ppn, addressOffset);
	
	System.arraycopy(mainMemory, paddr, data, offset, numFirstRead);
	
	remainingAmount -= numFirstRead;
	offset += numFirstRead;
	vaddr += numFirstRead;
	successAmount += numFirstRead;
	
	//Second read for whole pages
	int numWholePages = remainingAmount / pageSize;
	for (int i = 0; i < numWholePages; i++)
	{
		ppn = translate(Machine.processor().pageFromAddress(vaddr), readMode);
		if (ppn == -1)	{virtualMemoryLock.release(); return successAmount;}
		paddr = Machine.processor().makeAddress(ppn, 0);
		
		System.arraycopy(mainMemory, paddr, data, offset, pageSize);
		
		offset += pageSize;
		vaddr += pageSize;
		remainingAmount -= pageSize;
		successAmount += pageSize;
	}
	
	//Third read for partial pages
	ppn = translate(Machine.processor().pageFromAddress(vaddr), readMode);
	if (ppn == -1)	
	{
		virtualMemoryLock.release();
		return successAmount;
	}
	paddr = Machine.processor().makeAddress(ppn, 0);
	
	System.arraycopy(mainMemory, paddr, data, offset, remainingAmount);
	
	successAmount += remainingAmount;
	
	virtualMemoryLock.release();
	
	return successAmount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	if (vaddr < 0 || vaddr >= numPages * pageSize)
	    return 0;
	    
	virtualMemoryLock.acquire();
	
	byte[] mainMemory = Machine.processor().getMemory();

	int amount = Math.min(length, pageSize * numPages - vaddr);
	int remainingAmount = amount;
	int successAmount = 0;
	
	//Dual to the read() case.First write
	int addressOffset = Machine.processor().offsetFromAddress(vaddr);
	int numFirstWritten = Math.min(remainingAmount, pageSize - addressOffset);
	int ppn = translate(Machine.processor().pageFromAddress(vaddr), writeMode);
	if (ppn == -1)	{virtualMemoryLock.release();	return successAmount;}
	int paddr = Machine.processor().makeAddress(ppn, addressOffset);
	
	System.arraycopy(data, offset, mainMemory, paddr, numFirstWritten);
	
	remainingAmount -= numFirstWritten;
	offset += numFirstWritten;
	vaddr += numFirstWritten;
	successAmount += numFirstWritten;
	
	//Second write
	int numWholePages = remainingAmount / pageSize;
	for (int i = 0; i < numWholePages; i++)
	{
		ppn = translate(Machine.processor().pageFromAddress(vaddr), writeMode);
		if (ppn == -1)	{virtualMemoryLock.release();	return successAmount;}
		paddr = Machine.processor().makeAddress(ppn, 0);
		
		System.arraycopy(data, offset, mainMemory, paddr, pageSize);
		
		offset += pageSize;
		vaddr += pageSize;
		successAmount += pageSize;
		remainingAmount -= pageSize;
	}
	
	//Third write
	ppn = translate(Machine.processor().pageFromAddress(vaddr), writeMode);
	if (ppn == -1)	{virtualMemoryLock.release(); return successAmount;}
	paddr = Machine.processor().makeAddress(ppn, 0);
	
	System.arraycopy(data, offset, mainMemory, paddr, remainingAmount);
	
	successAmount += remainingAmount;
	
	virtualMemoryLock.release();
	
	return successAmount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;
	
	//Initialize the page table this process uses
	this.pageTable = new TranslationEntry[numPages];
	LinkedList<Integer> freePages = UserKernel.getFreePages(numPages);
	if (freePages == null)	return false;
	
	for (int i = 0; i < numPages; i++)	
		{pageTable[i] = new TranslationEntry(-1, freePages.pollFirst().intValue(), false, false, false, false);}
	
	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");
		
		int sectionLength = section.getLength();
		int firstVPN = section.getFirstVPN();
	    for (int i=0; i < sectionLength; i++) {
		int vpn = firstVPN + i;
		int ppn = pageTable[nextPageTableEntry].ppn;
		
		//load the page into physical memory
		section.loadPage(i, ppn);
		
		//modify the pageTable
		boolean readOnly = section.isReadOnly();
		pageTable[nextPageTableEntry] = new TranslationEntry(vpn, ppn, true, readOnly, false, false);
		nextPageTableEntry ++;
	    }
	}
	
	return true;
    }
    
    /**
   	 * Translate vpn to its corresponding ppn, using the page table
   	 */
    private int translate(int vpn, int mode)
    {
    	if (vpn < 0)	return -1;
    	
    	for(TranslationEntry te : pageTable)
    	{
    		if (te.vpn == vpn)
    		{
    			if (!te.valid)	return -1;
    			if (mode == writeMode)
    			{
    				if (te.readOnly)	return -1;
    				
    				te.used = true;
    				te.dirty = true;
    				
    				return te.ppn;
    			}
    			else
    			{
    				te.used = true;
    				
    				return te.ppn;
    			}
    		}
    	}
    	return -1;
    }
	
    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
		if (this.PID == 0)
		{
			Machine.halt();
			Lib.assertNotReached("Machine.halt() did not halt machine!");
			return 0;
		}
		return 0;
    }
    
    /**
     * Handle the Create() system call.
     */
    private int handleCreate(int fileNamePointer) {
    	return openFile(fileNamePointer, true);
    }
    
    /**
     * Handle the Open() system call.
     */
    private int handleOpen(int fileNamePointer)
    {
    	return openFile(fileNamePointer, false);
    }
    
    /**
     * Only called by handleOpen() and handleCreate()
     */
    private int openFile(int fileNamePointer, boolean create) {
    	if (!validVirtualAddress(fileNamePointer))
    		return terminate();
    		
    	int descriptor = getDescriptor();
    	if (descriptor == -1)	return -1;
    	
    	String fileName = readVirtualMemoryString(fileNamePointer, maxLengthForString);
    	
    	OpenFile file = UserKernel.fileSystem.open(fileName, create);
    	
    	if(file == null)	return -1;
    	else
    	{
    		int canRef = fileRefRecord.reference(fileName);
    		if (canRef == -1)	return -1;
    	}
    	
    	fileTable[descriptor] = file;
    	
    	return descriptor;
    	
    }
    
    /**
     * handle read()
     */
    private int handleRead (int fileDescriptor, int bufferPointer, int count)
    {
    	if (!validFileDescriptor(fileDescriptor))	return -1;
    	if (!validVirtualAddress(bufferPointer))	return terminate();
    	
    	OpenFile file = fileTable[fileDescriptor];
    	
    	/*Read the contents into the buffer*/
    	byte[] buffer = new byte[count];
    	int numBytesRead = file.read(buffer, 0, count);
    	if (numBytesRead == -1)		return -1;
    	
    	/*Write the contents of buffer into memory*/
    	if (writeVirtualMemory(bufferPointer, buffer, 0, numBytesRead) != numBytesRead)	return -1;
    	
    	return numBytesRead;
    }
    
    /**
     * handle write
     */
    private int handleWrite(int fileDescriptor, int bufferPointer, int count)
    {
    	if (!validFileDescriptor(fileDescriptor))	return -1;
    	if (!validVirtualAddress(bufferPointer))	return terminate();
    	
    	OpenFile file = fileTable[fileDescriptor];
    	byte[] buffer = new byte[count];
    	int numBytesRead = readVirtualMemory(bufferPointer, buffer, 0, count);
    	
    	/*Write the contents to the file*/
    	int numBytesWritten = file.write(buffer, 0, numBytesRead);
    	if (numBytesWritten < numBytesRead)		return -1;
    	
    	return numBytesWritten;
    }
    
    /**
     * handle close()
     */
    private int handleClose(int fileDescriptor)
    {
    	if (!validFileDescriptor(fileDescriptor))	return -1;
    	
    	OpenFile file = fileTable[fileDescriptor];
    	int status = fileRefRecord.unreference(file.getName());
    	fileTable[fileDescriptor].close();
    	fileTable[fileDescriptor] = null;
    	
  		return status == -1? -1 : 0;
    }
    
    /**
     * handle unlink()
     */
    private int handleUnlink(int namePointer)
    {
    	if (!validVirtualAddress(namePointer))	return terminate();
    	String name = readVirtualMemoryString(namePointer, maxLengthForString);
    	
    	for (int i = 0; i < maxNumFiles; i++)
    	{
    		if (fileTable[i].getName() == name)
    		{
    			fileRefRecord.markAsDelete(name);
    			int status = fileRefRecord.unreference(name);	//This will call remove() if necessary
    			
    			fileTable[i].close();
    			fileTable[i] = null;
				if (status == -1)	return -1;
				else	return 0;
    		}
    	}
    	fileRefRecord.markAsDelete(name);
    	if (fileRefRecord.deleteIfNecessary(name))	return 0;
    	else	return -1;
    }
    
    /**
     * check if one filedescriptor is valid
     */
    private boolean validFileDescriptor(int fileDescriptor)
    {
    	return 0 <= fileDescriptor && fileDescriptor < maxNumFiles && fileTable[fileDescriptor] != null;
    }
    
    /**
     * check if one virtualaddress is valid
     */
    private boolean validVirtualAddress(int address)
    {
    	int pageNum = Processor.pageFromAddress(address);
    	return 0 <= pageNum && pageNum < numPages;
    }
    
    private int terminate(){return 0;}		//need caution!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!NOT FINISHED.
 //   {
  //  	handleExit(null);
  //  	return -1;
 //   }
    
    /**
     * get the next available position of pageTable. -1 if none
     */
	private int getDescriptor()
	{
		for (int i = 0; i < maxNumFiles; i++)
		{
			if (fileTable[i] != null)	return i;
		}
		
		return -1;
	}
	
	/**
	 * handle open()
	 */
	private OpenFile handleOpen(String name) {
	
	FileSystem fileSystem = Machine.stubFileSystem();
	
	return fileSystem.open(name, false);
	}

    private static final int
    syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallCreate:
		return handleCreate(a0);
	case syscallOpen:
		return handleOpen(a1);
	case syscallRead:
		return handleRead(a0, a1, a2);
	case syscallWrite:
		return handleWrite(a0, a1, a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);



	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }
    /**
     * Record the number of references to files.
     * Take care of removing or releasing the files
     */
    protected static class fileRefRecord
    {
    	private int numRef = 0;
    	private boolean delete = false;
    	
    	/**
    	 * called when the file gets referenced by a process
    	 */
    	public static int reference(String fileName)
    	{
    		fileRefRecord ref = updateReference(fileName);
    		if (ref.delete)		return -1;
    		ref.numRef ++;
    		finishUpdate();
    		return 1;
    	}
    	
    	/**
    	 * called when the file gets unreferenced by a process
    	 * return -1 when an error occurs, 1 otherwise
    	 */
    	public static int unreference(String fileName)
    	{
    		fileRefRecord ref = updateReference(fileName);
    		ref.numRef --;
    		int status = 1;
    		if (ref.numRef <= 0)
    		{
    			if (ref.delete)		status = ref.delIfNecessary(ref, fileName)? 1 : -1;
    			globalFileReferences.remove(fileName);
    		}
    		finishUpdate();
    		return status;
    	}
    	
    	//This version is called by unreference() only.
    	private static boolean delIfNecessary(fileRefRecord ref, String fileName)
    	{
    		boolean status = false;	//true if delete it
    		if (ref.numRef <= 0)	status = UserKernel.fileSystem.remove(fileName);
    		return status;
    	}
    	
    	/**
    	 * Used to delete files if no process is referencing it
    	 * This version is called by outside world
    	 * Delete if possible, and return whether the file is deleted
    	 */
    	public static boolean deleteIfNecessary(String fileName)
    	{	
    		fileRefRecord ref = updateReference(fileName);
    		boolean status = false;	//true if delete it
    		if (ref.numRef <= 0)	status = UserKernel.fileSystem.remove(fileName);
    		finishUpdate();
    		return status;
    	}
    	
    	/**
    	 * As its name
    	 */
		public static void markAsDelete(String fileName)
		{
			fileRefRecord ref = updateReference(fileName);
			ref.delete = true;
			finishUpdate();
		}
		
		/**
		 * Called when beginning to update the reference table.
		 * return the corresponding fileRefRecord
		 */
    	private static fileRefRecord updateReference(String fileName)
    	{
    		fileRefLock.acquire();
    		fileRefRecord ref = globalFileReferences.get(fileName);
    		if (ref == null)
    		{
    			ref = new fileRefRecord();
    			globalFileReferences.put(fileName, ref);
    		}
    		return ref;
    	}
    	
    	
    	private static void finishUpdate()
    	{
    		fileRefLock.release();
    	}
    	
    	private static HashMap<String, fileRefRecord> globalFileReferences = new HashMap<String, fileRefRecord>();
    	private static Lock fileRefLock = new Lock();
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    
    //Used to store the opened files
    protected OpenFile[] fileTable = new OpenFile[16];
    private static final int maxLengthForString = 256;
    private static final int maxNumFiles = 16;
    
    //Used when initializing pagetable
    private int nextPageTableEntry = 0;
    
    private final int PID;
    public static int maxPID = 0;
    
    private static Lock PIDLock = new Lock();
    private Lock virtualMemoryLock = new Lock();
    
    //Used to indicate the translation mode
    private static int writeMode = 1;
    private static int readMode = -1;
}
