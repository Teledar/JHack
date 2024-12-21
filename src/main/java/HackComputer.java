
public class HackComputer {

    public static final short SCREEN = 16384;

    public static final short KBD = 24576;

    public static final short RAM_SIZE = KBD + 1; 

    public static final short TEMPS_SIZE = 8;

    public static final short STATICS_SIZE = 240;

    private static short ram[] = new short[RAM_SIZE];

    private static short temps[] = new short[TEMPS_SIZE];

    private static short statics[] = new short[STATICS_SIZE];

    static {
        for (int i = 0; i < RAM_SIZE; i++) {
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
        if (address < 0 || address >= RAM_SIZE) {
            //throw exception
        }
        return ram[address];
    }

    public static void poke(short value, short address) {
        if (address < 0 || address >= RAM_SIZE) {
            //throw exception
        }
        ram[address] = value;
    }

    public static void pushTemp(short value, short index) {
        if (index < 0 || index >= TEMPS_SIZE) {

        }
        temps[index] = value;
    } 

    public static short popTemp(short index) {
        if (index < 0 || index >= TEMPS_SIZE) {
            
        }
        return temps[index];
    }
    
    public static void pushStatic(short value, short index) {
        if (index < 0 || index >= STATICPS_SIZE) {

        }
        statics[index] = value;
    } 

    public static short popStatic(short index) {
        if (index < 0 || index >= STATICS_SIZE) {
            
        }
        return statics[index];
    }
}