package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMaximum)
	    return false;

	setPriority(thread, priority+1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    public boolean decreasePriority() {
	boolean intStatus = Machine.interrupt().disable();
		       
	KThread thread = KThread.currentThread();

	int priority = getPriority(thread);
	if (priority == priorityMinimum)
	    return false;

	setPriority(thread, priority-1);

	Machine.interrupt().restore(intStatus);
	return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null)
	    thread.schedulingState = new ThreadState(thread);

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */


/*******************************************************************************************/




    protected class PriorityQueue extends ThreadQueue {
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}

	/**
	 * Acquire the maximum effective priority value in this queue
	 */

	public int maxEffectivePriority(){
		if(queue.peek() != null) return queue.peek().effectivePriority;
		else return -1;
	}

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
		getThreadState(thread).waitForAccess(this);

		//debug info
		Lib.debug('t',thread.toString() + " calling waitForAccess in " + name);
		print();
	}

	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
		getThreadState(thread).acquire(this);

		//debug info
		Lib.debug('t',thread.toString() + " calling acquire wrt " + name);
		print();
	}

	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
		// implement me

		ThreadState nextTS = queue.poll();
		if(nextTS == null) return null;
		nextTS.acquire(this);

		//debug info
		Lib.debug('t',name + " calling nextThread, next thread is " + nextTS.getThread().toString());
		print();

	    return nextTS.getThread();
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.protected
	 */
	protected ThreadState pickNextThread() {
		// implement me
	    return queue.peek();
	}
	
	public void print() {
	    Lib.assertTrue(Machine.interrupt().disabled());
		// implement me (if you want)
		String str = "Current content of " + name + ":";
		for(ThreadState ts: queue){
			str += " " + ts.thread.toString();
		}
		Lib.debug('t',str);
	}
	
	public void setName(String str){
		name = str;
	}

	/**
	 * A treemap that contains all waiting threads in the queue, sorted with priority.
	 */

	public java.util.PriorityQueue<ThreadState> queue = new java.util.PriorityQueue<ThreadState>();

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
	
	public boolean transferPriority;
	public KThread lockingThread = null;
	public String name = "defaultName";
    }

    
    
    

    
/*********************************************************************************************************/    
    
    
    
    
    
    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState implements Comparable<ThreadState>{
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 * 
	 * Modified into a comparable object to compare the priority, in order to implement the queue.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
		
		setPriority(priorityDefault);
		effectivePriority = priority;
	}

	@Override
	public int compareTo(ThreadState other){
		if(effectivePriority > other.effectivePriority) return -1;
		else if(effectivePriority < other.effectivePriority) return 1;
		else {
			if(time > other.time) return 1;
			else if (time < other.time) return -1;
			else return 0;
		}
	}
	
	@Override
	public boolean equals(Object other){
		if(other == null || !(other instanceof ThreadState)) return false;

		ThreadState o = (ThreadState) other;
		return this.thread.compareTo(o.thread) == 0;
	}
	/**
	 * Get this thread.
	 */

	public KThread getThread(){
		return thread;
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {
	    // implement me
	    return effectivePriority;
	}

	/**
	 * Update the effective priority
	 */

	private void updateEffectivePriority(){
		/*TangBoshi version
		*/
		
		//Remove this thread from the queue considering the coming change of priority
		if(waitingIn != null)	waitingIn.queue.remove(this);
		
		//To see if we need change
		int myPriority = this.getPriority();
		int myEf = this.getEffectivePriority();
		this.effectivePriority = myPriority;
		for (PriorityQueue priorityQ : waitingThis)
		{
			if (priorityQ.transferPriority)
			{
				ThreadState firstTS = priorityQ.queue.peek();
				if (firstTS != null)
				{	
					int firstTSPriority = firstTS.getEffectivePriority();
					if (firstTSPriority > myPriority && firstTSPriority > this.getEffectivePriority())
					{
						this.effectivePriority = firstTSPriority;
					}
				}
			}
		}
		
		if(waitingIn != null) {
			waitingIn.queue.add(this);
			
			boolean needToPropagate = false;
			if (myEf != this.getEffectivePriority())   needToPropagate = true;
			
			if (needToPropagate)	//undergoes value change
			{
				ThreadState lockingState = getThreadState(waitingIn.lockingThread);
				if (lockingState != null && waitingIn.transferPriority)
					lockingState.updateEffectivePriority();
			}
		}
	}
		
		
		
		/*
		// update for setPriority()
		effectivePriority = java.lang.Math.max(effectivePriority, priority);
		// update for acquire()
		if(waitingIn!=null){
			waitingIn.queue.remove(this);
			int tmp = effectivePriority;
			for(PriorityQueue q: waitingThis){
				if(q.transferPriority && q.maxEffectivePriority()>tmp){
					tmp = q.maxEffectivePriority();
				}
			}
			effectivePriority = tmp;
			waitingIn.queue.add(this);
		}*/
	

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    if (this.priority == priority)
		return;
	    
		this.priority = priority;
		
	    // implement me
	    updateEffectivePriority();
	}

	
	
	/*A PriorityQueue can only be released by its lockingThread*/
	public void releaseQueue (PriorityQueue priorityQ)
	{
		/*The priorityQ is locked by the thread*/
		if (waitingThis.remove(priorityQ))
		{
			priorityQ.lockingThread = null;
			
			updateEffectivePriority();
		}
	}
	
	
	/**
	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
	 * the associated thread) is invoked on the specified priority queue.
	 * The associated thread is therefore waiting for access to the
	 * resource guarded by <tt>waitQueue</tt>. This method is only called
	 * if the associated thread cannot immediately obtain access.
	 *
	 * @param	waitQueue	the queue that the associated thread is
	 *				now waiting on.
	 *
	 * @see	nachos.threads.ThreadQueue#waitForAccess
	 */
	public void waitForAccess(PriorityQueue waitQueue) {
		// implement me
		this.releaseQueue(waitQueue);
		
		waitingIn = waitQueue;
		time = Machine.timer().getTime();
		waitQueue.queue.add(this);
		
		if (waitQueue.lockingThread != null)
			getThreadState(waitQueue.lockingThread).updateEffectivePriority();
	}

	/**
	 * Called when the associated thread has acquired access to whatever is
	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
	 * <tt>thread</tt> is the associated thread), or as a result of
	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
	 *
	 * @see	nachos.threads.ThreadQueue#acquire
	 * @see	nachos.threads.ThreadQueue#nextThread
	 */
	 
	 
	public void acquire(PriorityQueue waitQueue) {
		// implement me
		if (waitQueue.lockingThread != null)
			getThreadState(waitQueue.lockingThread).releaseQueue(waitQueue);
		
		waitQueue.lockingThread = this.thread;
		waitingThis.add(waitQueue);
		waitQueue.queue.remove(this);
		if (waitQueue == waitingIn)		waitingIn = null;
		
		updateEffectivePriority();
	}	
	/** The priority queue waiting for this thread, when this thread acquired the access. */
	private HashSet<PriorityQueue> waitingThis = new HashSet<PriorityQueue>();
	/** The priority queue that this thread is waiting in. */
	private PriorityQueue waitingIn = null;
	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority and effective priority of the associated thread. */
	protected int priority;
	protected int effectivePriority;
	/** The length of time the threads has been waiting */
	public long time;
	}
	



/*****************************************************************************************************************/	
	/*
	public static void selfTest() {
		ThreadQueue tq1 = ThreadedKernel.scheduler.newThreadQueue(true), tq2 = ThreadedKernel.scheduler.newThreadQueue(true), tq3 = ThreadedKernel.scheduler.newThreadQueue(true);
		KThread kt_1 = new KThread(), kt_2 = new KThread(), kt_3 = new KThread(), kt_4 = new KThread();
		
		kt_1.setName("schT1");
		kt_2.setName("schT2");
		kt_3.setName("schT3");
		kt_4.setName("schT4");
		boolean status = Machine.interrupt().disable();
		
		tq1.waitForAccess(kt_1);
		tq2.waitForAccess(kt_2);
		tq3.waitForAccess(kt_3);
		
		tq1.acquire(kt_2);
		tq2.acquire(kt_3);
		tq3.acquire(kt_4);
		
		ThreadedKernel.scheduler.setPriority(kt_1, 6);
		
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_4)==6);
		
		KThread kt_5 = new KThread();
		
		ThreadedKernel.scheduler.setPriority(kt_5, 7);
		
		tq1.waitForAccess(kt_5);
		
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_4)==7);
		
		tq1.nextThread();
		
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_4)==1);
		
		Machine.interrupt().restore(status);
	}*/
}
