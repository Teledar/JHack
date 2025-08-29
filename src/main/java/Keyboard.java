

import java.util.ArrayList;

/**
* A library for handling user input from the keyboard.
*/
public class Keyboard {

	/** Initializes the keyboard. */
	public static short init() {
		return 0;
	} 

 	/**
 	 * Returns the character of the currently pressed key on the keyboard;
 	 * if no key is currently pressed, returns 0.
 	 *
 	 * Recognizes all ASCII characters, as well as the following keys:
 	 * new line = 128 = String.newline()
 	 * backspace = 129 = String.backspace()
 	 * left arrow = 130
 	 * up arrow = 131
 	 * right arrow = 132
 	 * down arrow = 133
 	 * home = 134
 	 * End = 135
 	 * page up = 136
 	 * page down = 137
 	 * insert = 138
 	 * delete = 139
  	* ESC = 140
  	* F1 - F12 = 141 - 152
  	*/
 	public static short keyPressed() {
    	return HackComputer.peek(HackComputer.KBD);
	}

 	/**	Waits until a key is pressed on the keyboard and released,
	*  then echoes the key to the screen, and returns the character 
	*  of the pressed key. */
	public static short readChar() {
    	short c = 0;
    	Output.printChar((short) 0);
    	while (c == 0) {
         	c = HackComputer.peek(HackComputer.KBD);
     	}
     	while (c == HackComputer.peek(HackComputer.KBD)) {}
 		Output.backSpace();
     	if ((c > 31) & (c < 127)) {
     		Output.printChar(c);
     	}
     	return c;
	}

	/**	Displays the message on the screen, reads from the keyboard the entered
	 *  text until a newline character is detected, echoes the text to the screen,
	 *  and returns its value. Also handles user backspaces. */
	public static short readLine(short message) {
		short str;
		short key = 0;
		ArrayList<Short> chars = new ArrayList<>();
		Output.printString(message);
		while (key != 128) {
			key = readChar();
			if ((key == 129) && (chars.size() > 0)) {
				chars.remove(chars.size() - 1);
				Output.backSpace();
			} else if ((key > 31) && (key < 127)) {
				chars.add(key);
			}
		}
		Output.println();
		str = String.NEW((short) (chars.size()));
		for (short c : chars) {
			String.appendChar(str, c);
		}
     	return str;
	}   

	/** Displays the message on the screen, reads from the keyboard the entered
	 *  text until a newline character is detected, echoes the text to the screen,
	 *  and returns its integer value (until the first non-digit character in the
	 *  entered text is detected). Also handles user backspaces. */
	public static short readInt(short message) {
		return String.intValue(readLine(message));
	}
}


