package jackapp;

import jackos.String;
import jackos.*;

public class Main {
	public static short main() {
		
		print("Welcome to JHack!");
		Output.println();
		print("This sample program will run several tests on the operating system.");
		Output.println();
		print("Press any key: ");

		short key = Keyboard.readChar();
		
		for (int i = 0; i < 17; i++) {
			Output.backSpace();
		}
		
		Output.println();
		print("It looks like you pressed ");
		Output.printInt(key);
		print(" or ");
		Output.printChar(key);
		Output.println();
		
		print("Please enter a string (don't hesitate to use Backspace): ");
		short str = String.NEW((short) 4);
		short str2 = Keyboard.readLine(str);

		Output.moveCursor((short) 0, (short) 0);
		print("You entered: ");
		Output.printString(str2);
		Output.println();
		
		print("Testing Array and Memory... ");
		Output.println();
		
		// Test the Array class and the Memory class
		
		// Create an Array
		short arr = Array.NEW((short) 150);
		arr--;
		
		// Allocate a bunch of differently sized blocks and store their addresses in the array
		for (short i = 1; i < 150; i++) {
			Memory.poke((short) (arr + i), Memory.alloc(i));
		}

		// Deallocate all the blocks
		for (short i = 1; i < 150; i++) {
			Memory.deAlloc(Memory.peek((short) (arr + i)));
		}
		
		// Dispose the array
		arr++;
		Array.dispose(arr);
		
		print("Test complete. ");
		
		// Test Sys.wait and Keyboard.readInt
		
		print("Please enter a number of milliseconds to wait: ");
		short w = Keyboard.readInt(str);
		print("Waiting... ");
		Output.println();
		Sys.wait(w);
		print("The wait is over! ");
		Output.println();
		
		// Test Screen
		
		print("Press any key to start the Screen test: ");

		key = Keyboard.readChar();
		
		// Test the String class
		
		// Create a String
		
		String.appendChar(str, (short) 65);  //A
		String.appendChar(str, (short) 65);
		String.appendChar(str, (short) 65);
		String.appendChar(str, (short) 65);
		
		String.setInt(str, (short) 9999);
		String.setInt(str, (short) -123);

		String.eraseLastChar(str);
		String.eraseLastChar(str);
		String.eraseLastChar(str);
		String.eraseLastChar(str);
		
		String.appendChar(str, (short) 66);  //B
		String.appendChar(str, (short) 66);
		String.appendChar(str, (short) 66);
		String.appendChar(str, (short) 66);
		
		String.setCharAt(str, (short) 0, (short) (String.charAt(str, (short) 3) + 2)); //D
		String.setCharAt(str, (short) 1, (short) (String.charAt(str, (short) 0) + 43));//o
		String.setCharAt(str, (short) 2, (short) (String.charAt(str, (short) 1) - 1)); //n
		String.setCharAt(str, (short) 3, (short) (String.charAt(str, (short) 2) - 9)); //e
		
		// Print the string
		Output.printString(str);
		String.dispose(str);
		
		return 0;
	}
	
	private static void print(java.lang.String msg) {
		for (char c : msg.toCharArray()) {
			Output.printChar((short) c);
		}
	}
}