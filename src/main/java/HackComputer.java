

/** Provides access to the memory of the Hack Computer */
public class HackComputer {

	/** The first address in the screen memory map */
	public static final short SCREEN = 16384;
	
	/** The last address in the screen memory map */
	public static final short SCREEN_END = SCREEN + 8191;

	/** The address of the keyboard memory map */
	public static final short KBD = SCREEN_END + 1;

	// This can be increased up to 32767 if you need more RAM
	/** The last address in the RAM */
	public static final short RAM_END = KBD; 

	// The Nand2Tetris VM emulator does not permit direct access to addresses below 2048,
	// since these are used for the stack and static variables. With JHack, the JVM handles
	// the stack and static variables, so the first 2048 addresses are unused. If you want
	// to make them available for your system to use as heap, change this value to 0.
	/** The first address in the heap. Addresses below this are not accessible. */
	public static final short HEAP_START = 2048;

	/** The size of the temp segment */
	public static final short TEMPS_SIZE = 8;

	// In this implementation of the Hack computer, the temp segment is
	// stored outside the RAM. The JVM handles the stack, so the only reserved
	// areas in the RAM are the screen map and the keyboard map.
	// There is no static segment; each class contains its own static fields.
	private static short ram[] = new short[RAM_END + 1];

	private static short temps[] = new short[TEMPS_SIZE];

	static {
		for (int i = 0; i <= RAM_END; i++) {
			ram[i] = 0;
		}
		for (int i = 0; i < TEMPS_SIZE; i++) {
			temps[i] = 0;
		}
	}

	/** Retrieves a value from the RAM of the Hack computer.
	 * @throws IndexOutOfBoundsException if the index is out of range
	 * @param address The register to look in 
	 */
	public static short peek(int address) {
		try {
			Thread.sleep(0); // Keyboard input will not get through unless the thread sleeps
		} catch (InterruptedException e) {
		}
	
		if (address < HEAP_START || address > RAM_END) {
			throw new IndexOutOfBoundsException(address);
		}
		return ram[address];
	}

	// The value parameter precedes the address parameter because this reduces the number of 
	// stack operations required to update a register in the RAM. The Hack VM language always
	// specifies the storage location after the value to store. This makes it natural to push
	// the address onto the stack after the value. The downside is that this order of parameters
	// is opposite from the Jack Memory.poke function, making mistakes common in user-written code
	// that calls this function directly. However, the user should generally not need to call this
	// function directly.
	/** Stores a value in the RAM of the Hack computer.
	 * @throws IndexOutOfBoundsException if the index is out of range
	 * @param value The value to be stored (will be truncated to a short)
	 * @param address The address to store the value in
	 */
	public static void poke(int value, int address) {
		try {
			Thread.sleep(0);
		} catch (InterruptedException e) {
		}
	
		if (address < HEAP_START || address > RAM_END) {
			throw new IndexOutOfBoundsException(address);
		}
		ram[address] = (short) value;
	}
	
	/** Stores a value in the temp segment of the Hack computer.
	 * @throws IndexOutOfBoundsException if the index is out of range
	 * @param value The value to be stored (will be truncated to a short)
	 * @param index The temp segment index
	 */
	public static void popTemp(int value, int index) {
		if (index < 0 || index >= TEMPS_SIZE) {
			throw new IndexOutOfBoundsException(index);
		}
		temps[index] = (short) value;
	}

	/** Retrieves a value from the temp segment of the Hack computer.
	 * @throws IndexOutOfBoundsException if the index is out of range
	 * @param index The temp segment index
	 */
	public static short pushTemp(int index) {
	    if (index < 0 || index >= TEMPS_SIZE) {
	        throw new IndexOutOfBoundsException(index);
	    }
	    return temps[index];
	}
	
}