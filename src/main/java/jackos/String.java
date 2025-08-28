package jackos;

import runtime.HackComputer;

/**
 * Represents character strings. In addition for constructing and disposing
 * strings, the class features methods for getting and setting individual
 * characters of the string, for erasing the string's last character,
 * for appending a character to the string's end, and more typical
 * string-oriented operations.
 */
public class String {

    //field int addr, max;

	//Instead of actually storing string data in Hack memory, a reference
	//is stored to the index of the string in this array
	private static int MAX_STRINGS = 8193;
	private static StringBuffer strings[] = new StringBuffer[MAX_STRINGS];
	private static int first_free = 0;
	
    /** constructs a new empty string with a maximum length of maxLength
     *  and initial length of 0. */
    public static short NEW(short maxLength) {
    	short me = Memory.alloc((short) 2);
        HackComputer.poke((short) first_free, me);
        if (maxLength < 0) {
            Sys.error((short) 14);
            return -1;
        }
        HackComputer.poke(maxLength, (short) (me + 1));
        strings[first_free] = new StringBuffer();
        updateFree();
        return me;
    }

    //Updates the first address that is available for a new String
    private static void updateFree() {
    	do {
    		first_free++;
    		if (first_free >= MAX_STRINGS) {
    			first_free = 0;
    		}
    	} while (strings[first_free] != null);
    }
    
    /** Disposes this string. */
    public static short dispose(short me) {
        strings[HackComputer.peek(me)] = null;
        Memory.deAlloc(me);
        return 0;
    }

    /** Returns the current length of this string. */
    public static short length(short me) {
        return (short) strings[HackComputer.peek(me)].length();
    }

    /** Returns the character at the j-th location of this string. */
    public static short charAt(short me, short j) {
    	StringBuffer mine = strings[HackComputer.peek(me)];
        if ((j < 0) || (mine.length() <= j)) {
            Sys.error((short) 15);
        }
        return (short) mine.charAt(j);
    }

    /** Sets the character at the j-th location of this string to c. */
    public static short setCharAt(short me, short j, short c) {
    	StringBuffer mine = strings[HackComputer.peek(me)];
        if ((j < 0) || (mine.length() <= j)) {
            Sys.error((short) 16);
            return 0;
        }
        mine.setCharAt(j, (char) c);
        return 0;
    }

    /** Appends c to this string's end and returns this string. */
    public static short appendChar(short me, short c) {
    	StringBuffer mine = strings[HackComputer.peek(me)];
    	short max = HackComputer.peek((short) (me + 1));
        if (mine.length() == max) {
            Sys.error((short) 17);
            return me;
        }
        mine.append((char) c);
        return me;
    }

    /** Erases the last character from this string. */
	public static short eraseLastChar(short me) {
    	StringBuffer mine = strings[HackComputer.peek(me)];
        if (mine.length() > 0) {
        	mine.deleteCharAt(mine.length() - 1);
        } else {
            Sys.error((short) 18);
        }
        return 0;
    }

    /** Returns the integer value of this string, 
     *  until a non-digit character is detected. */
    public static short intValue(short me) {
    	StringBuffer mine = strings[HackComputer.peek(me)];
        short out = 0, i = 0, l = 0, t;
    	short len = (short) mine.length();
        if (charAt(me, (short) 0) == 45) {
            i = 1;
            l = 1;
            while (i < len) {
                if ((charAt(me, i) < 48) || (charAt(me, i) > 57)) {
                    l = (short) (i - 1);
                    i = len;
                }
                i++;
                l++;
            }
            t = 1;
            while (l > 1) {
                l--;
                out += (charAt(me, l) - 48) * t;
                t *= 10;
            }
            out = (short) -out;
        } else {
            while (i < len) {
                if ((charAt(me, i) < 48) || (charAt(me, i) > 57)) {
                    l = (short) (i - 1);
                    i = len;
                }
                i++;
                l++;
            }
            t = 1;
            while (l > 0) {
                l--;
                out += (charAt(me, l) - 48) * t;
                t *= 10;
            }
        }
        return out;
    }

    /** Sets this string to hold a representation of the given value. */
    public static short setInt(short me, short val) {
    	strings[HackComputer.peek(me)] = new StringBuffer(Integer.toString(val));
        return 0;
    }

    /** Returns the new line character. */
    public static short newLine() {
        return 128;
    }

    /** Returns the backspace character. */
    public static short backSpace() {
        return 129;
    }

    /** Returns the double quote (") character. */
    public static short doubleQuote() {
        return 34;
    }
}
