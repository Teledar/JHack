/**
 * JHack - https://github.com/Teledar/JHack
 * This file implements the Nand to Tetris JackOS Output class for the JHack emulator
 * Nand to Tetris - https://www.nand2tetris.org/
 */

/**
 * A library of functions for writing text on the screen.
 * The Hack physical screen consists of 512 rows of 256 pixels each.
 * The library uses a fixed font, in which each character is displayed 
 * within a frame which is 11 pixels high (including 1 pixel for inter-line 
 * spacing) and 8 pixels wide (including 2 pixels for inter-character spacing).
 * The resulting grid accommodates 23 rows (indexed 0..22, top to bottom)
 * of 64 characters each (indexed 0..63, left to right). The top left 
 * character position on the screen is indexed (0,0). A cursor, implemented
 * as a small filled square, indicates where the next character will be displayed.
 */
public class Output {

    // Character map for displaying characters
    private static int[][] charMaps; 

    // The row and column of the cursor
    private static int x, y;

    /** 
     * Initializes the screen, and locates the cursor at the screen's top-left.
     * @return The return value of this method is ignored.
     */
    public static short init() {
        x = 0;
        y = 0;
        initMap();
        return 0;
    }

    /**
     * Initializes the character map array
     */
    private static void initMap() {

        charMaps = new int[127][11];
        
        // Black square, used for displaying non-printable characters.
        create(0,63,63,63,63,63,63,63,63,63,0,0);

        // Assigns the bitmap for each character in the character set.
        // The first parameter is the character index, the next 11 numbers
        // are the values of each row in the frame that represents this character.
        create(32,0,0,0,0,0,0,0,0,0,0,0);          //
        create(33,12,30,30,30,12,12,0,12,12,0,0);  // !
        create(34,54,54,20,0,0,0,0,0,0,0,0);       // "
        create(35,0,18,18,63,18,18,63,18,18,0,0);  // #
        create(36,12,30,51,3,30,48,51,30,12,12,0); // $
        create(37,0,0,35,51,24,12,6,51,49,0,0);    // %
        create(38,12,30,30,12,54,27,27,27,54,0,0); // &
        create(39,12,12,6,0,0,0,0,0,0,0,0);        // '
        create(40,24,12,6,6,6,6,6,12,24,0,0);      // (
        create(41,6,12,24,24,24,24,24,12,6,0,0);   // )
        create(42,0,0,0,51,30,63,30,51,0,0,0);     // *
        create(43,0,0,0,12,12,63,12,12,0,0,0);     // +
        create(44,0,0,0,0,0,0,0,12,12,6,0);        // ,
        create(45,0,0,0,0,0,63,0,0,0,0,0);         // -
        create(46,0,0,0,0,0,0,0,12,12,0,0);        // .    
        create(47,0,0,32,48,24,12,6,3,1,0,0);      // /
        
        create(48,12,30,51,51,51,51,51,30,12,0,0); // 0
        create(49,12,14,15,12,12,12,12,12,63,0,0); // 1
        create(50,30,51,48,24,12,6,3,51,63,0,0);   // 2
        create(51,30,51,48,48,28,48,48,51,30,0,0); // 3
        create(52,16,24,28,26,25,63,24,24,60,0,0); // 4
        create(53,63,3,3,31,48,48,48,51,30,0,0);   // 5
        create(54,28,6,3,3,31,51,51,51,30,0,0);    // 6
        create(55,63,49,48,48,24,12,12,12,12,0,0); // 7
        create(56,30,51,51,51,30,51,51,51,30,0,0); // 8
        create(57,30,51,51,51,62,48,48,24,14,0,0); // 9
        
        create(58,0,0,12,12,0,0,12,12,0,0,0);      // :
        create(59,0,0,12,12,0,0,12,12,6,0,0);      // ;
        create(60,0,0,24,12,6,3,6,12,24,0,0);      // <
        create(61,0,0,0,63,0,0,63,0,0,0,0);        // =
        create(62,0,0,3,6,12,24,12,6,3,0,0);       // >
        create(64,30,51,51,59,59,59,27,3,30,0,0);  // @
        create(63,30,51,51,24,12,12,0,12,12,0,0);  // ?

        create(65,12,30,51,51,63,51,51,51,51,0,0); // A
        create(66,31,51,51,51,31,51,51,51,31,0,0); // B
        create(67,28,54,35,3,3,3,35,54,28,0,0);    // C
        create(68,15,27,51,51,51,51,51,27,15,0,0); // D
        create(69,63,51,35,11,15,11,35,51,63,0,0); // E
        create(70,63,51,35,11,15,11,3,3,3,0,0);    // F
        create(71,28,54,35,3,59,51,51,54,44,0,0);  // G
        create(72,51,51,51,51,63,51,51,51,51,0,0); // H
        create(73,30,12,12,12,12,12,12,12,30,0,0); // I
        create(74,60,24,24,24,24,24,27,27,14,0,0); // J
        create(75,51,51,51,27,15,27,51,51,51,0,0); // K
        create(76,3,3,3,3,3,3,35,51,63,0,0);       // L
        create(77,33,51,63,63,51,51,51,51,51,0,0); // M
        create(78,51,51,55,55,63,59,59,51,51,0,0); // N
        create(79,30,51,51,51,51,51,51,51,30,0,0); // O
        create(80,31,51,51,51,31,3,3,3,3,0,0);     // P
        create(81,30,51,51,51,51,51,63,59,30,48,0);// Q
        create(82,31,51,51,51,31,27,51,51,51,0,0); // R
        create(83,30,51,51,6,28,48,51,51,30,0,0);  // S
        create(84,63,63,45,12,12,12,12,12,30,0,0); // T
        create(85,51,51,51,51,51,51,51,51,30,0,0); // U
        create(86,51,51,51,51,51,30,30,12,12,0,0); // V
        create(87,51,51,51,51,51,63,63,63,18,0,0); // W
        create(88,51,51,30,30,12,30,30,51,51,0,0); // X
        create(89,51,51,51,51,30,12,12,12,30,0,0); // Y
        create(90,63,51,49,24,12,6,35,51,63,0,0);  // Z

        create(91,30,6,6,6,6,6,6,6,30,0,0);          // [
        create(92,0,0,1,3,6,12,24,48,32,0,0);        // \
        create(93,30,24,24,24,24,24,24,24,30,0,0);   // ]
        create(94,8,28,54,0,0,0,0,0,0,0,0);          // ^
        create(95,0,0,0,0,0,0,0,0,0,63,0);           // _
        create(96,6,12,24,0,0,0,0,0,0,0,0);          // `

        create(97,0,0,0,14,24,30,27,27,54,0,0);      // a
        create(98,3,3,3,15,27,51,51,51,30,0,0);      // b
        create(99,0,0,0,30,51,3,3,51,30,0,0);        // c
        create(100,48,48,48,60,54,51,51,51,30,0,0);  // d
        create(101,0,0,0,30,51,63,3,51,30,0,0);      // e
        create(102,28,54,38,6,15,6,6,6,15,0,0);      // f
        create(103,0,0,30,51,51,51,62,48,51,30,0);   // g
        create(104,3,3,3,27,55,51,51,51,51,0,0);     // h
        create(105,12,12,0,14,12,12,12,12,30,0,0);   // i
        create(106,48,48,0,56,48,48,48,48,51,30,0);  // j
        create(107,3,3,3,51,27,15,15,27,51,0,0);     // k
        create(108,14,12,12,12,12,12,12,12,30,0,0);  // l
        create(109,0,0,0,29,63,43,43,43,43,0,0);     // m
        create(110,0,0,0,29,51,51,51,51,51,0,0);     // n
        create(111,0,0,0,30,51,51,51,51,30,0,0);     // o
        create(112,0,0,0,30,51,51,51,31,3,3,0);      // p
        create(113,0,0,0,30,51,51,51,62,48,48,0);    // q
        create(114,0,0,0,29,55,51,3,3,7,0,0);        // r
        create(115,0,0,0,30,51,6,24,51,30,0,0);      // s
        create(116,4,6,6,15,6,6,6,54,28,0,0);        // t
        create(117,0,0,0,27,27,27,27,27,54,0,0);     // u
        create(118,0,0,0,51,51,51,51,30,12,0,0);     // v
        create(119,0,0,0,51,51,51,63,63,18,0,0);     // w
        create(120,0,0,0,51,30,12,12,30,51,0,0);     // x
        create(121,0,0,0,51,51,51,62,48,24,15,0);    // y
        create(122,0,0,0,63,27,12,6,51,63,0,0);      // z
        
        create(123,56,12,12,12,7,12,12,12,56,0,0);   // {
        create(124,12,12,12,12,12,12,12,12,12,0,0);  // |
        create(125,7,12,12,12,56,12,12,12,7,0,0);    // }
        create(126,38,45,25,0,0,0,0,0,0,0,0);        // ~

	    return;
    }

    /**
     * Creates the character map array of the given character index, using the given values.
     */
    private static void create(int index, int a, int b, int c, int d, int e,
                         int f, int g, int h, int i, int j, int k) {
    	
    	int map[] = charMaps[index];
    	
        map[0] = a;
        map[1] = b;
        map[2] = c;
        map[3] = d;
        map[4] = e;
        map[5] = f;
        map[6] = g;
        map[7] = h;
        map[8] = i;
        map[9] = j;
        map[10] = k;

        return;
    }
    
    /**
     * Returns the character map (array of size 11) of the given character.
     * If the given character is invalid or non-printable, returns the
     * character map of a black square.
     */
    private static int[] getMap(int c) {
        if ((c < 32) | (c > 126)) {
            c = 0;
        }
        return charMaps[c];
    }

    /** 
     * Moves the cursor to the j-th column of the i-th row,
     * and erases the character displayed there.
     * @return The return value of this method is ignored.
     */
    public static short moveCursor(short i, short j) {
        if ((i < 0) | (i > 22) | (j < 0) | (j > 63)) {
            Sys.error((short) 20);
            return 0;
        }
        x = j * 8;
        y = i * 11;
        overwrite(32);
        return 0;
    }

    /** 
     * Displays the given character at the cursor location,
     * and advances the cursor one column forward.
     * @return The return value of this method is ignored.
     */
    public static short printChar(short c) {
    	overwrite(c);
        x = x + 8;
        if (x > 511) {
            x = 0;
            y = y + 11;
            if (y > 252) {
                y = 0;
            }
        }
        return 0;
    }
    
    /** 
     * Overwrites the character at the cursor location without advancing the cursor
     */
    private static void overwrite(int c) {
       	int map[] = getMap(c);
        short i = 0, j;
        j = (short) ((y * 32) + (x / 16) + HackComputer.SCREEN);
        if ((x & 15) > 0) {
            while (i < 11) {
                HackComputer.poke((short) (map[i] * 256 | HackComputer.peek(j) & 0x00FF), j);
                i++;
                j += 32;
            }
        } else {
            while (i < 11) {
                HackComputer.poke((short) (map[i] | HackComputer.peek(j) & 0xFF00), j);
                i++;
                j += 32;
            }
        }
    }

    /** 
     * Displays the given string starting at the cursor location,
     * and advances the cursor appropriately.
     * @return The return value of this method is ignored.
     */
    public static short printString(short s) {
        short len, i = 0;
        len = String.length(s);
        while (i < len) {
            printChar(String.charAt(s, i));
            i++;
        }
        return 0;
    }

    /** 
     * Displays the given integer starting at the cursor location,
     * and advances the cursor appropriately.
     * @return The return value of this method is ignored.
     */
    public static short printInt(short i) {
        short s;
        s = String.NEW((short) 6);
        String.setInt(s, i);
        printString(s);
        return 0;
    }

    /** 
     * Advances the cursor to the beginning of the next line.
     * @return The return value of this method is ignored.
     */
    public static short println() {
        x = 0;
        y = y + 11;
        if (y > 252) {
            y = 0;
        }
        return 0;
    }

    /** 
     * Moves the cursor one column back.
     * @return The return value of this method is ignored.
     */
    public static short backSpace() {
        x -= 8;
        if (x < 0) {
            x = 504;
            y -= 11;
            if (y < 0) {
            	y = 242;
            }
        }
        overwrite(32);
        return 0;
    }
}
