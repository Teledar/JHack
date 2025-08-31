/**
 * JHack - https://github.com/Teledar/JHack
 * This file implements the Nand to Tetris JackOS Math class for the JHack emulator
 * Nand to Tetris - https://www.nand2tetris.org/
 */

/**
*  A library of commonly used mathematical functions.
*  Note: Jack compilers implement multiplication and division
*  using calls to OS functions in this class.
*/
public class Math {

	/**
	 * Initializes the Math library.
     * @return The return value of this method is ignored.
	 */
	public static short init() {
	    return 0;
	}

	/** 
	 * Returns the product of x and y. 
	 * When a Jack compiler detects the multiplication operator '*'
	 * in an expression, it handles it by invoking this method. 
	 * Thus, in Jack, x * y and Math.multiply(x,y) return the same value. 
	 * @param x the multiplicand
	 * @param y the multiplier
	 */
	public static short multiply(short x, short y) {
	    return (short) (x * y);
	}

	/** 
	 * Returns the integer part of x / y.
	 * When a Jack compiler detects the division operator '/'
	 * an an expression, it handles it by invoking this method.
	 * Thus, x/y and Math.divide(x,y) return the same value. 
	 * @param x the dividend
	 * @param y the divisor
	 */
	public static short divide(short x, short y) {
		return (short) (x / y);
	}

	/** Returns the integer part of the square root of x. */
	public static short sqrt(short x) {
	    if (x < 0) {
	        Sys.error((short) 4);
	        return -1;
	    }

	    return (short) java.lang.Math.sqrt(x);
	}

	/** Returns the greater value. */
	public static short max(short a, short b) {
	    return a > b ? (short) a : (short) b;
	}

	/** Returns the smaller value. */
	public static short min(short a, short b) {
	    return a < b ? (short) a : (short) b;
	}

	/** Returns the absolute value of x. */
	public static short abs(short x) {
	    return x >= 0 ? (short) x : (short) -x;
	}
 
}

