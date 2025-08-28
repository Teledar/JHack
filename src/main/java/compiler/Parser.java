package compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

public class Parser {
	
	private BufferedReader reader;
	
	private String next;
	
	private int line_index = 0;
	
	private Command type;
	
	private String arg1;
	
	private int arg2;
	
	private Hashtable<String, Integer> functions = new Hashtable<String, Integer>();;
	
	private Path file;
	
	private String file_name;
	
	//Constructs a new Parser for the given file
	public Parser(Path file) throws IOException {
		file = file.toAbsolutePath();
		if (Files.isDirectory(file)) {
			throw new IllegalArgumentException("Path must be a file.");
		}
		this.file = file;
		
		file_name = file.getFileName().toString();
		int dot = file_name.lastIndexOf('.');
		if (dot > -1) {
			file_name = file_name.substring(0, dot);
		}
		
		findFunctions();
		
		reader = Files.newBufferedReader(this.file);
		next = reader.readLine();
	}
	
	
	//Whether there are more lines to read
	public boolean moreLines() {
		return next != null;
	}
	
	
	//Advance to the next line
	public void advance() throws IOException {
		if (next != null) {
			parse();
		}
		if (reader != null) {
			next = reader.readLine();
			line_index++;
		}
		if (next == null) {
			reader.close();
			reader = null;
		}
	}
	
	
	//Return the command type of the current line
	public Command getType() {
		return type;
	}
	
	
	//Return the second word of the current line
	public String getArg1() {
		return arg1;
	}

	
	//Return the third word of the current line
	public int getArg2() {
		return arg2;
	}
	
	
	//Preprocess the input .vm file: find all the functions and the number of arguments
	private void findFunctions() throws IOException {
		
		String current_function = "", line;
		
		int arg_count = 0;
		
		reader = Files.newBufferedReader(file);
		line = reader.readLine();
		
		while (line != null) {
			
			line_index++;
			
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
				arg_count = 0;
				String words[] = getWords(line);
				if (words.length == 3) {
					String func = words[1];
					validateFunction(func);
					if (functions.containsKey(func)) {
						throw new IllegalArgumentException("Line " + line_index + ": Duplicate function " + func);
					} 
					else {
						functions.put(func, 0);
					}
					current_function = func;
				}
			}
			
			//Count the number of arguments used within a function
			else if (line.contains(" argument ")) {
				String words[] = getWords(line);
				if (words.length == 3) {
					int index = parseInt(words[2]) + 1;
					if (index > arg_count) {
						arg_count = index;
						functions.put(current_function, arg_count);
					}
				}
			}
			
			line = reader.readLine();
			
		}
		
		reader.close();
		
	}
	
	
	//Parse the next line
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
				
			default:
				throw new IllegalArgumentException("Line " + line_index + ": unknown command " + arg1);
			}
		}
		
		if (words.length > 1) {
			switch (type) {
			
			case MATH:
			case RETURN:
				throw new IllegalArgumentException("Line " + line_index + ": end of line expected");
				
			case PUSH:
			case POP:
				arg1 = words[1];
				switch (arg1) {
				
				case "constant":
					if (type == Command.POP) {
						throw new IllegalArgumentException("Line " + line_index + ": cannot pop constant");
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
					throw new IllegalArgumentException("Line " + line_index + ": unknown segment");
						
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
				throw new IllegalArgumentException("Line " + line_index + ": segment expected");
				
			case IF:
			case GOTO:
			case LABEL:
			case FUNC:
			case CALL:
				throw new IllegalArgumentException("Line " + line_index + ": name expected");
				
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
					throw new IllegalArgumentException("Line " + line_index + ": pointer index may not exceed 1");
				}
			case CALL:
			case FUNC:
				break;
			
			default:
				throw new IllegalArgumentException("Line " + line_index + ": end of line expected");
			} 
		}
		else {
			switch (type) {

			case PUSH:
			case POP:
				throw new IllegalArgumentException("Line " + line_index + ": segment index expected");
				
			case CALL:
				throw new IllegalArgumentException("Line " + line_index + ": argument count expected");
				
			case FUNC:
				throw new IllegalArgumentException("Line " + line_index + ": variable count expected");
				
			default:
				break;
			}
				
		}
	}
	
	
	//Split the line of VM code into words
	private String[] getWords(String line) {

		line = line.trim();
		if (line.contains("//")) {
			line = line.substring(0, line.indexOf("//")).trim();
		}
		
		String words[] = line.toLowerCase().split(" ");
		
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

	
	//Check that the function name is valid and follows the format <file>.<function>
	private void validateFunction(String func) {
		
		validateLabel(func);
		
		String names[] = func.split("\\.");
		if (names.length != 2) {
			throw new IllegalArgumentException("Line " + line_index + ": function name must match the format <file>.<function>");
		}
		
	}
	
	
	//Check that the given label is valid for Hack VM
	private void validateLabel(String label) {
		
		if (!Character.isLetter(label.charAt(0))) {
			throw new IllegalArgumentException("Line " + line_index + ": label must begin with a letter");
		}
		
		for (int i = 1; i < label.length(); i++) {
			if (!validChar(label.charAt(i))) {
				throw new IllegalArgumentException("Line " + line_index + ": name may not contain " + label.charAt(i));
			}
		}
		
	}
	

	//Return true if the given character is allowed in Hack VM symbols
	private boolean validChar(char c) {
		if (Character.isLetterOrDigit(c) || c == '_' || c == '.' 
				|| c == '$')
			return true;
		else
			return false;
	}	
	

	//Convert the given string to an integer
	private int parseInt(String str) {
		try {
			int val = Integer.parseUnsignedInt(str);
			if (val > 32767) {
				throw new IllegalArgumentException("Line " + line_index + ": constant may not exceed 32767");
			}
			return val;
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("Line " + line_index + ": invalid number format");
		}
	}

	
	public enum Command {
		MATH, PUSH, POP, LABEL, GOTO, IF, FUNC, RETURN, CALL
	}
	
	
}