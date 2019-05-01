package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends Scheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }

    protected LThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new LThreadState(thread);
    
        return (LThreadState) thread.schedulingState;
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	    // implement me
	    return new LotteryQueue(transferPriority);
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
        public static final int priorityMaximum = Integer.MAX_VALUE; 

    protected class LotteryQueue extends ThreadQueue{
        LotteryQueue(boolean transferPriority){
            this.transferPriority = transferPriority;
        }

        public int sumEffectivePriority(){
            int sum = 0;
            for(LThreadState t: queue){
                sum += t.getEffectivePriority();
            }
            return sum;
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
        
        protected LThreadState pickNextThread() {
            int sum = this.sumEffectivePriority();
            if(sum == 0) return null;
            double winner = (sum * Math.random());
            int current = 0;
            LThreadState prev = null;
        
            for(LThreadState st: this.queue){
                if(current > winner){
                    return prev;
                }
                prev = st;
                current += st.effectivePriority;
            }
            return prev;
        }
        
        public KThread nextThread(){
            Lib.assertTrue(Machine.interrupt().disabled());

            LThreadState nextThread = pickNextThread();
            if(nextThread == null) return null;
            queue.remove(nextThread);
            nextThread.acquire(this);
            return nextThread.getThread();
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
            String str = "Current content of " + name + ":";
            for(LThreadState ts: queue){
                str += " " + ts.thread.toString();
            }
            Lib.debug('t',str);
        }
        
        public void setName(String str){
            name = str;
        }

        public boolean transferPriority = false;
        public HashSet<LThreadState> queue = new HashSet<LThreadState>();
        public KThread lockingThread = null;
        public String name = "defaultName";
    }
    
    protected class LThreadState{
        public LThreadState(KThread thread){
            this.thread = thread;
		
		    setPriority(priorityDefault);
		    effectivePriority = priority;
        }

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

        private void updateEffectivePriority(){
            //Remove this thread from the queue considering the coming change of priority
            if(waitingIn != null)	waitingIn.queue.remove(this);
            
            int newEffectivePrio = getPriority();

            for(LotteryQueue pq: waitingThis){
                if(pq.transferPriority)
                    newEffectivePrio += pq.sumEffectivePriority();
            }
            

            if(waitingIn != null) {
                waitingIn.queue.add(this);
                
                if(newEffectivePrio != getEffectivePriority()){
                    if(waitingIn.lockingThread != null){
                        getThreadState(waitingIn.lockingThread).updateEffectivePriority();
                    }
                }
            }

            /*if(originalPrio > newEffectivePrio){
                newEffectivePrio = originalPrio;
            }*/

            this.effectivePriority = newEffectivePrio;
        }
        /* duplicate to call updateEffectPriority() in LThreadState */
        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param	priority	the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
            return;
            
            this.priority = priority;
            
            updateEffectivePriority();
        }

        /*A PriorityQueue can only be released by its lockingThread*/
        public void releaseQueue (LotteryQueue priorityQ)
        {
            /*The priorityQ is locked by the thread*/
            if (waitingThis.remove(priorityQ))
            {
                priorityQ.lockingThread = null;
                this.effectivePriority = this.priority;
                updateEffectivePriority();
            }
        }

        public void waitForAccess(LotteryQueue waitQueue) {
            // implement me
            this.releaseQueue(waitQueue);
            
            waitingIn = waitQueue;
            waitQueue.queue.add(this);
            
            if (waitQueue.lockingThread != null)
                getThreadState(waitQueue.lockingThread).updateEffectivePriority();
        }

        public void acquire(LotteryQueue waitQueue) {
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
        protected HashSet<LotteryQueue> waitingThis = new HashSet<LotteryQueue>();
        /** The priority queue that this thread is waiting in. */
        protected LotteryQueue waitingIn = null;
        /** The thread with which this object is associated. */	   
        protected KThread thread;
        /** The priority and effective priority of the associated thread. */
        protected int priority;
        protected int effectivePriority;
    }
}
