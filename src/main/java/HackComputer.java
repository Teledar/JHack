
// Represents the memory of the Hack Computer
public class HackComputer {

    // Pointer to the beginning of the screen memory map
    public static final short SCREEN = 16384;

    // Pointer to the keyboard memory map
    public static final short KBD = SCREEN + 8192;

    // The last address in the RAM
    // This can be increased up to 32767 if you need more RAM
    public static final short RAM_END = KBD; 

    // The Nand2Tetris VM emulator does not permit direct access to addresses below 2048,
    // since these are used for the stack and static variables. With JHack, the JVM handles
    // the stack and static variables, so the first 2048 addresses are unused. If you want
    // to make them available for your system to use as heap, change this value to 0.
    public static final short HEAP_START = 2048;

    public static final short TEMPS_SIZE = 8;

    public static final short STATICS_SIZE = 240;

    private static short ram[] = new short[RAM_END + 1];

    private static short temps[] = new short[TEMPS_SIZE];

    private static short statics[] = new short[STATICS_SIZE];

    static {
        for (int i = 0; i <= RAM_END; i++) {
            ram[i] = 0;
        }
        for (int i = 0; i < TEMPS_SIZE; i++) {
            temps[i] = 0;
        }
        for (int i = 0; i < STATICS_SIZE; i++) {
            statics[i] = 0;
        }
    }

    public static short peek(short address) {
        if (address < HEAP_START || address > RAM_END) {
            throw new IndexOutOfBoundsException(address);
        }
        return ram[address];
    }

    public static void poke(short value, short address) {
        if (address < HEAP_START || address > RAM_END) {
            throw new IndexOutOfBoundsException(address);
        }
        ram[address] = value;
    }

    public static void popTemp(short value, short index) {
        if (index < 0 || index >= TEMPS_SIZE) {
            throw new IndexOutOfBoundsException(index);
        }
        temps[index] = value;
    } 

    public static short pushTemp(short index) {
        if (index < 0 || index >= TEMPS_SIZE) {
            throw new IndexOutOfBoundsException(index);
        }
        return temps[index];
    }
    
    public static void popStatic(short value, short index) {
        if (index < 0 || index >= STATICS_SIZE) {
            throw new IndexOutOfBoundsException(index);
        }
        statics[index] = value;
    } 

    public static short pushStatic(short index) {
        if (index < 0 || index >= STATICS_SIZE) {
            throw new IndexOutOfBoundsException(index);
        }
        return statics[index];
    }
}