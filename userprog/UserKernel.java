package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.LinkedList;
import java.lang.Integer;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	console = new SynchConsole(Machine.console());
	
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
	
	for (int pageIndex = 0; pageIndex < Machine.processor().getNumPhysPages(); pageIndex++)
		freePhysicalPages.add(new TranslationEntry(0, pageIndex, false, false, false, false));
	numRemainingPages = Machine.processor().getNumPhysPages();
		
	memoryLock = new Lock();
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }
	/**
	 * Allocate numPages of free physical pages as table
	 * return the table containing the ppns.Null if not enough space
	 */
	public TranslationEntry[] getFreePages(int numPages)
	{
		TranslationEntry[] returnPages = null;
		
		memoryLock.acquire();
		if (numRemainingPages < numPages)	
		{
			memoryLock.release();	return null;
		}
		
		returnPages = new TranslationEntry[numPages];
		
		for (int i = 0; i < numPages; i++)
		{
			returnPages[i] = freePhysicalPages.remove();
			returnPages[i].valid = true;
		}
		
		numRemainingPages -= numPages;
		
		memoryLock.release();
		
		return returnPages;
    }
    /**
     * Free the pages in pageTable
     */
    public void freePages(TranslationEntry[] pageTable){
        memoryLock.acquire();
        for(TranslationEntry pte: pageTable){
            freePhysicalPages.add(pte);
            pte.valid = false;
        }
        memoryLock.release();
    }
	
    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }
 
    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

	private LinkedList<TranslationEntry> freePhysicalPages = new LinkedList<TranslationEntry>();
	private int numRemainingPages;
    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
    private Lock memoryLock;
}
