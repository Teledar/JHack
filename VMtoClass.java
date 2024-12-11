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
	
	//Indexes to the RAM arrays in the constant pool
	static int ram_index, temps_index, statics_index;

	//The number of arguments of the current method
	static int arg_count;
	
	//Specific to the current .vm file; which index to store its static variables in the RAM
	//and how many static variables it has
	static int static_start, static_count;
	
	//A boolean value indicating whether the function is ever called; if not, the function is not added
	//to the generated class file
	static boolean write_function;
	
	//If set to true, code will be inserted into each generated method that causes it to print its name
	//when called; a rudimentary stack trace for debugging the output class file
	static boolean debug_flag = false;
	
	//If set to true, calls to Math.multiply and Math.divide will be replaced with JVM instructions
	//instead of calling the methods implemented in the .vm input files
	static boolean math_flag = true;
	
	//Temporary; the args that are passed to VMtoClass when it starts
	static String[] temp = {"C:\\nand2tetris\\nand2tetris\\projects\\13\\Chess"};
	
	public static void main(String[] args) {
		
		//Use the built-in temp args
		args = temp;

		System.out.println("nand2tetris VM to .class translator");
		System.out.println("www.nand2tetris.org");
		System.out.println();
		
		if (!getArgs(args)) {
			return;
		}
		
		funcs.put("Sys.init", 1);
		
		initialize_class();
		
		boolean good = false;
		
		for (int i = 0; i < inFiles.length; i++) {
			good = find_functions(inFiles[i]);
			if (!good) break;
		}
		
		for (String func : funcs.keySet()) {
			if (funcs.get(func) != 0) {
				System.out.println("Error: Missing function");
				System.out.println(func);
				good = false;
			}
		}
		
		if (good ) {
			static_start = 0;
			static_count = 0;
			for (int i = 0; i < inFiles.length; i++) {
				good = translate(inFiles[i]);
				if (!good) break;
			}
		}

		for (String label : label_found.keySet()) {
			if (label_found.get(label) != 0) {
				System.out.println("Error: Missing label");
				System.out.println(label);
				good = false;
			}
		}
		
		if (good) {
			for (MethodGen m : methods.values()) {
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
			JavaClass jc = cg.getJavaClass();
			
			try {
				jc.dump(outPath.toString());
				System.out.println("Assembly code saved to:");
			} catch (IOException e) {
				System.out.println("Error writing to file: ");
			}
			System.out.println(outPath.toString());	
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
		
		outPath = inPath.resolve("HackApplication.class");
		
		return true;
	}
	
	
	static void printHelp() {
		System.out.println("SYNTAX");
		System.out.println("VMtoClass <inDir>");
		System.out.println("\tTranslates all .vm files in inDir from Hack VM language to a single Java class file.");
		System.out.println("\tThe output is saved to HackApplication.class.");
	}
	
	
	//Sets up the underlying implementation of the Hack computer
	private static void initialize_class() {
		
		cg = new ClassGen("HackApplication", "java.lang.Object",
		        "<generated>", Const.ACC_PUBLIC | Const.ACC_SUPER | Const.ACC_STATIC | Const.ACC_FINAL, null);
		
		cpg = cg.getConstantPool();
		
		//A method named Sys.init must be implemented in the input files
		methods.put("Sys.init", new MethodGen(Const.ACC_STATIC | Const.ACC_PUBLIC, Type.SHORT, new Type[] {},
		        new String[] {}, "Sys_init", "HackApplication", new InstructionList(), cpg));
		
		//An array of short integers that represents the RAM of the Hack computer
		FieldGen fg = new FieldGen(Const.ACC_STATIC | Const.ACC_PRIVATE, new ArrayType(Type.SHORT, 1), "ram", cpg);
		cg.addField(fg.getField());

		//An array of short integers that represents the "temp" segment of the Hack computer;
		//"push temp 0" is translated to "temps[0] = value;"
		fg = new FieldGen(Const.ACC_STATIC | Const.ACC_PRIVATE, new ArrayType(Type.SHORT, 1), "temps", cpg);
		cg.addField(fg.getField());

		//An array of short integers that represents the "static" segment of the Hack computer;
		fg = new FieldGen(Const.ACC_STATIC | Const.ACC_PRIVATE, new ArrayType(Type.SHORT, 1), "statics", cpg);
		cg.addField(fg.getField());
		
		//Get the indexes of the RAM arrays in the constant pool
		ram_index = cpg.addFieldref("HackApplication", "ram", "[S");
		temps_index = cpg.addFieldref("HackApplication", "temps", "[S");
		statics_index = cpg.addFieldref("HackApplication", "statics", "[S");
		
		//Create the run() method; this is called to start the Hack application
		il = new InstructionList();
		il.append(new ALOAD(0));
		il.append(new PUTSTATIC(ram_index));
		il.append(new BIPUSH((byte) 8));
		il.append(new NEWARRAY(Type.SHORT));
		il.append(new PUTSTATIC(temps_index));
		il.append(new SIPUSH((short) 240));
		il.append(new NEWARRAY(Type.SHORT));
		il.append(new PUTSTATIC(statics_index));
		il.append(new INVOKESTATIC(cpg.addMethodref("HackApplication", "Sys_init", "()S")));
		il.append(new POP());
		il.append(new RETURN());
		mg = new MethodGen(Const.ACC_STATIC | Const.ACC_PUBLIC, Type.VOID, new Type[] { new ArrayType(Type.SHORT, 1) },
		        new String[] { "ram_array" }, "run", "HackApplication", il, cpg);
		mg.setMaxLocals();
		mg.setMaxStack();
		cg.addMethod(mg.getMethod());
		il.dispose();
		
		//Create the getName() accessor; returns the name of the Hack application
		il = new InstructionList();
		il.append(new LDC(cpg.addString(inPath.getFileName().toString())));
		il.append(new ARETURN());
		mg = new MethodGen(Const.ACC_STATIC | Const.ACC_PUBLIC, Type.STRING, new Type[] { },
		        new String[] { }, "getName", "HackApplication", il, cpg);
		mg.setMaxLocals();
		mg.setMaxStack();
		cg.addMethod(mg.getMethod());
		il.dispose();
		
	}

	
	//Preprocess the input .vm files: find all the functions and the number of arguments
	static boolean find_functions(String file) {
		
		int line_index = 0;
		
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
							if (args > -1 && validLabel(words[1])) {
								add_function(words[1], args, line_index);
							}
						}
						
					} 
					
					//Note that the function definition was found, but do not add a MethodGen yet
					else if (line.startsWith("function ")) {
						String words[] = get_words(line);
						if (words.length == 3) {
							String func = words[1];
							if (validLabel(func)) {
								if (!funcs.containsKey(func)) {
									funcs.put(func, 0);
								} 
								else if (funcs.get(func) == 0) {
									System.out.println("Error: Duplicate function " + func);
									return false;
								}
								funcs.replace(func, 0);
							}
						}
					}
					
					line = reader.readLine();
					
				}
				
				reader.close();
				
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
				
				boolean back = parseLines();
				
				reader.close();
				
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
		
		//If this function is never called, do not generate bytecode for it
		if (!write_function && !words[0].toLowerCase().equals("function")) {
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
			if (!validLabel(words[1]))
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
			write_function = true;
			
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
		
		//Don't generate bytecode if the function is never called
		else {
			write_function = false;
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
	static void add_function(String func, int vars, int line) {
		
		//Note where the function was first called
		if (!funcs.containsKey(func)) {
			funcs.put(func, line);
		}
		
		//Create a MethodGen for the function
		if (!methods.containsKey(func)) {
			
			Type t[] = new Type[vars];
			String s[] = new String[vars];
			for (int i = 0; i < vars; i++) {
				t[i] = Type.SHORT;
				s[i] = "arg" + i;
			}
			
			//Dots in VM functions are replaced with underscores in the bytecode
			MethodGen ref = new MethodGen(Const.ACC_STATIC | Const.ACC_PUBLIC, Type.SHORT, t,
			        s, func.replace('.', '_'), "HackApplication", new InstructionList(), cpg);
			
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
				il.append(new GETSTATIC(ram_index));
				//load pointer 0
				il.append(new ILOAD(arg_count));
				if (!push_const(i)) {
					return false;
				}
				il.append(new IADD());
				il.append(new SALOAD());
				return true;
				
			case "that":
				il.append(new GETSTATIC(ram_index));
				//load pointer 1
				il.append(new ILOAD(arg_count + 1));
				if (!push_const(i)) {
					return false;
				}
				il.append(new IADD());
				il.append(new SALOAD());
				return true;
				
			case "static":
				static_count = Math.max(static_count, i + 1);
				il.append(new GETSTATIC(statics_index));
				if (!push_const(i + static_start)) {
					return false;
				}
				il.append(new SALOAD());
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
				il.append(new GETSTATIC(temps_index));
				if (!push_const(i)) {
					return false;
				}
				il.append(new SALOAD());
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
				il.append(new GETSTATIC(ram_index));
				//We need to swap values on the stack, because the JVM SASTORE instruction requires the
				//array and subscript to come before the value being stored
				il.append(new SWAP());
				//pointer 0
				il.append(new ILOAD(arg_count));
				if (!push_const(i)) {
					return false;
				}
				il.append(new IADD());
				il.append(new SWAP());
				il.append(new SASTORE());
				return true;
				
			case "that":
				il.append(new GETSTATIC(ram_index));
				il.append(new SWAP());
				//pointer 1
				il.append(new ILOAD(arg_count + 1));
				if (!push_const(i)) {
					return false;
				}
				il.append(new IADD());
				il.append(new SWAP());
				il.append(new SASTORE());
				return true;
				
			case "static":
				static_count = Math.max(static_count, i + 1);
				il.append(new GETSTATIC(statics_index));
				il.append(new SWAP());
				if (!push_const(i + static_start)) {
					return false;
				}
				il.append(new SWAP());
				il.append(new SASTORE());
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
				il.append(new GETSTATIC(temps_index));
				il.append(new SWAP());
				if (!push_const(i)) {
					return false;
				}
				il.append(new SWAP());
				il.append(new SASTORE());
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
