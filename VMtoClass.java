import org.apache.bcel.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.classfile.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

public class VMtoClass {

	static Path outPath;
	static Path inPath;
	static String inFiles[];
	static String current_file;
	static BufferedReader reader;
	static Hashtable<String, InstructionHandle> labels = new Hashtable<String, InstructionHandle>();
	static Hashtable<String, Integer> label_found = new Hashtable<String, Integer>();
	static Hashtable<String, Integer> funcs = new Hashtable<String, Integer>();
	static Hashtable<String, MethodGen> methods = new Hashtable<String, MethodGen>();
	
	static ClassGen cg;
	static MethodGen mg;
	static InstructionList il;
	static ConstantPoolGen cpg;
	static int arg_count, ram_index, temps_index, statics_index, static_start, static_count;

	static boolean write_function;
	
	static boolean debug_flag = false;
	
	static String[] temp = {"C:\\nand2tetris\\nand2tetris\\projects\\13\\chess-vm-files-main"};
	
	public static void main(String[] args) {
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

	
	//Examines the args sent to the program and stores them in the appropriate
	//variables; prints a message and returns false if an arg is incorrect
	static boolean getArgs(String[] args) {
		for (String arg : args) {
			if (arg.equals("/h") || arg.equals("/help") || arg.equals("-h") || arg.equals("--help")) {
				printHelp();
				return false;
			}
		}
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
		System.out.println("vm2c <inDir>");
		System.out.println("\tTranslates all .vm files in inDir from Hack VM language to a single Java class file.");
		System.out.println("\tThe output is saved to HackApplication.class.");
	}
	
	
	private static void initialize_class() {
		cg = new ClassGen("HackApplication", "java.lang.Object",
		        "<generated>", Const.ACC_PUBLIC | Const.ACC_SUPER | Const.ACC_STATIC | Const.ACC_FINAL, null);
		
		cpg = cg.getConstantPool();

		methods.put("Sys.init", new MethodGen(Const.ACC_STATIC | Const.ACC_PUBLIC, Type.SHORT, new Type[] {},
		        new String[] {}, "Sys_init", "HackApplication", new InstructionList(), cpg));
		
		FieldGen fg = new FieldGen(Const.ACC_STATIC | Const.ACC_PRIVATE, new ArrayType(Type.SHORT, 1), "ram", cpg);
		cg.addField(fg.getField());
		fg = new FieldGen(Const.ACC_STATIC | Const.ACC_PRIVATE, new ArrayType(Type.SHORT, 1), "temps", cpg);
		cg.addField(fg.getField());
		fg = new FieldGen(Const.ACC_STATIC | Const.ACC_PRIVATE, new ArrayType(Type.SHORT, 1), "statics", cpg);
		cg.addField(fg.getField());
		fg = new FieldGen(Const.ACC_STATIC | Const.ACC_PUBLIC | Const.ACC_FINAL, Type.STRING, "name", cpg);
		fg.setInitValue(inPath.getFileName().toString());
		cg.addField(fg.getField());
		
		ram_index = cpg.addFieldref("HackApplication", "ram", "[S");
		temps_index = cpg.addFieldref("HackApplication", "temps", "[S");
		statics_index = cpg.addFieldref("HackApplication", "statics", "[S");
		
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
	}

	
	static boolean find_functions(String file) {
		int count = 0;
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
					count++;
					line = line.trim();
					if (line.contains("//")) {
						line = line.substring(0, line.indexOf("//")).trim();
					}
					if (line.startsWith("call ")) {
						String words[] = get_words(line);
						if (words.length == 3) {
							int args = parseInt(words[2]);
							if (args > -1 && validLabel(words[1])) {
								add_function(words[1], args, count);
							}
						}
					} else if (line.startsWith("function ")) {
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
	
	
	static boolean translate(String file) {
		try {
			Path p = inPath.resolve(file);
			if (!Files.isDirectory(p)) {
				reader = Files.newBufferedReader(p);
				int dot = file.lastIndexOf('.');
				if (dot > -1) {
					file = file.substring(0, dot);
				}
				if (!current_file.equals("")) {
					
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

	
	static boolean parseLines() {
		int count = 0;
		try {
			String line = reader.readLine();
			while (line != null) {
				count++;
				line = line.trim();
				if (line.contains("//")) {
					line = line.substring(0, line.indexOf("//")).trim();
				}
				if (!parse(line, count)) {
					System.out.println("Line " + count);
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
	
	
	static boolean parse(String line, int index) {
		String words[] = get_words(line);
		if (words.length == 0 || words[0].equals("")) {
			return true;
		}
		
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
			return branch(words[0].toLowerCase(), words[1], index);
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
				return call(words[1], args, index);
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
	
	
	static int parseInt(String str) {
		try {
			return Integer.parseUnsignedInt(str);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	
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
	
	
	static boolean validChar(char c) {
		if (Character.isLetterOrDigit(c) || c == '_' || c == '.' 
				|| c == '$')
			return true;
		else
			return false;
	}	
	
	
	static boolean call(String func, int args, int index) {
		MethodGen ref = methods.get(func);
		il.append(new INVOKESTATIC(cpg.addMethodref(ref)));
		return true;
	}
	
	
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
			il.append(new LCONST(0));
			il.append(new INVOKESTATIC(cpg.addMethodref("java.lang.Thread", "sleep", "(J)V")));
		} else {
			write_function = false;
		}
		labels.clear();
		return true;
	}
	

	static boolean ret() {
		il.append(new IRETURN());
		return true;
	}
	
	
	static void add_function(String func, int vars, int line) {
		if (!funcs.containsKey(func)) {
			funcs.put(func, line);
		}
		if (!methods.containsKey(func)) {
			Type t[] = new Type[vars];
			String s[] = new String[vars];
			for (int i = 0; i < vars; i++) {
				t[i] = Type.SHORT;
				s[i] = "arg" + i;
			}
			MethodGen ref = new MethodGen(Const.ACC_STATIC | Const.ACC_PUBLIC, Type.SHORT, t,
			        s, func.replace('.', '_'), "HackApplication", new InstructionList(), cpg);
			methods.put(func, ref);
		}
	}
	
	
	static boolean stack(String cmd, String seg, int i) {
		switch (cmd) {
		case "push":
			switch (seg) {
			case "constant":
				return push_const(i);
			case "local":
				il.append(new ILOAD(i + arg_count + 2));
				return true;
			case "argument":
				il.append(new ILOAD(i));
				return true;
			case "this":
				il.append(new GETSTATIC(ram_index));
				il.append(new ILOAD(arg_count));
				if (!push_const(i)) {
					return false;
				}
				il.append(new IADD());
				il.append(new SALOAD());
				return true;
			case "that":
				il.append(new GETSTATIC(ram_index));
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
					il.append(new ILOAD(arg_count));
				}
				else {
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
				il.append(new ISTORE(i + arg_count + 2));
				return true;
			case "argument":
				il.append(new ISTORE(i));
				return true;
			case "this":
				il.append(new GETSTATIC(ram_index));
				il.append(new SWAP());
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
					il.append(new ISTORE(arg_count));
				}
				else {
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

	
	static boolean logical(String cmd) {
		InstructionHandle h1, h2;
		switch (cmd) {
		case "add":
			il.append(new IADD());
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
			il.append(new ICONST(1));
			il.append(new IADD());
			il.append(new INEG());
			il.append(new I2S());
			return true;
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

	
	static boolean branch(String cmd, String label, int index) {
		if (!validLabel(label))
			return false;
		switch (cmd) {
		case "goto":
			if (!labels.containsKey(label)) {
				labels.put(label, il.append(new LCONST(0)));
				label_found.put(label, index);
			}
			il.append(new GOTO(labels.get(label)));
			return true;
		case "if-goto":
			if (!labels.containsKey(label)) {
				labels.put(label, il.append(new LCONST(0)));
				label_found.put(label, index);
			}
			il.append(new IFNE(labels.get(label)));
			return true;
		case "label":
			if (!labels.containsKey(label)) {
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