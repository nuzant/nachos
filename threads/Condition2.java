package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	this.conditionLock = conditionLock;
        this.waitQueue = new LinkedList<KThread>();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
    //Last modified by: Tang Boshi, on 23/03/2019
    
    /* Instead of using semaphore, I(Tang) use LinkedList as queue.*/
    
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        
	KThread currentThread = KThread.currentThread();
	boolean intStatus = Machine.interrupt().disable();
	conditionLock.release();
        
	waitQueue.add(currentThread);
 	KThread.sleep();  /*This method automatically gives control to another thread */
    conditionLock.acquire();
    
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();

        if (!waitQueue.isEmpty()){
            ((KThread) waitQueue.removeFirst()).ready();
        }
        Machine.interrupt().restore(intStatus);   
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
    Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    boolean intStatus = Machine.interrupt().disable();

        for(KThread t: waitQueue){
            t.ready();
        }

        waitQueue.clear();
    
    Machine.interrupt().restore(intStatus);
    }

    private LinkedList<KThread>  waitQueue;
    private Lock conditionLock;


	/**
	 * Tests whether this module is working.
	 */
	public static void selfTest() {
		/*
		 * The only way we can test this without crashing the program is with a normal sort of test with KThread sleeping on this (just to test basic functionality)
		 */
		System.out.println("Condition2 Self Test");

		// Verify that thread reacquires lock when woken
		final Lock lock = new Lock();
		final Condition2 cond = new Condition2(lock);
		
		// KThread thread1 = new KThread(new Runnable() {
		// 	public void run() {
		// 		lock.acquire();
		// 		cond.sleep();
				
		// 		// When I wake up, I should hold the lock
		// 		System.out.println((lock.isHeldByCurrentThread() ? "[PASS]" : "[FAIL]") + ": Thread reacquires lock when woken.");
		// 		lock.release();
		// 	}
		// });
		
		// KThread thread2 = new KThread(new Runnable() {
		// 	public void run() {
		// 		lock.acquire();
		// 		cond.wake();
		// 		lock.release();
		// 	}
		// });
		
		// thread1.fork();
		// thread2.fork();
		// thread1.join();
		
		// Verify that wake() wakes up 1 thread
		WakeCounter.wakeups = 0;
		WakeCounter.lock = lock;
		WakeCounter.cond = cond;
		
		new KThread(new WakeCounter()).fork();
		new KThread(new WakeCounter()).fork();
		KThread thread = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				cond.wake();
				lock.release();
			}
		});
		thread.fork();
		thread.join();
		
		System.out.println((WakeCounter.wakeups == 1 ? "[PASS]" : "[FAIL]") + ": Only 1 sleeping thread woken by Condition2.wake(). (" + WakeCounter.wakeups + ")");

		// Verify that wakeAll() wakes up all threads
		WakeCounter.wakeups = 0;
		
		new KThread(new WakeCounter()).fork();
        new KThread(new WakeCounter()).fork();
		//new KThread(new WakeCounter()).fork();
		thread = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				cond.wakeAll();
				lock.release();
			}
		});
		thread.fork();
		thread.join();
		
		// Notice: this should wake up the thread that's still hanging around from the last test, in addition to the new ones.
		System.out.println((WakeCounter.wakeups == 3 ? "[PASS]" : "[FAIL]") + ": All sleeping threads woken by Condition2.wakeAll(). (" + WakeCounter.wakeups + ")");
	}
	
	/**
	 * Test class which increments a static counter when woken
	 */
	static class WakeCounter implements Runnable {
		public static int wakeups = 0;
		public static Lock lock = null;
		public static Condition2 cond = null;
		
		public void run() {
			lock.acquire();
			cond.sleep();
			wakeups++;
			lock.release();
		}
    }   
}