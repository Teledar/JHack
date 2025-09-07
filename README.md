# JHack - a Java-based Hack emulator
JHack provides a Java-based emulator for the Nand to Tetris Hack computer. Its most significant feature is that Hack programs are compiled to Java bytecode before running on the emulator. This significantly improves the execution speed over the emulator included with the Nand to Tetris project.

(See the [Nand to Tetris](https://www.nand2tetris.org/) course for more information on the Hack computer.)

This repo contains two separate programs: VMtoClass and JHack. VMtoClass is a compiler for Hack VM files: it transpiles to Java class files with API calls to JHack. JHack is an implementation of the Hack computer which provides memory, display, and keyboard input for these compiled Java class files.

VMtoClass uses the java.lang.classfile package, which requires at least JDK 24. However, JHack and the classes compiled by VMtoClass should work fine with earlier versions of Java.

To run the emulator, compile JHack.java and VMtoClass.java. Then use VMtoClass to compile your VM files:
```
java compiler.VMtoClass path\to\vmfiles
```

The class files will be saved to the same directory as the source VM files. Copy the created class files into the same directory as "JHack.class," then open a terminal and run:
```
java JHack
```

An implementation of the JackOS standard library is included with JHack. If you want to use this implementation, you will need to compile those files as well (Array.java, Keyboard.java, etc.), and copy them into the same directory as JHack and your compiled program. The existing Main.java file in the repo is a test application; if you want to run your own application on JHack, do not include this file with your compiled program files.

By default, JHack scales graphics to 2x to make the display easier to read. If you'd like to change the scale, you can run JHack with the following flag:
```
java -Dsun.java2d.uiScale=1.0 JHack
```

<img width="516" alt="image" src="https://github.com/user-attachments/assets/12b30030-0d2e-4f83-85ae-fe9e433ab588" />

Running ashort's [chess](https://github.com/AndrewRShort/chess-vm-files) program on JHack.

