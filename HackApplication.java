public class HackApplication {

    //Hack .vm files will be converted to a HackApplication.class file when compiled. Execution will begin in the run() function.

    //Pointer to the beginning of the screen memory map
    final static int SCREEN = 16384 - 2048;

    //Pointer to the keyboard memory map
    final static int KBD = 24576 - 2048;

    public static void run(Short ram[]) {
        ram[SCREEN + 32] = -32768;
        ram[SCREEN + 33] = -32768;
    }
}