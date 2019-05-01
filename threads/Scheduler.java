package nachos.threads;

import nachos.machine.*;

/**
 * Coordinates a group of thread queues of the same kind.
 *
 * @see	nachos.threads.ThreadQueue
 */
public abstract class Scheduler {
    /**
     * Allocate a new scheduler.
     */
    public Scheduler() {
    }
    
    /**
     * Allocate a new thread queue. If <i>transferPriority</i> is
     * <tt>true</tt>, then threads waiting on the new queue will transfer their
     * "priority" to the thread that has access to whatever is being guarded by
     * the queue. This is the mechanism used to partially solve priority
     * inversion.
     *
     * <p>
     * If there is no definite thread that can be said to have "access" (as in
     * the case of semaphores and condition variables), this parameter should
     * be <tt>false</tt>, indicating that no priority should be transferred.
     *
     * <p>
     * The processor is a special case. There is clearly no purpose to donating
     * priority to a thread that already has the processor. When the processor
     * wait queue is created, this parameter should be <tt>false</tt>.
     *
     * <p>
     * Otherwise, it is beneficial to donate priority. For example, a lock has
     * a definite owner (the thread that holds the lock), and a lock is always
     * released by the same thread that acquired it, so it is possible to help
     * a high priority thread waiting for a lock by donating its priority to
     * the thread holding the lock. Therefore, a queue for a lock should be
     * created with this parameter set to <tt>true</tt>.
     *
     * <p>
     * Similarly, when a thread is asleep in <tt>join()</tt> waiting for the
     * target thread to finish, the sleeping thread should donate its priority
     * to the target thread. Therefore, a join queue should be created with
     * this parameter set to <tt>true</tt>.
     *
     * @param	transferPriority	<tt>true</tt> if the thread that has
     *					access should receive priority from the
     *					threads that are waiting on this queue.
     * @return	a new thread queue.
     */
    public abstract ThreadQueue newThreadQueue(boolean transferPriority);

    /**
     * Get the priority of the specified thread. Must be called with
     * interrupts disabled.
     *
     * @param	thread	the thread to get the priority of.
     * @return	the thread's priority.
     */
    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
	return 0;
    }

    /**
     * Get the priority of the current thread. Equivalent to
     * <tt>getPriority(KThread.currentThread())</tt>.
     *
     * @return	the current thread's priority.
     */
    public int getPriority() {
	return getPriority(KThread.currentThread());
    }

    /**
     * Get the effective priority of the specified thread. Must be called with
     * interrupts disabled.
     *
     * <p>
     * The effective priority of a thread is the priority of a thread after
     * taking into account priority donations.
     *
     * <p>
     * For a priority scheduler, this is the maximum of the thread's priority
     * and the priorities of all other threads waiting for the thread through a
     * lock or a join.
     *
     * <p>
     * For a lottery scheduler, this is the sum of the thread's tickets and the
     * tickets of all other threads waiting for the thread through a lock or a
     * join.
     *
     * @param	thread	the thread to get the effective priority of.
     * @return	the thread's effective priority.
     */
    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
	return 0;
    }

    /**
     * Get the effective priority of the current thread. Equivalent to
     * <tt>getEffectivePriority(KThread.currentThread())</tt>.
     *
     * @return	the current thread's priority.
     */
    public int getEffectivePriority() {
	return getEffectivePriority(KThread.currentThread());
    }

    /**
     * Set the priority of the specified thread. Must be called with interrupts
     * disabled.
     *
     * @param	thread	the thread to set the priority of.
     * @param	priority	the new priority.
     */
    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
    }

    /**
     * Set the priority of the current thread. Equivalent to
     * <tt>setPriority(KThread.currentThread(), priority)</tt>.
     *
     * @param	priority	the new priority.
     */
    public void setPriority(int priority) {
	setPriority(KThread.currentThread(), priority);
    }

    /**
     * If possible, raise the priority of the current thread in some
     * scheduler-dependent way.
     *
     * @return	<tt>true</tt> if the scheduler was able to increase the current
     *		thread's
     *		priority.
     */
    public boolean increasePriority() {
	return false;
    }

    /**
     * If possible, lower the priority of the current thread user in some
     * scheduler-dependent way, preferably by the same amount as would a call
     * to <tt>increasePriority()</tt>.
     *
     * @return	<tt>true</tt> if the scheduler was able to decrease the current
     *		thread's priority.
     */
    public boolean decreasePriority() {
	return false;
    }

    /**
     * self test method
     */

    public static void selfTest() {
        System.out.println("\nEntering selfTest() for LotteryScheduler....\n");
        
		ThreadQueue tq1 = ThreadedKernel.scheduler.newThreadQueue(true), tq2 = ThreadedKernel.scheduler.newThreadQueue(true), tq3 = ThreadedKernel.scheduler.newThreadQueue(true);
		KThread kt_1 = new KThread(), kt_2 = new KThread(), kt_3 = new KThread(), kt_4 = new KThread();
        tq1.setName("queue1");
        tq2.setName("queue2");
        tq3.setName("queue3");
		kt_1.setName("T1");
		kt_2.setName("T2");
		kt_3.setName("T3");
		kt_4.setName("T4");
		boolean status = Machine.interrupt().disable();
        
        System.out.println(tq1.getClass().toString());

		tq1.waitForAccess(kt_1);
		tq2.waitForAccess(kt_2);
		tq3.waitForAccess(kt_3);
        
        ThreadedKernel.scheduler.setPriority(kt_1, 10);
        ThreadedKernel.scheduler.setPriority(kt_2, 10);
        ThreadedKernel.scheduler.setPriority(kt_3, 10);
        ThreadedKernel.scheduler.setPriority(kt_4, 10);

        System.out.println("t1:"+ThreadedKernel.scheduler.getEffectivePriority(kt_1));
        System.out.println("t2:"+ThreadedKernel.scheduler.getEffectivePriority(kt_2));
        System.out.println("t3:"+ThreadedKernel.scheduler.getEffectivePriority(kt_3));
        System.out.println("t4:"+ThreadedKernel.scheduler.getEffectivePriority(kt_4));
        //System.out.println("t5:"+ThreadedKernel.scheduler.getEffectivePriority(kt_5));

		tq1.acquire(kt_2);
		tq2.acquire(kt_3);
        tq3.acquire(kt_4);
        
        System.out.println("t1:"+ThreadedKernel.scheduler.getEffectivePriority(kt_1));
        System.out.println("t2:"+ThreadedKernel.scheduler.getEffectivePriority(kt_2));
        System.out.println("t3:"+ThreadedKernel.scheduler.getEffectivePriority(kt_3));
        System.out.println("t4:"+ThreadedKernel.scheduler.getEffectivePriority(kt_4));
        //System.out.println("t5:"+ThreadedKernel.scheduler.getEffectivePriority(kt_5));
        
        //System.out.println(ThreadedKernel.scheduler.getEffectivePriority(kt_4));
		//Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_4)==40);
		
        KThread kt_5 = new KThread();
        kt_5.setName("T5");
		
		ThreadedKernel.scheduler.setPriority(kt_5, 100);
		
        tq1.waitForAccess(kt_5);
        
        System.out.println("t1:"+ThreadedKernel.scheduler.getEffectivePriority(kt_1));
        System.out.println("t2:"+ThreadedKernel.scheduler.getEffectivePriority(kt_2));
        System.out.println("t3:"+ThreadedKernel.scheduler.getEffectivePriority(kt_3));
        System.out.println("t4:"+ThreadedKernel.scheduler.getEffectivePriority(kt_4));
        System.out.println("t5:"+ThreadedKernel.scheduler.getEffectivePriority(kt_5));
        
		//Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_2)==110);
		
        tq1.nextThread();
        
        System.out.println("t1:"+ThreadedKernel.scheduler.getEffectivePriority(kt_1));
        System.out.println("t2:"+ThreadedKernel.scheduler.getEffectivePriority(kt_2));
        System.out.println("t3:"+ThreadedKernel.scheduler.getEffectivePriority(kt_3));
        System.out.println("t4:"+ThreadedKernel.scheduler.getEffectivePriority(kt_4));
        System.out.println("t5:"+ThreadedKernel.scheduler.getEffectivePriority(kt_5));
		//Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_4)==1);
        //KThread.finish();
        Machine.interrupt().restore(status);
        System.out.println("\nExiting.....\n");

        
    }
}
