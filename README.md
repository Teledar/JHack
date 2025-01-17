# JHack
An implementation of the Nand to Tetris Hack computer and a compiler to convert Hack .vm files into .class files to run on the JVM.

(See the [Nand to Tetris](https://www.nand2tetris.org/) course for more information on the Hack computer.)

This repo contains two separate programs: VMtoClass and JHack. VMtoClass compiles Hack .vm files to Java class files designed to run on JHack. JHack is an implementation of the Hack computer that is designed to run these compiled Java class files.

VMtoClass depends on the [Apache Commons BCEL](https://commons.apache.org/proper/commons-bcel/) library. For convenience, I have compiled a JAR for VMtoClass that includes all the necessary resources. You can run this by opening a terminal and typing:
```
java -jar VMtoClass.jar
```
VMtoClass will load all the .vm files in a specified directory and convert them to individual Java class files. 

You will also need to compile JHack.java, HackComputer.java, and HackDisplay.java with commands like:
```
javac JHack.java
```
An implementation of the JackOS standard library is included with JHack. If you want to use this implementation, you will need to compile those files as well (Array.java, Keyboard.java, etc.). The existing Main.java file in the repo is a sample application; if you want to run your own application on JHack, do not compile this file.

To run the compiled classes output by VMtoClass, copy the created class files into the same directory as "JHack.class," then open a terminal and run:
```
java JHack.
```

If you have issues with the graphics (like the image below), this may be a Windows scaling issue. Try running:
```
java -Dsun.java2d.uiScale=1.0 JHack
```
or, for a larger screen size:
```
java -Dsun.java2d.uiScale=2.0 JHack
```

<img width="386" alt="image" src="https://github.com/user-attachments/assets/beb4ff01-367a-4f1b-b308-f5e44e58bb53">

Running ashort's [chess](https://github.com/AndrewRShort/chess-vm-files) program on the JVM.

<img width="516" alt="image" src="https://github.com/user-attachments/assets/12b30030-0d2e-4f83-85ae-fe9e433ab588" />

Running Chess with 2.0 uiScale.

