public final class HackApplication {

    //Hack .vm files will be converted to a HackApplication.class file when compiled. Execution will begin in the run() function.
    //This is a sample file to show the underlying structure of the generated .class file

    //a reference to the ram
    private static short ram[];

    //temp 0 through temp 7
    private static short temps[];

    //static 0 through static 239
    private static short statics[];

    //the original name of the Hack program
    public static String getName() {
        //Name is auto-generated from the directory that the VM files were in
        return "Pong";
    }

    public static void run(short ram_array[]) {
        ram = ram_array;
        temps = new short[8];
        statics = new short[240];
        Sys_init();
    }

    //Generated from Hack VM code
    private static short Sys_init() {
        //Application logic starts here
        return 0;
    }

}
