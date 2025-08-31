/**
 * JHack - https://github.com/Teledar/JHack
 * This class writes the Java bytecode translation of a Nand2Tetris Hack VM code for JHack,
 * a Java-based emulator of the Nand to Tetris Hack computer.
 * Nand to Tetris - https://www.nand2tetris.org/
 */

// Apache Commons BCEL
// Copyright 2004-2024 The Apache Software Foundation

// This product includes software developed at
// The Apache Software Foundation (https://www.apache.org/).

package compiler;

import org.apache.bcel.generic.*;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

/**
 * Writes the Java bytecode translation of a Nand to Tetris Hack VM file
 */
public class CodeWriter {
	
	// The full path of the output class file
	private Path outFile;
	
	// The class file
	private ClassGen clss;
	
	// The class file's constant pool
	private ConstantPoolGen pool;
	
	// The bytecode methods generated from the .vm files
	private Hashtable<String, MethodGen> methods = new Hashtable<String, MethodGen>();
	
	// The code of the current method
	private InstructionList code;
	
	// The number of arguments of the current method
	private int argCount;
	
	// The number of static fields in the class
	private int staticCount = 0;
	
	// A table of labels found in the current function, and the corresponding destination that they
	// point to in the Java bytecode
	private Hashtable<String, InstructionHandle> labels = new Hashtable<String, InstructionHandle>();
	
	/**
	 * Constructs a new CodeWriter to create the given file
	 * @file the full path of the output class file
	 */
	public CodeWriter(Path file) {
		file = file.toAbsolutePath();
		if (Files.isDirectory(file)) {
			throw new IllegalArgumentException("Path must be a file.");
		}
		outFile = file;
		
		String name = file.getFileName().toString();
		if (name.contains(".")) {
			name = name.substring(0, name.lastIndexOf("."));
		}
		clss = new ClassGen(name, "java.lang.Object", name + ".vm", 
				Const.ACC_PUBLIC | Const.ACC_SUPER | Const.ACC_STATIC, null);
		
		pool = clss.getConstantPool();
	}
	
	/**
	 * Writes an arithmetic instruction
	 * @param command the Hack VM arithmetic instruction
	 */
	public void writeArithmetic(String command) {

		InstructionHandle h1, h2;
		
		switch (command) {
		case "add":
			code.append(new IADD());
			// The result must be truncated to a short value to match Hack computer specs
			code.append(new I2S());
			break;
			
		case "sub":
			code.append(new ISUB());
			code.append(new I2S());
			break;
			
		case "neg":
			code.append(new INEG());
			code.append(new I2S());
			break;			
			
		// The JVM doesn't have commands that correspond to Hack VM logical commands
		case "eq":
			h1 = code.append(new ICONST(-1));
			code.append(new IF_ICMPEQ(h1));
			code.append(new ICONST(0));
			h2 = code.append(new NOP());
			code.append(new GOTO(h2));
			code.move(h1, code.getEnd());
			code.move(h2, code.getEnd());
			break;
			
		case "gt":
			h1 = code.append(new ICONST(-1));
			code.append(new IF_ICMPGT(h1));
			code.append(new ICONST(0));
			h2 = code.append(new NOP());
			code.append(new GOTO(h2));
			code.move(h1, code.getEnd());
			code.move(h2, code.getEnd());
			break;
			
		case "lt":
			h1 = code.append(new ICONST(-1));
			code.append(new IF_ICMPLT(h1));
			code.append(new ICONST(0));
			h2 = code.append(new NOP());
			code.append(new GOTO(h2));
			code.move(h1, code.getEnd());
			code.move(h2, code.getEnd());
			break;
			
		case "and":
			code.append(new IAND());
			break;
			
		case "or":
			code.append(new IOR());
			break;
			
		case "not":
			// The JVM has no command for bitwise NOT
			code.append(new ICONST(1));
			code.append(new IADD());
			code.append(new INEG());
			code.append(new I2S());
			break;
			
		default:
			throw new IllegalArgumentException("Illegal command: " + command);
		}
	}
	
	/**
	 * Writes a push or pop instruction
	 * @param command PUSH or POP
	 * @param segment the Hack memory segment to push from or pop to
	 * @param index the index of the Hack memory segment to push from or pop to
	 */
	public void writePushPop(Parser.Command command, String segment, int index) {

		if (index < 0) {
			throw new IllegalArgumentException("Segment index may not be negative");
		}
		
		switch (command) {
		
		case PUSH:
			
			switch (segment) {
			
			case "constant":
				pushConst(index);
				break;
				
			case "local":
				//In Java bytecode, arguments and local variables are accessed with the same instruction
				//The first two local variables of the method are reserved for pointer 0 and pointer 1
				code.append(new ILOAD(index + argCount + 2));
				break;
				
			case "argument":
				code.append(new ILOAD(index));
				break;
				
			case "this":
				//load pointer 0
				code.append(new ILOAD(argCount));
				pushConst(index);
				code.append(new IADD());
				code.append(new INVOKESTATIC(pool.addMethodref("HackComputer", "peek", "(I)S")));
				break;
				
			case "that":
				//load pointer 1
				code.append(new ILOAD(argCount + 1));
				pushConst(index);
				code.append(new IADD());
				code.append(new INVOKESTATIC(pool.addMethodref("HackComputer", "peek", "(I)S")));
				break;
				
			case "static":
				code.append(new GETSTATIC(pool.addFieldref(clss.getClassName(), "static" + index, "S")));
				staticCount = Math.max(staticCount, index + 1);
				break;
				
			case "pointer":
				if (index > 1) {
					throw new IllegalArgumentException("Pointer segment index may not exceed 1");
				}
				if (index == 0) {
					//pointer 0
					code.append(new ILOAD(argCount));
				}
				else {
					//pointer 1
					code.append(new ILOAD(argCount + 1));
				}
				break;
				
			case "temp":
				if (index > 7) {
					throw new IllegalArgumentException("Temp segment index may not exceed 7");
				}
				pushConst(index);
				code.append(new INVOKESTATIC(pool.addMethodref("HackComputer", "pushTemp", "(I)S")));
				break;
				
			default:
				throw new IllegalArgumentException("Invalid segment: " + segment);
			}
			
			break;
			
		case POP:
			
			switch (segment) {
			
			case "local":
				//In Java bytecode, arguments and local variables are accessed with the same instruction
				//The first two local variables of the method are reserved for pointer 0 and pointer 1
				code.append(new ISTORE(index + argCount + 2));
				break;
				
			case "argument":
				code.append(new ISTORE(index));
				break;
				
			case "this":
				//pointer 0
				code.append(new ILOAD(argCount));
				pushConst(index);
				code.append(new IADD());
				code.append(new INVOKESTATIC(pool.addMethodref("HackComputer", "poke", "(II)V")));
				break;
				
			case "that":
				//pointer 1
				code.append(new ILOAD(argCount + 1));
				pushConst(index);
				code.append(new IADD());
				code.append(new INVOKESTATIC(pool.addMethodref("HackComputer", "poke", "(II)V")));
				break;
				
			case "static":
				code.append(new PUTSTATIC(pool.addFieldref(clss.getClassName(), "static" + index, "S")));
				staticCount = Math.max(staticCount, index + 1);
				break;
				
			case "pointer":
				if (index > 1) {
					throw new IllegalArgumentException("Pointer segment index may not exceed 1");
				}
				if (index == 0) {
					//pointer 0
					code.append(new ISTORE(argCount));
				}
				else {
					//pointer 1
					code.append(new ISTORE(argCount + 1));
				}
				break;
				
			case "temp":
				if (index > 7) {
					throw new IllegalArgumentException("Temp segment index may not exceed 7");
				}
				pushConst(index);
				code.append(new INVOKESTATIC(pool.addMethodref("HackComputer", "popTemp", "(II)V")));
				break;
				
			case "constant":
				throw new IllegalArgumentException("Cannot pop constant");
				
			default:
				throw new IllegalArgumentException("Invalid segment: " + segment);
			}
			
			break;
			
		default:
			throw new IllegalArgumentException("Invalid command" + command);

		}
		
	}
	
	/**
	 * Writes a label
	 * @param label the name of the label
	 */
	public void writeLabel(String label) {
		if (!labels.containsKey(label)) {
			//Insert a call to Thread.sleep() after each label; this allows keyboard input
			//to get through during input loops
			labels.put(label, code.append(new LCONST(0)));
			code.append(new INVOKESTATIC(pool.addMethodref("java.lang.Thread", "sleep", "(J)V")));
		}
		else {
			code.move(labels.get(label), code.getEnd());
			code.append(new INVOKESTATIC(pool.addMethodref("java.lang.Thread", "sleep", "(J)V")));
		}
	}
	

	/**
	 * Writes a goto instruction
	 * @param label the target of the goto instruction
	 */
	public void writeGoto(String label) {
		if (!labels.containsKey(label)) {
			//Until we find the label, link to an instruction stored in the labels table
			labels.put(label, code.append(new LCONST(0)));
		}
		code.append(new GOTO(labels.get(label)));
	}
	

	/**
	 * Writes an if-goto instruction
	 * @param label the target of the goto instruction
	 */
	public void writeIf(String label) {
		if (!labels.containsKey(label)) {
			//Until we find the label, link to an instruction stored in the labels table
			labels.put(label, code.append(new LCONST(0)));
		}
		code.append(new IFNE(labels.get(label)));
	}
	
	/**
	 * Writes a function definition
	 * @param function the name of the function
	 * @param nLocals the number of local variables in the function
	 * @param nArgs the number of arguments in the function definition
	 */
	public void writeFunction(String function, int nLocals, int nArgs) {

		String names[] = function.split("\\.");
		
		if (names[1].equals("new")) {
			names[1] = "NEW";
		}
		
		if (names.length != 2 || !names[0].equals(clss.getClassName())) {
			throw new IllegalArgumentException("Function name must match the format <file>.<function>");
		}
		
		// Method overloading is not supported
		if (methods.containsKey(names[1])) {
			throw new IllegalArgumentException("Function " + function + " already exists");
		}
		
		if (nLocals < 0) {
			throw new IllegalArgumentException("Number of local variables may not be negative");
		}
		
		if (nArgs < 0) {
			throw new IllegalArgumentException("Number of arguments may not be negative");
		}
		
		// Label names only have scope of the current function
		labels.clear();

		Type t[] = new Type[nArgs];
		String s[] = new String[nArgs];
		for (int i = 0; i < nArgs; i++) {
			t[i] = Type.SHORT;
			s[i] = "arg" + i;
		}
		
		MethodGen ref = new MethodGen(Const.ACC_STATIC | Const.ACC_PUBLIC, Type.SHORT, t,
		        s, names[1], names[0], new InstructionList(), pool);
		
		methods.put(names[1], ref);
		
		code = ref.getInstructionList();
		
		argCount = nArgs;
		

		//Insert code so that the HackApplication thread sleeps when a function is called;
		//this allows keyboard input to get through
		code.append(new LCONST(0));
		code.append(new INVOKESTATIC(pool.addMethodref("java.lang.Thread", "sleep", "(J)V")));
		
	}
	
	/**
	 * Writes a function call
	 * @param function the name of the function to call
	 * @param nArgs the number of arguments in the definition of the called function
	 */
	public void writeCall(String function, int nArgs) {
		
		// Replace these specific OS functions with JVM stack operations which are much faster
		if (function.equals("Math.multiply")) {
			code.append(new IMUL());
			code.append(new I2S());
			return;
		}
		else if (function.equals("Math.divide")) {
			code.append(new IDIV());
			code.append(new I2S());
			return;
		}
		

		String names[] = function.split("\\.");
		
		if (names[1].equals("new")) {
			names[1] = "NEW";
		}
		
		if (names.length != 2) {
			throw new IllegalArgumentException("Function name must match the format <file>.<function>");
		}
		
		if (nArgs < 0) {
			throw new IllegalArgumentException("Number of arguments may not be negative");
		}
		
		String signature = "(" + "S".repeat(nArgs) + ")S";
		
		code.append(new INVOKESTATIC(pool.addMethodref(names[0], names[1], signature)));
		
	}
	
	/**
	 * Writes a return instruction
	 */
	public void writeReturn() {
		// In the Hack VM language, all functions return a value
		code.append(new IRETURN());
	}
	
	/**
	 * Finalizes, writes, and closes the output class file
	 */
	public void close() throws IOException {

		for (String name : methods.keySet()) {
			
			MethodGen m = methods.get(name);
			
			m.setConstantPool(pool);
			
			m.setMaxLocals();
			
			//we need to initialize all the local variables to 0
			code = m.getInstructionList();
			for (int i = m.getMaxLocals() - 1; i >= m.getArgumentTypes().length; i--) {
				code.insert(new ISTORE(i));
				code.insert(new ICONST(0));
			}
			
			m.setMaxStack();
			clss.addMethod(m.getMethod());	
			
		}
		
		// Add static initializer and static fields
		if (staticCount > 0) {
			MethodGen staticInitializer = new MethodGen(Const.ACC_STATIC, Type.VOID, new Type[0], 
				new String[0], "<clinit>", clss.getClassName(), new InstructionList(), pool);
			
			code = staticInitializer.getInstructionList();

			for (int i = 0; i < staticCount; i++) {
				FieldGen fg = new FieldGen(Const.ACC_PRIVATE | Const.ACC_STATIC, Type.SHORT, "static" + i, pool);
				clss.addField(fg.getField());
				code.append(new ICONST(0));
				code.append(new PUTSTATIC(pool.addFieldref(clss.getClassName(), "static" + i, "S")));
			}

			code.append(new RETURN());
			staticInitializer.setMaxLocals();
			staticInitializer.setMaxStack();
			clss.addMethod(staticInitializer.getMethod());
		}
		
		JavaClass jc = clss.getJavaClass();
		jc.dump(outFile.toString());
		
	}
	
	/**
	 * Generates bytecode to push the given integer
	 * @param i the integer to push to the stack
	 */
	private void pushConst(int i) {
		
		if (i >= -1 && i <= 5) {
			code.append(new ICONST(i));
		}
		
		else if (i >= -128 && i <= 127) {
			code.append(new BIPUSH((byte) i));
		}
		
		else if (i >= -32768 && i <= 32767) {
			code.append(new SIPUSH((short) i));
		}
		
		else {
			throw new IllegalArgumentException("Constant out of range: " + i);
		}
		
	}
}
