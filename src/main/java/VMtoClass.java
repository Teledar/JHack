//VMtoClass.java
//This class converts Nand2Tetris Hack VM code to Java bytecode that can be run on the JVM using JHack.
//www.nand2tetris.org

//Apache Commons BCEL
//Copyright 2004-2024 The Apache Software Foundation

//This product includes software developed at
//The Apache Software Foundation (https://www.apache.org/).

import org.apache.bcel.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.classfile.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

public class VMtoClass {
	
	//The path that the output will be saved to - a Java .class file named HackApplication.class
	static Path outPath;
	
	//The directory containing the Hack .vm files to translate
	static Path inPath;
	
	//An array of the files in the input directory
	static String inFiles[];
	
	//The input file that is currently being translated
	static String current_file;
	
	//The object used to read files
	static BufferedReader reader;
	
	//A table of labels found in the Hack .vm files, and the corresponding destination that they
	//point to in the Java bytecode
	static Hashtable<String, InstructionHandle> labels = new Hashtable<String, InstructionHandle>();
	
	//A table of labels and the line where they are first called; the line is set to 0 when the label is found
	static Hashtable<String, Integer> label_found = new Hashtable<String, Integer>();
	
	//A table of Hack .vm functions and the line where they are first called; the line is set to 0 when the 
	//function is found
	static Hashtable<String, Integer> funcs = new Hashtable<String, Integer>();
	
	//The bytecode methods generated from the .vm files
	static Hashtable<String, MethodGen> methods = new Hashtable<String, MethodGen>();
	
	//The class that will be generated
	static ClassGen cg;
	
	//The method that is currently being generated
	static MethodGen mg;
	
	//The bytecode instruction list of the method that is currently being generated
	static InstructionList il;
	
	//The constant pool of the class being generated
	static ConstantPoolGen cpg;
	
	//The number of arguments of the current method
	static int arg_count;
	
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
		
		boolean good = false;

		for (int i = 0; i < inFiles.length; i++) {
			good = find_functions(inFiles[i]);
			if (!good) break;
		}
		
		if (good) {
			static_start = 0;
			static_count = 0;
			for (int i = 0; i < inFiles.length; i++) {
				good = translate(inFiles[i]);
				if (!good) break;
			}
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
	
	
	//Writes the generated class file
	private static void write_class() {

		for (String name : methods.keySet()) {
			
			if (name.startsWith(current_file + ".")) {
				MethodGen m = methods.get(name);
				
				m.setConstantPool(cpg);
				
				m.setMaxLocals();
				
				//we need to initialize all the local variables to 0
				il = m.getInstructionList();
				for (int i = m.getMaxLocals() - 1; i >= m.getArgumentTypes().length; i--) {
					il.insert(new ISTORE(i));
					il.insert(new ICONST(0));
				}
				
				m.setMaxStack();
				cg.addMethod(m.getMethod());
			}
			
		}
		
		JavaClass jc = cg.getJavaClass();
		
		try {
			jc.dump(outPath.toString());
			System.out.println("Assembly code saved to:");
		} catch (IOException e) {
			System.out.println("Error writing to file: ");
		}
		System.out.println(outPath.toString());	
		
	}
	
	
	//Sets up a new class file
	private static void initialize_class() {
		
		cg = new ClassGen(current_file, "java.lang.Object", current_file + ".vm",
		        Const.ACC_PUBLIC | Const.ACC_SUPER | Const.ACC_STATIC, null);
		
		cpg = cg.getConstantPool();
		
		outPath = inPath.resolve(current_file + ".class");
		
	}

	
	//Preprocess the input .vm files: find all the functions and the number of arguments
	static boolean find_functions(String file) {
		
		String current_function = "";
		
		int line_index = 0, arg_count = 0;
		
		try {
			
			Path p = inPath.resolve(file);
			
			if (!Files.isDirectory(p)) {
				
				reader = Files.newBufferedReader(p);
				int dot = file.lastIndexOf('.');
				if (dot > -1) {
					file = file.substring(0, dot);
				}
				current_file = file;
				
				String line = reader.readLine();
				while (line != null) {
					
					line_index++;
					
					line = line.trim();
					if (line.contains("//")) {
						line = line.substring(0, line.indexOf("//")).trim();
					}
					
					//The Hack VM language does not specify a function's number of arguments
					//where it is defined; it is only specified when the function is called:
					//Add a MethodGen object for the function
					if (line.startsWith("call ")) {
						
						String words[] = get_words(line);
						if (words.length == 3) {
							int args = parseInt(words[2]);
							if (args > -1 && validFunction(words[1])) {
								add_function(words[1], args, line_index);
							}
						}
						
					} 
					
					//Note that the function definition was found, but do not add a MethodGen yet
					else if (line.startsWith("function ")) {
						String words[] = get_words(line);
						if (words.length == 3) {
							String func = words[1];
							if (validFunction(func)) {
								if (!funcs.containsKey(func)) {
									funcs.put(func, 0);
								} 
								else if (funcs.get(func) == 0) {
									System.out.println("Error: Duplicate function " + func);
									return false;
								}
								funcs.replace(func, 0);
								if (!current_function.equals("")) {
									add_function(current_function, arg_count, 0);
								}
								arg_count = 0;
								current_function = func;
							}
						}
					}
					
					//Count the number of arguments used within a function
					else if (line.contains(" argument ")) {
						String words[] = get_words(line);
						if (words.length == 3) {
							arg_count = Math.max(arg_count, parseInt(words[2]) + 1);
						}
					}
					
					line = reader.readLine();
					
				}
				
				if (!current_function.equals("")) {
					add_function(current_function, arg_count, 0);
				}
				
				reader.close();
				
			}
			
			for (String func : funcs.keySet()) {
				if (func.startsWith(current_file + ".") && funcs.get(func) != 0) {
					System.out.println("Error: missing function " + func);
					return false;
				}
			}
			
			return true;
			
		} catch (IOException e) {
			System.out.println("Error opening file:");
			System.out.println(file);
			return false;
		}
		
	}
	
	
	//Generate bytecode for the given .vm file
	static boolean translate(String file) {
		
		try {
			
			Path p = inPath.resolve(file);
			if (!Files.isDirectory(p)) {
				
				reader = Files.newBufferedReader(p);
				int dot = file.lastIndexOf('.');
				if (dot > -1) {
					file = file.substring(0, dot);
				}
				current_file = file;
				
				static_start += static_count;
				static_count = 0;

				initialize_class();
				
				boolean back = parseLines();
				
				reader.close();
				
				if (back) {
					write_class();
				}
				
				return back;
			}
			
			return true;
			
		} catch (IOException e) {
			System.out.println("Error opening file:");
			System.out.println(file);
			return false;
		}
		
	}

	
	//Translate the opened file line by line
	static boolean parseLines() {
		
		int line_index = 0;
		
		try {
			
			String line = reader.readLine();
			while (line != null) {
				
				line_index++;
				line = line.trim();
				if (line.contains("//")) {
					line = line.substring(0, line.indexOf("//")).trim();
				}
				
				if (!parse(line, line_index)) {
					System.out.println("Line " + line_index);
					System.out.println(current_file + ".vm");
					return false;
				}
				
				line = reader.readLine();
				
			}
			
		}
		
		catch (IOException e) {
			System.out.println("Error reading file:");
			System.out.println(current_file + ".vm");
			return false;
		}
		
		for (String label : labels.keySet()) {
			if (label_found.get(label) != 0) {
				System.out.println("Error: Missing label");
				System.out.println("Line " + labels.get(label));
				System.out.println(current_file + ".vm");
				return false;
			}
		}
		
		return true;
		
	}
	
	
	//Generate bytecode for the given line
	static boolean parse(String line, int line_index) {
		
		String words[] = get_words(line);
		
		if (words.length == 0 || words[0].equals("")) {
			return true;
		}
		
		switch (words[0].toLowerCase()) {
		
		case "add":
		case "sub":
		case "neg":
		case "eq":
		case "gt":
		case "lt":
		case "and":
		case "or":
		case "not":
			if (words.length > 1 && !words[1].equals("")) {
				System.out.println("Error: End of line expected");
				return false;
			}
			return logical(words[0].toLowerCase());
			
		case "push":
		case "pop":
			if (words.length == 1 || words[1].equals("")) {
				System.out.println("Error: Memory segment expected");
				return false;
			}
			if (words.length == 2 || words[2].equals("")) {
				System.out.println("Error: Segment index expected");
				return false;
			}
			if (words.length > 3 && !words[3].equals("")) {
				System.out.println("Error: End of line expected");
				return false;
			}
			if (parseInt(words[2]) == -1) {
				System.out.println("Error: Invalid segment index");
				return false;
			}
			
			switch (words[1].toLowerCase()) {

			case "constant":
				if (words[0].toLowerCase().equals("pop")) {
					System.out.println("Error: Cannot pop constant");
					return false;
				}
				
			case "local":
			case "argument":
			case "this":
			case "that":
			case "static":
			case "pointer":
			case "temp":
				return stack(words[0].toLowerCase(), words[1].toLowerCase(), parseInt(words[2]));
				
			default:
				System.out.println("Error: Invalid memory segment");
				return false;
				
			}
			
		case "goto":
		case "if-goto":
		case "label":
			if (words.length == 1 || words[1].equals("")) {
				System.out.println("Error: Label expected");
				return false;
			}
			if (words.length > 2 && !words[2].equals("")) {
				System.out.println("Error: End of line expected");
				return false;
			}
			return branch(words[0].toLowerCase(), words[1], line_index);
			
		case "function":
		case "call":
			if (words.length == 1 || words[1].equals("")) {
				System.out.println("Error: Function name expected");
				return false;
			}
			if (words.length == 2 || words[2].equals("")) {
				System.out.println("Error: Variable count expected");
				return false;
			}
			if (words.length > 3 && !words[3].equals("")) {
				System.out.println("Error: End of line expected");
				return false;
			}
			int args = parseInt(words[2]);
			if (args == -1) {
				System.out.println("Error: Invalid variable count");
				return false;
			}
			if (!validFunction(words[1]))
				return false;
			if (words[0].toLowerCase().equals("call")) 
				return call(words[1], args, line_index);
			else
				return function(words[1], args);
			
		case "return":
			if (words.length > 1 && !words[1].equals("")) {
				System.out.println("Error: End of line expected");
				return false;
			}
			return ret();
			
		default:
			System.out.println("Error: Invalid command");
			return false;
			
		}
		
	}
	
	
	//Split the line of VM code into words
	static String[] get_words(String line) {
		
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
	
	
	//Convert the given string to an integer; return -1 if the string is invalid
	static int parseInt(String str) {
		try {
			return Integer.parseUnsignedInt(str);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	
	//Check that the function name is valid and follows the format <file>.<function>
	static boolean validFunction(String func) {
		
		if (!validLabel(func)) {
			return false;
		}
		
		String names[] = func.split("\\.");
		if (names.length != 2) {
			System.out.println("Error: Function name must match the format <file>.<function>");
			return false;
		}
		
		return true;
		
	}
	
	
	//Check that the given label is valid for Hack VM
	static boolean validLabel(String label) {
		
		if (!Character.isLetter(label.charAt(0))) {
			System.out.println("Error: Label must begin with a letter");
			return false;
		}
		
		for (int i = 1; i < label.length(); i++) {
			if (!validChar(label.charAt(i))) {
				System.out.println("Error: Symbol may not contain " + label.charAt(i));
				return false;
			}
		}
		
		return true;
		
	}
	
	
	//Return true if the given character is allowed in Hack VM symbols
	static boolean validChar(char c) {
		if (Character.isLetterOrDigit(c) || c == '_' || c == '.' 
				|| c == '$')
			return true;
		else
			return false;
	}	
	
	
	//Generate bytecode for a function call
	static boolean call(String func, int args, int index) {
		
		if (math_flag) {
			if (func.equals("Math.multiply")) {
				il.append(new IMUL());
				il.append(new I2S());
				return true;
			}
			else if (func.equals("Math.divide")) {
				il.append(new IDIV());
				il.append(new I2S());
				return true;
			}
		}
		
		il.append(new INVOKESTATIC(cpg.addMethodref(methods.get(func))));
		return true;
		
	}
	

	//Generate bytecode for a function definition
	static boolean function(String func, int vars) {
		
		if (methods.containsKey(func)) {
			
			mg = methods.get(func);
			il = mg.getInstructionList();
			arg_count = mg.getArgumentTypes().length;
			
			if (debug_flag) {
				il.append(new GETSTATIC(cpg.addFieldref("java.lang.System", "out", "Ljava/io/PrintStream;")));
				il.append(new LDC(cpg.addString(func)));
				il.append(new INVOKEVIRTUAL(cpg.addMethodref("java.io.PrintStream", "println", "(Ljava/lang/String;)V")));
			}
			
			//Insert code so that the HackApplication thread sleeps when a function is called;
			//this allows keyboard input to get through
			il.append(new LCONST(0));
			il.append(new INVOKESTATIC(cpg.addMethodref("java.lang.Thread", "sleep", "(J)V")));
			
		}
		
		for (String label : label_found.keySet()) {
			if (label_found.get(label) != 0) {
				System.out.println("Error: Missing label");
				System.out.println(label);
				return false;
			}
		}
		
		//Don't remember labels from other functions
		labels.clear();
		label_found.clear();
		
		return true;
		
	}
	

	//Generate bytecode for a function return
	static boolean ret() {
		
		//In the Hack VM language, all functions return a value
		il.append(new IRETURN());
		return true;
		
	}
	
	
	//Create a MethodGen for the given function
	static void add_function(String func, int args, int line) {
		
		//Note where the function was first called
		if (!funcs.containsKey(func)) {
			funcs.put(func, line);
		}
		
		//Create a MethodGen for the function
		if (!methods.containsKey(func)) {
			
			Type t[] = new Type[args];
			String s[] = new String[args];
			for (int i = 0; i < args; i++) {
				t[i] = Type.SHORT;
				s[i] = "arg" + i;
			}

			String names[] = func.split("\\.");
			
			if (names[1].equals("new")) {
				names[1] = "NEW";
			}
			
			MethodGen ref = new MethodGen(Const.ACC_STATIC | Const.ACC_PUBLIC, Type.SHORT, t,
			        s, names[1], names[0], new InstructionList(), cpg);
			
			methods.put(func, ref);
			
		}
	}
	
	
	//Generate bytecode for a stack operation
	static boolean stack(String cmd, String seg, int i) {
		
		switch (cmd) {
		
		case "push":
			
			switch (seg) {
			
			case "constant":
				return push_const(i);
				
			case "local":
				//In Java bytecode, arguments and local variables are accessed with the same instruction
				//The first two local variables of the method are reserved for pointer 0 and pointer 1
				il.append(new ILOAD(i + arg_count + 2));
				return true;
				
			case "argument":
				il.append(new ILOAD(i));
				return true;
				
			case "this":
				//load pointer 0
				il.append(new ILOAD(arg_count));
				if (!push_const(i)) {
					return false;
				}
				il.append(new IADD());
				il.append(new INVOKESTATIC(cpg.addMethodref("HackComputer", "peek", "(I)S")));
				return true;
				
			case "that":
				//load pointer 1
				il.append(new ILOAD(arg_count + 1));
				if (!push_const(i)) {
					return false;
				}
				il.append(new IADD());
				il.append(new INVOKESTATIC(cpg.addMethodref("HackComputer", "peek", "(I)S")));
				return true;
				
			case "static":
				static_count = Math.max(static_count, i + 1);
				if (!push_const(i + static_start)) {
					return false;
				}
				il.append(new INVOKESTATIC(cpg.addMethodref("HackComputer", "pushStatic", "(I)S")));
				return true;
				
			case "pointer":
				if (i > 1) {
					System.out.println("Error: Pointer segment index may not exceed 1");
					return false;
				}
				if (i == 0) {
					//pointer 0
					il.append(new ILOAD(arg_count));
				}
				else {
					//pointer 1
					il.append(new ILOAD(arg_count + 1));
				}
				return true;
				
			case "temp":
				if (i > 7) {
					System.out.println("Error: Temp segment index may not exceed 7");
					return false;
				}
				if (!push_const(i)) {
					return false;
				}
				il.append(new INVOKESTATIC(cpg.addMethodref("HackComputer", "pushTemp", "(I)S")));
				return true;
				
			}
			
			break;
			
		case "pop":
			
			switch (seg) {
			
			case "local":
				//In Java bytecode, arguments and local variables are accessed with the same instruction
				//The first two local variables of the method are reserved for pointer 0 and pointer 1
				il.append(new ISTORE(i + arg_count + 2));
				return true;
				
			case "argument":
				il.append(new ISTORE(i));
				return true;
				
			case "this":
				//pointer 0
				il.append(new ILOAD(arg_count));
				if (!push_const(i)) {
					return false;
				}
				il.append(new IADD());
				il.append(new INVOKESTATIC(cpg.addMethodref("HackComputer", "poke", "(II)V")));
				return true;
				
			case "that":
				//pointer 1
				il.append(new ILOAD(arg_count + 1));
				if (!push_const(i)) {
					return false;
				}
				il.append(new IADD());
				il.append(new INVOKESTATIC(cpg.addMethodref("HackComputer", "poke", "(II)V")));
				return true;
				
			case "static":
				static_count = Math.max(static_count, i + 1);
				if (!push_const(i + static_start)) {
					return false;
				}
				il.append(new INVOKESTATIC(cpg.addMethodref("HackComputer", "popStatic", "(II)V")));
				return true;
				
			case "pointer":
				if (i > 1) {
					System.out.println("Error: Pointer segment index may not exceed 1");
					return false;
				}
				if (i == 0) {
					//pointer 0
					il.append(new ISTORE(arg_count));
				}
				else {
					//pointer 1
					il.append(new ISTORE(arg_count + 1));
				}
				return true;
				
			case "temp":
				if (i > 7) {
					System.out.println("Error: Temp segment index may not exceed 7");
					return false;
				}
				if (!push_const(i)) {
					return false;
				}
				il.append(new INVOKESTATIC(cpg.addMethodref("HackComputer", "popTemp", "(II)V")));
				return true;
				
			}
			
			break;
			
		}
		
		return false;
		
	}
	
	
	//Generate bytecode to push the given integer; return false if the integer is too large
	static boolean push_const(int i) {
		
		if (i >= -1 && i <= 5) {
			il.append(new ICONST(i));
		}
		
		else if (i >= -128 && i <= 127) {
			il.append(new BIPUSH((byte) i));
		}
		
		else if (i >= -32768 && i <= 32767) {
			il.append(new SIPUSH((short) i));
		}
		
		else {
			System.out.println("Error: Constant too large: " + i);
			return false;
		}
		
		return true;
		
	}

	
	//Generate bytecode for a mathematical stack operation
	static boolean logical(String cmd) {
		
		InstructionHandle h1, h2;
		
		switch (cmd) {
		
		case "add":
			il.append(new IADD());
			//The result must be truncated to a short value to match Hack computer specs
			il.append(new I2S());
			return true;
			
		case "sub":
			il.append(new ISUB());
			il.append(new I2S());
			return true;
			
		case "and":
			il.append(new IAND());
			return true;
			
		case "or":
			il.append(new IOR());
			return true;
			
		case "neg":
			il.append(new INEG());
			il.append(new I2S());
			return true;
			
		case "not":
			//The JVM has no command for bitwise NOT
			il.append(new ICONST(1));
			il.append(new IADD());
			il.append(new INEG());
			il.append(new I2S());
			return true;
			
		//The JVM doesn't have commands that correspond to Hack VM logical commands
		case "eq":
			h1 = il.append(new ICONST(-1));
			il.append(new IF_ICMPEQ(h1));
			il.append(new ICONST(0));
			h2 = il.append(new NOP());
			il.append(new GOTO(h2));
			il.move(h1, il.getEnd());
			il.move(h2, il.getEnd());
			return true;
			
		case "gt":
			h1 = il.append(new ICONST(-1));
			il.append(new IF_ICMPGT(h1));
			il.append(new ICONST(0));
			h2 = il.append(new NOP());
			il.append(new GOTO(h2));
			il.move(h1, il.getEnd());
			il.move(h2, il.getEnd());
			return true;
			
		case "lt":
			h1 = il.append(new ICONST(-1));
			il.append(new IF_ICMPLT(h1));
			il.append(new ICONST(0));
			h2 = il.append(new NOP());
			il.append(new GOTO(h2));
			il.move(h1, il.getEnd());
			il.move(h2, il.getEnd());
			return true;
			
		}
		
		return false;
		
	}

	
	//Generate bytecode for a branch instruction
	static boolean branch(String cmd, String label, int index) {
		
		if (!validLabel(label))
			return false;
		
		switch (cmd) {
		
		case "goto":
			if (!labels.containsKey(label)) {
				//Until we find the label, link to an instruction stored in the labels table
				labels.put(label, il.append(new LCONST(0)));
				label_found.put(label, index);
			}
			il.append(new GOTO(labels.get(label)));
			return true;
			
		case "if-goto":
			if (!labels.containsKey(label)) {
				//Until we find the label, link to an instruction stored in the labels table
				labels.put(label, il.append(new LCONST(0)));
				label_found.put(label, index);
			}
			il.append(new IFNE(labels.get(label)));
			return true;
			
		case "label":
			if (!labels.containsKey(label)) {
				//Insert a call to Thread.sleep() after each label; this allows keyboard input
				//to get through during input loops
				labels.put(label, il.append(new LCONST(0)));
				il.append(new INVOKESTATIC(cpg.addMethodref("java.lang.Thread", "sleep", "(J)V")));
				label_found.put(label, 0);
			}
			else if (label_found.get(label) == 0) {
				System.out.println("Error: Duplicate label");
				return false;
			}
			else {
				label_found.replace(label, 0);
				il.move(labels.get(label), il.getEnd());
				il.append(new INVOKESTATIC(cpg.addMethodref("java.lang.Thread", "sleep", "(J)V")));
			}
			return true;
			
		}
		
		return false;
		
	}
	
}
