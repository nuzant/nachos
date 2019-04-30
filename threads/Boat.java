package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.*;
import nachos.threads.*;

public class Boat {
  static BoatGrader bg;

  static final int O = 234245; // Some random integer constant
  static final int M = O + 1;

  static int boat = O;
  static int passenger = 0;
  static Lock boatLock = new Lock();
  static Condition waitO = new Condition(boatLock);
  static Condition waitM = new Condition(boatLock);
  static Condition waitB = new Condition(boatLock); // Waiting on boat
  static Communicator endTest = new Communicator();

  static int Ochild = 0;
  static int Oadult = 0;
  static int Mchild = 0;
  static int Madult = 0;
  static int endNumber = 0;

  public static void selfTest() {
    BoatGrader b = new BoatGrader();
        begin(0, 2, b);
        begin(5, 2, b);
        begin(10, 2, b);
        begin(0, 5, b);
        begin(5, 5, b);
        begin(10, 5, b);
        begin(0, 10, b);
        begin(5, 10, b);
        begin(10, 10, b);
  }

  public static void begin( int adults, int children, BoatGrader b ) {
    bg = b;
    Ochild = 0; Oadult = 0; Mchild = 0; Madult = 0;
    endNumber = children + adults;
    boolean last = false;

    Runnable child = new Runnable() {
      public void run () {
        int location = O;
        ChildItinerary(location);
      }
    };

    Runnable adult = new Runnable() {
      public void run () {
        int location = O;
        AdultItinerary(location, last);
      }
    };
/*    Runnable rhookO = new Runnable() {
      public void run () {
        hookOMethod();
      }
    };
    Runnable rhookM = new Runnable() {
      public void run () {
        hookMMethod();
      }
    };
    KThread hookO = new KThread(rhookO);
    hookO.setName("myhookO");
    hookO.fork();

    KThread hookM = new KThread(rhookM);
    hookM.setName("myhookM");
    hookM.fork();
*/
    for (int i = 0; i < children; i++) {
      KThread childThread = new KThread(child);
      childThread.setName("Child Thread #" + (i+1));
      childThread.fork();
    }
    for (int i = 0; i < adults; i++) {
      KThread adultThread = new KThread(adult);
      adultThread.setName("Adult Thread #" + (i+1));
      adultThread.fork();
    }



    while(true) {
      int receiver = endTest.listen();
      //System.out.println("testing end" + receiver);
      if (receiver == adults + children) return; // To be edited
    }
  }

/*  static void hookOMethod() {
    //System.out.println("hookO forked");
    boatLock.acquire();
    while (true) {
      waitO.wakeAll();
      waitO.sleep();
    }
    //boatLock.release();
  }

  static void hookMMethod(){
    //System.out.println("hookM forked");
    boatLock.acquire();
    while (true) {
      waitM.wakeAll();
      waitM.sleep();
    }
    //boatLock.release();
  }*/

  static void AdultItinerary(int location, boolean last) {
    bg.initializeAdult();
    String name = KThread.currentThread().getName();
    boatLock.acquire();
    //System.out.println(name + " acquire");
    Oadult++;
    while (true) {
      if (location == O) {
        while (Ochild > 1 || boat == M || passenger > 0) {
          //System.out.println(name + " sleep0");
          waitO.sleep();
        }
        //System.out.println(name + " wake1");
        bg.AdultRowToMolokai();

        Oadult--; Madult++;
        location = M;
        boat = M;

        endTest.speak(Mchild + Madult);
        //System.out.println(name + " sleep2");
        waitM.wakeAll();
        waitM.sleep();
      }

      if (location == M) {
        if (boat == M && Madult < endNumber && Mchild == 0) {
          bg.AdultRowToOahu();
          Madult--; Oadult++;
          location = O; boat = O;
          //System.out.println(name + " sleep2");
          waitO.wakeAll();

          waitO.sleep();
        }
        else {
          //System.out.println(name + " sleep3");
          waitM.sleep();
        }
      }


      else {
        if (false) {break;}
      }
    }
    boatLock.release();
  }

  static void ChildItinerary(int location) {
    bg.initializeChild();
    String name = KThread.currentThread().getName();
    boatLock.acquire();
    //System.out.println(name + " acquire");
    Ochild++;
    while (true) {
      if (location == M) { // Sail back to O
        //System.out.println(name + " conditionM");
        Lib.assertTrue(Mchild > 0);
        while (boat != M) { waitM.sleep(); }
        Mchild--;
        bg.ChildRowToOahu();

        location = O;
        Ochild++;
        boat = O;


        //System.out.println(name + " wakeall");
        waitO.wakeAll();
        //System.out.println(name + " sleep");

        waitO.sleep();
        //System.out.println(name + " wake");
      }

      if (location == O) {
        while (boat != O || passenger > 1 || (Oadult > 0 && Ochild == 1)) {
          //System.out.println(name + " condition1");
          if (boat == M) waitM.wakeAll();
          //System.out.println(name + " sleep1");
          waitO.sleep();
          //System.out.println(name + " wake1");
        }

        waitO.wakeAll();
        //Lib.assertTrue(Oadult == 0);

        if (Oadult == 0 && Ochild == 1) {
          Ochild--;
          bg.ChildRowToMolokai();

          location = M;
          Mchild++;
          boat = M;
          passenger = 0;

          endTest.speak(Mchild + Madult);
          if (Mchild == 0 && Madult == 0) continue;
          //System.out.println(name + " wakeall2");
          waitO.wakeAll();
          //System.out.println(name + " sleep2");
          waitM.sleep();
          //System.out.println(name + " wake2");
        }

        if (Ochild > 1 && location == O) { // send children to M
          passenger++;
          if (passenger == 1) {
            waitB.sleep();
            Ochild--;
            bg.ChildRowToMolokai();

            location = M;
            Mchild++;
            //System.out.println("Mchild" + Mchild);

            Lib.assertTrue(boatLock.isHeldByCurrentThread());
            waitB.wake();
            //System.out.println(name + " sleep3");
            waitM.sleep();
          }

          if (passenger == 2) {
            waitB.wake();
            waitB.sleep();

            Ochild--;
            //System.out.println(name);
            bg.ChildRideToMolokai();

            location = M;
            Mchild++;
            boat = M;
            passenger = 0;
            //System.out.println("Mchild" + Mchild);
            endTest.speak(Mchild + Madult);
            waitM.wakeAll();
            waitM.sleep();
          }
        }
      }

      else {
        if (false) {break;}
      }
    }
    boatLock.release();
  }
}
