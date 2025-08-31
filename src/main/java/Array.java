/**
 * JHack - https://github.com/Teledar/JHack
 * This file implements the Nand to Tetris JackOS Array class for the JHack emulator
 * Nand to Tetris - https://www.nand2tetris.org/
 */

/**
 * Represents an array.
 * In the Jack language, arrays are instances of the Array class.
 * Once declared, the array entries can be accessed using the usual
 * syntax arr[i]. Each array entry can hold a primitive data type as 
 * well as any object type. Different array entries can have different 
 * data types.
 */
public class Array {

    /** 
     * Constructs a new Array of the given size.
     * @param size the length of the array
     * @return a pointer to the Array in Hack memory
     */
    public static short NEW(short size) {
        if (size < 1) {
            Sys.error((short) 2);
        }
        return Memory.alloc(size);
    }

    /** 
     * Disposes this array.
     * @param me a pointer to the Array in Hack memory
     * @return The return value of this method is ignored.
     */
    public static short dispose(short me) {
        Memory.deAlloc(me);
        return 0;
    }
}
