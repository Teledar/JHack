package compiler;

//VMtoClass.java
//This class converts Nand2Tetris Hack VM code to Java bytecode that can be run on the JVM using JHack.
//www.nand2tetris.org

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VMtoClass {
	
	//The path that the output will be saved to - a Java .class file named HackApplication.class
	static Path outPath;
	
	//The directory containing the Hack .vm files to translate
	static Path inPath;
	
	//An array of the files in the input directory
	static String inFiles[];
	
	//Specific to the current .vm file; which index to store its static variables in the RAM
	//and how many static variables it has
	static int static_start, static_count;
	
	//If set to true, code will be inserted into each generated method that causes it to print its name
	//when called; a rudimentary stack trace for debugging the output class file
	static boolean debug_flag = false;
	
	//If set to true, calls to Math.multiply and Math.divide will be replaced with JVM instructions
	//instead of calling the methods implemented in the .vm input files
	static boolean math_flag = true;
	
	//Temporary; the args that are passed to VMtoClass when it starts
	static String[] temp = {"C:\\nand2tetris\\nand2tetris\\projects\\13\\chess-vm-files-main"};
	
	public static void main(String[] args) {
		
		//Use the built-in temp args
		args = temp;

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

	
	//Parses the args sent to the program and stores them in the appropriate
	//variables; prints a message and returns false if an arg is incorrect
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
	
	
	static void printHelp() {
		System.out.println("SYNTAX");
		System.out.println("VMtoClass <inDir>");
		System.out.println("\tTranslates all .vm files in inDir from Hack VM language to a single Java class file.");
		System.out.println("\tThe output is saved to HackApplication.class.");
	}
	
	
	//Generate bytecode for the given .vm file
	static boolean translate(String file) {
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
			return false;
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return false;
		}
		
		return true;
		
	}

}
