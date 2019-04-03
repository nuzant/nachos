package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /*Last edited by: Tang Boshi, on 23/03/2019*/
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    this.waitSpeakerQueue = new Condition2(this.conditionLock);
    this.waitListenerQueue = new Condition2(this.conditionLock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
     
     /*(Edited by Tang) The speaker will first check if somebody is waiting for him. If so, the speaker will wake 
       a listener, leave the message on Communicator, and go to sleep. When the listener receives 
       the message, he will wake up the speaker and returns from Communicator. Otherwise, the speaker
       will release the lock and go to sleep. */
    public void speak(int word) {
    
    boolean intStatus = Machine.interrupt().disable();
    
    this.conditionLock.acquire();
    
    while (this.activeListener == null && this.waitListener == 0)
    {
        this.waitSpeaker += 1;
        this.waitSpeakerQueue.sleep();
        this.waitSpeaker -= 1;
    }
    
    this.channel = word;
    if (this.activeListener == null)
        this.waitListenerQueue.wake();
    else
        this.activeListener.ready();
            
    this.sleepingSpeaker = KThread.currentThread();
    KThread.sleep();
    this.sleepingSpeaker = null;
    
    this.conditionLock.release();
    Machine.interrupt().restore(intStatus);
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
     
     /*(Edited by Tang) */
    public int listen() {
    
    boolean intStatus = Machine.interrupt().disable();
    
    this.conditionLock.acquire();
    
    while (waitSpeaker == 0 && sleepingSpeaker == null)
    {
        waitListener += 1;
        waitListenerQueue.sleep();
        waitListener -= 1;
    }
    
    if (sleepingSpeaker != null)
        sleepingSpeaker.ready();
    else
    {   
        activeListener = KThread.currentThread();
        waitSpeakerQueue.wake();
        KThread.sleep();
        activeListener = null;
        
        sleepingSpeaker.ready();
    }
    this.conditionLock.release();
    Machine.interrupt().restore(intStatus);
	return this.channel;
    }
    private int waitSpeaker = 0;
    private int waitListener = 0;
    private KThread sleepingSpeaker = null;
    private KThread activeListener = null;
    private Lock conditionLock = new Lock();
    private Condition2 waitSpeakerQueue;
    private Condition2 waitListenerQueue;
    private int channel;
}