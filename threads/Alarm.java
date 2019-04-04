package nachos.threads;

import nachos.machine.*;
import java.util.TreeMap;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
		sleepingThreads = new TreeMap<Long, KThread>();
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    /* Last modified by: TangBoshi, on 19/03/2019 */
    /* I(Tang) implement this method so that this function maintains the heap (sleepingThreads)
	and get those threads whose waiting time has expired into the ready queue.*/
	
	boolean intStatus = Machine.interrupt().disable();
	long currentTime = Machine.timer().getTime();
	while (!sleepingThreads.isEmpty() && this.sleepingThreads.firstKey() <= currentTime)
		this.sleepingThreads.pollFirstEntry().getValue().ready();	/*Do periodic house keeping */
	
    KThread.currentThread().yield();
    Machine.interrupt().restore(intStatus);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	//Last modified by: Tang Boshi, on 19/03/2019
	/* I(Tang) reimplement this method so that any thread calling this method will be
	 * put in the sleepingThreads and go to sleep. The waking part is guaranteed by 
	 * timerInterrupt handler
	 */
	long wakeTime = Machine.timer().getTime() + x;
    boolean intStatus = Machine.interrupt().disable();
        
    this.sleepingThreads.put(wakeTime, KThread.currentThread());
    KThread.sleep();
    Machine.interrupt().restore(intStatus);
        
    }
    
    private TreeMap<Long, KThread> sleepingThreads;
    // self test method

    public static void selfTest(){
        KThread t = new KThread(new Runnable(){
            public void run(){
                System.out.println("I am awake");
            }
        });
        
        t.fork();
        ThreadedKernel.alarm.waitUntil(100);
    }
}