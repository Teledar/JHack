/**
 * JHack - https://github.com/Teledar/JHack
 * This class parses a Nand2Tetris Hack VM file line by line
 * Nand to Tetris - https://www.nand2tetris.org/
 */

package compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

/**
 * Parses a Nand2Tetris Hack VM file line by line
 */
public class Parser {
	
	private BufferedReader reader;
	
	// The next line in the file
	private String next;
	
	// The index of the current line
	private int lineIndex = 0;
	
	// The command type of the current line
	private Command type;
	
	// The second word of the current line
	private String arg1;
	
	// The third word of the current line
	private int arg2;
	
	// The functions in the file, with their numbers of arguments
	private Hashtable<String, Integer> functions = new Hashtable<String, Integer>();;
	
	// The file being parsed
	private Path file;
	
	// The name of the file being parsed
	private String fileName;
	
	/**
	 * Constructs a new Parser for the given file
	 * @param file the full path to the VM file to parse
	 */
	public Parser(Path file) throws IOException {
		file = file.toAbsolutePath();
		if (Files.isDirectory(file)) {
			throw new IllegalArgumentException("Path must be a file.");
		}
		this.file = file;
		
		fileName = file.getFileName().toString();
		int dot = fileName.lastIndexOf('.');
		if (dot > -1) {
			fileName = fileName.substring(0, dot);
		}
		
		findFunctions();
		
		reader = Files.newBufferedReader(this.file);
		next = reader.readLine();
	}
	
	
	/**
	 * Returns whether there are more lines left to read in the file
	 */
	public boolean moreLines() {
		return next != null;
	}
	
	
	/**
	 * Parses the next line of the file
	 */
	public void advance() throws IOException {
		if (next != null) {
			parse();
		}
		if (reader != null) {
			next = reader.readLine();
			lineIndex++;
		}
		if (next == null) {
			reader.close();
			reader = null;
		}
	}
	
	
	/**
	 * Returns the command type of the current line
	 */
	public Command getType() {
		return type;
	}
	
	
	/**
	 * Returns the second word of the current line
	 */
	public String getArg1() {
		return arg1;
	}

	
	/**
	 * Returns the third word of the current line
	 */
	public int getArg2() {
		return arg2;
	}


	/**
	 * Returns the number of local arguments in the given function
	 */
	public int getFuncArgs(String function) {
		if (!functions.containsKey(function)) {
			throw new IllegalArgumentException("Function " + function + " does not exist");
		}
		return functions.get(function);
	}
	
	
	/**
	 * Preprocesses the input .vm file, finding all the functions and their number of arguments
	 */
	private void findFunctions() throws IOException {
		
		String currentFunction = "", line;
		
		int argCount = 0;
		
		reader = Files.newBufferedReader(file);
		line = reader.readLine();
		
		while (line != null) {
			
			lineIndex++;
			
			line = line.trim();
			if (line.contains("//")) {
				line = line.substring(0, line.indexOf("//")).trim();
			}
			
			//The Hack VM language does not specify a function's number of arguments
			//where it is defined; we will count the arguments used within the function
			//to determine how many arguments it has. This works for most cases; however
			//if a function implementation does not use all its arguments, calls to the
			//function may fail.
			if (line.startsWith("function ")) {
				argCount = 0;
				String words[] = getWords(line);
				if (words.length == 3) {
					String func = words[1];
					validateFunction(func);
					if (functions.containsKey(func)) {
						throw new IllegalArgumentException("Line " + lineIndex + ": Duplicate function " + func);
					} 
					else {
						functions.put(func, 0);
					}
					currentFunction = func;
				}
			}
			
			//Count the number of arguments used within a function
			else if (line.contains(" argument ")) {
				String words[] = getWords(line);
				if (words.length == 3) {
					int index = parseInt(words[2]) + 1;
					if (index > argCount) {
						argCount = index;
						functions.put(currentFunction, argCount);
					}
				}
			}
			
			line = reader.readLine();
			
		}
		
		reader.close();
		
	}
	
	
	/**
	 * Parses the next line of the file
	 */
	private void parse() {
		String words[] = getWords(next);
		
		if (words.length > 0) {
			arg1 = words[0];
			
			switch (arg1) {
			
			case "add":
			case "sub":
			case "neg":
			case "eq":
			case "gt":
			case "lt":
			case "and":
			case "or":
			case "not":
				type = Command.MATH;
				break;
				
			case "push":
				type = Command.PUSH;
				break;
				
			case "pop":
				type = Command.POP;
				break;

			case "goto":
				type = Command.GOTO;
				break;
				
			case "if-goto":
				type = Command.IF;
				break;

			case "label":
				type = Command.LABEL;
				break;

			case "function":
				type = Command.FUNC;
				break;
				
			case "call":
				type = Command.CALL;
				break;
				
			case "return":
				type = Command.RETURN;
				break;
				
			default:
				throw new IllegalArgumentException("Line " + lineIndex + ": unknown command " + arg1);
			}
		}
		
		if (words.length > 1) {
			switch (type) {
			
			case MATH:
			case RETURN:
				throw new IllegalArgumentException("Line " + lineIndex + ": end of line expected");
				
			case PUSH:
			case POP:
				arg1 = words[1];
				switch (arg1) {
				
				case "constant":
					if (type == Command.POP) {
						throw new IllegalArgumentException("Line " + lineIndex + ": cannot pop constant");
					}
					
				case "local":
				case "argument":
				case "this":
				case "that":
				case "static":
				case "pointer":
				case "temp":
					break;
					
				default:
					throw new IllegalArgumentException("Line " + lineIndex + ": unknown segment");
						
				}
				break;

			case IF:
			case GOTO:
			case LABEL:
				arg1 = words[1];
				validateLabel(arg1);
				break;

			case FUNC:
			case CALL:
				arg1 = words[1];
				validateFunction(arg1);
				break;
				
			}
		}
		else {
			switch (type) {
			
			case PUSH:
			case POP:
				throw new IllegalArgumentException("Line " + lineIndex + ": segment expected");
				
			case IF:
			case GOTO:
			case LABEL:
			case FUNC:
			case CALL:
				throw new IllegalArgumentException("Line " + lineIndex + ": name expected");
				
			default:
				break;
				
			}
			
		}
		
		if (words.length > 2) {
			arg2 = parseInt(words[2]);
			
			switch (type) {
			
			case PUSH:
			case POP:
				if (arg1.equals("pointer") && arg2 > 1) {
					throw new IllegalArgumentException("Line " + lineIndex + ": pointer index may not exceed 1");
				}
			case CALL:
			case FUNC:
				break;
			
			default:
				throw new IllegalArgumentException("Line " + lineIndex + ": end of line expected");
			} 
		}
		else {
			switch (type) {

			case PUSH:
			case POP:
				throw new IllegalArgumentException("Line " + lineIndex + ": segment index expected");
				
			case CALL:
				throw new IllegalArgumentException("Line " + lineIndex + ": argument count expected");
				
			case FUNC:
				throw new IllegalArgumentException("Line " + lineIndex + ": variable count expected");
				
			default:
				break;
			}
				
		}
	}
	
	
	/**
	 * Splits a line of VM code into words
	 */
	private String[] getWords(String line) {

		line = line.trim();
		if (line.contains("//")) {
			line = line.substring(0, line.indexOf("//")).trim();
		}
		
		String words[] = line.split(" ");
		
		for (int i = 0; i < words.length; i++) {
			
			for (int j = 1; i + j < words.length; j++) {
				
				if (words[i].equals("")) {
					words[i] = words[i + j];
					words[i + j] = "";
				}
				else {
					break;
				}
				
			}
			
		}
		
		return words;
		
	}

	
	/**
	 * Checks that the function name is valid and follows the format <file>.<function>
	 */
	private void validateFunction(String functionName) {
		
		validateLabel(functionName);
		
		String names[] = functionName.split("\\.");
		if (names.length != 2) {
			throw new IllegalArgumentException("Line " + lineIndex + ": function name must match the format <file>.<function>");
		}
		
	}
	
	
	/**
	 * Check that the given label is valid for Hack VM format
	 */
	private void validateLabel(String label) {
		
		if (!Character.isLetter(label.charAt(0))) {
			throw new IllegalArgumentException("Line " + lineIndex + ": label must begin with a letter");
		}
		
		for (int i = 1; i < label.length(); i++) {
			if (!validChar(label.charAt(i))) {
				throw new IllegalArgumentException("Line " + lineIndex + ": name may not contain " + label.charAt(i));
			}
		}
		
	}
	

	/**
	 * Returns true if the given character is allowed in a Hack VM symbol
	 */
	private boolean validChar(char c) {
		if (Character.isLetterOrDigit(c) || c == '_' || c == '.' 
				|| c == '$')
			return true;
		else
			return false;
	}	
	

	/**
	 * Converts the given string to an integer
	 */
	private int parseInt(String str) {
		try {
			int val = Integer.parseUnsignedInt(str);
			if (val > 32767) {
				throw new IllegalArgumentException("Line " + lineIndex + ": constant may not exceed 32767");
			}
			return val;
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("Line " + lineIndex + ": invalid number format");
		}
	}

	
	/**
	 * The command types of the Hack VM file format
	 */
	public enum Command {
		MATH, PUSH, POP, LABEL, GOTO, IF, FUNC, RETURN, CALL
	}
	
	
}