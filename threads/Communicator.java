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
    this.speakerCond = new Condition2(this.lock);
    this.listenerCond = new Condition2(this.lock);
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
     
    public void speak(int word) {
        lock.acquire();

        while(waitListener == 0 || fullcache){
            speakerCond.sleep();
        }
        cache = word; 
        fullcache = true;

        listenerCond.wake();
        lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
     
     /*(Edited by Tang) */
    public int listen() {
        lock.acquire();
        waitListener++;
        speakerCond.wake();
        listenerCond.sleep();

        int word = cache;
        fullcache = false;
        waitListener--;

        speakerCond.wake();
        lock.release();

        return word;
    }

    private int waitListener = 0;
    private Lock lock = new Lock();
    private Condition2 speakerCond;
    private Condition2 listenerCond;
    private int cache;
    private boolean fullcache;
}