/**
 * JHack - https://github.com/Teledar/JHack
 * This class transpiles Nand2Tetris Hack VM code to Java bytecode that can be run on the JVM using JHack,
 * a Java-based emulator of the Nand to Tetris Hack computer.
 * Nand to Tetris - https://www.nand2tetris.org/
 */

package compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Transpiles Hack VM code to Java class files for the JHack emulator
 */
public class VMtoClass {
	
	// The directory containing the Hack .vm files to translate
	static Path sourceDir;
	
	// An array of the files in the input directory
	static String sourceFileNames[];


	/**
	 * The entry point of the JHack compiler program
	 * @param args The compiler must be provided with one argument: a directory containing the files
	 * to transpile
	 */
	public static void main(String[] args) {

		System.out.println("nand2tetris VM to .class translator");
		System.out.println("https://www.nand2tetris.org");
		System.out.println("Part of the JHack project");
		System.out.println("https://github.com/Teledar/JHack");
		System.out.println();
		
		if (!getArgs(args)) {
			return;
		}

		for (String fileName : sourceFileNames) {
			System.out.println(fileName);
			ClassWriter writer = new ClassWriter(sourceDir.resolve(fileName));
			try {
				writer.compile();
			} catch (IOException e) {
				System.err.println("Error while reading file");
				System.err.println(fileName);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
			}
		} 
	}

	
	/**
	 * Parses the arguments sent to the program and stores them in the appropriate
	 * variables; prints a message and returns false if an argument is incorrect
	 */
	static boolean getArgs(String[] args) {
		
		for (String arg : args) {
			if (arg.equals("/h") || arg.equals("/help") || arg.equals("-h") || arg.equals("--help")) {
				printHelp();
				return false;
			}
		}
		
		// Currently, one argument is expected: the input directory
		if (args.length != 1) {
			printHelp();
			return false;
		}
		
		sourceDir = Paths.get(args[0]).toAbsolutePath();
		
		if (!Files.isDirectory(sourceDir)) {
			sourceDir = sourceDir.getParent();
		}
		
		sourceFileNames = sourceDir.toFile().list((dir, name) -> name.toLowerCase().endsWith(".vm"));
		
		return true;
	}
	
	
	/**
	 * Prints the usage of the program
	 */
	static void printHelp() {
		System.out.println("SYNTAX");
		System.out.println("VMtoClass <inDir>");
		System.out.println("\tTranslates all .vm files in inDir from Hack VM language to Java class files.");
	}

}
