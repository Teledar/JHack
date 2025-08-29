/**
 * A library that supports various program execution services.
 */
public class Sys {

	// This is set to true when an error occurs, to prevent Sys.error from starting a runaway
	// chain of recursion
	private static boolean errorMode = false;
	
    /** Performs all the initializations required by the OS. */
    public static short init() {
        Memory.init();
        Math.init();
        Screen.init();
        Output.init();
        Keyboard.init();
        Main.main();
        Sys.halt();
        return 0;
    }

    /** Halts the program execution. */
    public static short halt() {
    	throw new RuntimeException("System halted");
        /*
        while (true) {
        	try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
			}
        } */
    }

    /** Waits approximately duration milliseconds and returns.  */
    public static short wait(short duration) {
    	
        if (duration < 1) {
            error((short) 1);
        }

        try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        return 0;
    }

    /** Displays the given error code in the form "ERR [errorCode]",
     *  and halts the program's execution. */
    public static short error(short errorCode) {
    	
    	if (!errorMode) {
    		
    		// Set the error mode to true so that calls to Output functions don't call Sys.error
    		// and start runaway recursion
    		errorMode = true;
    		
        	// Since it's possible that the Output class is causing the error, write "ERR " manually
        	
        	// Write "ER" at the beginning of row 22
        	HackComputer.poke(31 * 256 + 63, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 0);
        	HackComputer.poke(51 * 256 + 51, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 1);
        	HackComputer.poke(51 * 256 + 35, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 2);
        	HackComputer.poke(51 * 256 + 11, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 3);
        	HackComputer.poke(31 * 256 + 15, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 4);
        	HackComputer.poke(27 * 256 + 11, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 5);
        	HackComputer.poke(51 * 256 + 35, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 6);
        	HackComputer.poke(51 * 256 + 51, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 7);
        	HackComputer.poke(51 * 256 + 63, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 8);
        	
        	// Write "R " next
        	HackComputer.poke(31, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 0 + 1);
        	HackComputer.poke(51, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 1 + 1);
        	HackComputer.poke(51, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 2 + 1);
        	HackComputer.poke(51, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 3 + 1);
        	HackComputer.poke(31, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 4 + 1);
        	HackComputer.poke(27, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 5 + 1);
        	HackComputer.poke(51, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 6 + 1);
        	HackComputer.poke(51, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 7 + 1);
        	HackComputer.poke(51, HackComputer.SCREEN + 22 * 11 * 32 + 32 * 8 + 1);

        	// Write the error code; avoid using Output.printString since this may cause a memory error
            Output.moveCursor((short) 22, (short) 4);
            if (errorCode > 19) {
                Output.printChar((short) 50); //2
            }
            else if (errorCode > 9) {
                Output.printChar((short) 49); //1
            }
            Output.printChar((short) (48 + errorCode % 10));
        
    	}

        // Since we're using the JVM, we get to throw exceptions. This displays the stack trace, making
        // it easier to debug Jack programs
        switch (errorCode) {
        case 1:
        	throw new IllegalArgumentException("Duration must be positive");
        case 2:
        	throw new IllegalArgumentException("Array size must be positive");
        case 3:
        	throw new ArithmeticException("Division by zero");
        case 4:   
        	throw new ArithmeticException("Cannot compute square root of a negative number");
        case 5:
        	throw new IllegalArgumentException("Allocated memory size must be positive");
        case 6:
        	throw new OutOfMemoryError("Heap overflow");
        case 7:
        	throw new IllegalArgumentException("Illegal pixel coordinates");
        case 8:
        	throw new IllegalArgumentException("Illegal line coordinates");
        case 9:
        	throw new IllegalArgumentException("Illegal rectangle coordinates");
        case 12:
        	throw new IllegalArgumentException("Illegal center coordinates");
        case 13:
        	throw new IllegalArgumentException("Illegal radius");
        case 14:
        	throw new IllegalArgumentException("Maximum length must be non-negative");
        case 15:
        case 16:
        	throw new ArrayIndexOutOfBoundsException("String index out of bounds");
        case 17:
        	throw new ArrayIndexOutOfBoundsException("String is full");
        case 18:
        	throw new ArrayIndexOutOfBoundsException("String is empty");
        case 19:
        	throw new ArrayIndexOutOfBoundsException("Insufficient string capacity");
        case 20:
        	throw new IllegalArgumentException("Illegal cursor location");
        }
        
        halt();
        return 0;
    }
}
