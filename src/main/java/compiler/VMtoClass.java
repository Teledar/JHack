/**
 * JHack - https://github.com/Teledar/JHack
 * This class transpiles Nand2Tetris Hack VM code to Java bytecode that can be run on the JVM using JHack,
 * a Java-based emulator of the Nand to Tetris Hack computer.
 * Nand to Tetris - https://www.nand2tetris.org/
 */

package compiler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Transpiles Hack VM code to Java class files for the JHack emulator
 */
public class VMtoClass {
	
	// The directory containing the Hack .vm files to translate
	static Path inPath;
	
	// An array of the files in the input directory
	static String inFiles[];

	/**
	 * The entry point of the JHack compiler program
	 * @param args The compiler must be provided with one argument: a directory containing the files
	 * to transpile
	 */
	public static void main(String[] args) {

		System.out.println("nand2tetris VM to .class translator");
		System.out.println("www.nand2tetris.org");
		System.out.println();
		
		if (!getArgs(args)) {
			return;
		}

		for (String file : inFiles) {
			translate(file);
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
		
		//Currently, one argument is expected: the input directory
		if (args.length != 1) {
			printHelp();
			return false;
		}
		
		inPath = Paths.get(args[0]).toAbsolutePath();
		
		if (!Files.isDirectory(inPath)) {
			inPath = inPath.getParent();
		}
		
		inFiles = inPath.toFile().list(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.toLowerCase().endsWith(".vm");
		    }
		});
		
		return true;
	}
	
	
	/**
	 * Prints the usage of the program
	 */
	static void printHelp() {
		System.out.println("SYNTAX");
		System.out.println("VMtoClass <inDir>");
		System.out.println("\tTranslates all .vm files in inDir from Hack VM language to a single Java class file.");
		System.out.println("\tThe output is saved to HackApplication.class.");
	}
	
	
	/**
	 * Generates bytecode for the given .vm file
	 * @param file the full path of the file to transpile
	 */
	static void translate(String file) {
		try {

			Parser parser = new Parser(inPath.resolve(file));
			String classFile = file.substring(0,file.length() - 3) + ".class";
			CodeWriter writer = new CodeWriter(inPath.resolve(classFile));

			while (parser.moreLines()) {
				parser.advance();

				switch (parser.getType()) {
					case MATH:
					writer.writeArithmetic(parser.getArg1());
					break;

					case PUSH:
					case POP:
					writer.writePushPop(parser.getType(), parser.getArg1(), parser.getArg2());
					break;

					case LABEL:
					writer.writeLabel(parser.getArg1());
					break;

					case GOTO:
					writer.writeGoto(parser.getArg1());
					break;

					case IF:
					writer.writeIf(parser.getArg1());
					break;

					case FUNC:
					writer.writeFunction(parser.getArg1(), parser.getArg2(), 
							parser.getFuncArgs(parser.getArg1()));
					break;

					case RETURN:
					writer.writeReturn();
					break;

					case CALL:
					writer.writeCall(parser.getArg1(), parser.getArg2());
					break;
				}
			}

			writer.close();

		} catch (IOException e) {
			System.err.println("Error while reading file: ");
			System.err.println(file);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}
		
	}

}
