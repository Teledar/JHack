# JHack
A compiler to convert Hack .vm files into a .class file to run on the JVM.

(See the [Nand to Tetris](https://www.nand2tetris.org/) course for more information on the Hack .vm format and JackOS.)

VMtoClass loads all the .vm files in a specified directory and converts them to a single Java class file named "HackApplication.class." VMtoClass depends on the [Apache Commons BCEL](https://commons.apache.org/proper/commons-bcel/) library. 

For convenience, I have compiled a JAR for VMtoClass that includes all the necessary resources. You can run this by opening a terminal and typing "java -jar VMtoClass.jar" (WARNING: Do not run untrusted executables downloaded from the Internet).

You will also need to compile JHack.java and HackComputer.java with the commands "javac JHack.java" and "javac HackComputer.java." The existing HackApplication.java file in the repo is only a sample; this is not the application you want to run on JHack.

To run the compiled class output by VMtoClass, copy the created "HackApplication.class" file into the same directory as "JHack.class" and "HackComputer.class," then open a terminal and run "java JHack."

Note: HackApplication calls the Sys.init function when it starts. No built-in implementation of JackOS functions is included; you must supply your own JackOS .vm files.

<img width="386" alt="image" src="https://github.com/user-attachments/assets/beb4ff01-367a-4f1b-b308-f5e44e58bb53">

Running ashort's [chess](https://github.com/AndrewRShort/chess-vm-files) program on the JVM.
