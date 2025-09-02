/**
 * JHack - https://github.com/Teledar/JHack
 * This class writes the Java bytecode translation of a Nand2Tetris Hack VM code for JHack,
 * a Java-based emulator of the Nand to Tetris Hack computer.
 * Nand to Tetris - https://www.nand2tetris.org/
 */

package compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Consumer;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.Label;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import compiler.Parser.Command;


/**
 * Writes the Java bytecode translation of a Nand to Tetris Hack VM file
 */
public class ClassWriter implements Consumer<ClassBuilder> {
	
	// The full path of the input VM file
	private Path inputFile;

	// The full path of the output class file
	private Path outputFile;

    // The name of the class
    private String className;

    // The parser for the input file
    private Parser parser;

    // The exception to throw if a file operation fails
    private IOException ioException;
    
    // The exception to throw if a compile fails
    private IllegalArgumentException argException;

    // The number of static variables in the class
    private int staticCount = 0;
	

	/**
	 * Constructs a new ClassWriter to write a class for the given file
	 * @param inputFile the full path of the input class file
	 */
	public ClassWriter(Path inputFile) {
		inputFile = inputFile.toAbsolutePath();
		if (Files.isDirectory(inputFile)) {
			throw new IllegalArgumentException("Input path must be a file.");
		}
		this.inputFile = inputFile;

		String name = inputFile.getFileName().toString();
		if (name.contains(".")) {
			name = name.substring(0, name.lastIndexOf("."));
		}
        className = name;

		outputFile = inputFile.getParent().resolve(name + ".class");
		
	}


    /**
     * Parses the input file and creates a class file
     */
    public void compile() throws IOException {

        try {
            parser = new Parser(inputFile);
            parser.advance();

            if (parser.getType() != Command.FUNC) {
                throw new IllegalArgumentException("Line " + parser.getLineIndex() + 
                    ": function expected");
            }

        } catch (IOException e) {
            ioException = new IOException("Failed to read file: " + inputFile, e);
            throw ioException;
        } catch (IllegalArgumentException e) {
            argException = e;
            throw argException;
        }

        try {
            ClassFile.of().buildTo(outputFile, ClassDesc.of(className), this);;
        } catch (IOException e) {
            ioException = new IOException("Failed to write file: " + outputFile, e);
            throw ioException;
        }

        if (ioException != null) {
            throw ioException;
        }

        if (argException != null) {
            throw argException;
        }

    }


    @Override
    public void accept(ClassBuilder clss) {
        clss.withVersion(45, 3);
        clss.withSuperclass(ConstantDescs.CD_Object);

        while (parser.moreLines()) {
            
            String methodName = parser.getArg1();
            int numArgs = parser.getFuncArgs(methodName);
            int numLocals = parser.getArg2();
            
            methodName = methodName.split("\\.")[1];

            if (methodName.equals("new")) {
                methodName = "NEW";
            }

            clss.withMethodBody(methodName, MethodTypeDesc.of(ConstantDescs.CD_short,
                Collections.nCopies(numArgs, ConstantDescs.CD_short)),
                ClassFile.ACC_STATIC | ClassFile.ACC_PUBLIC, new MethodBodyWriter(numArgs, numLocals));

            if (ioException != null || argException != null) {
                return;
            }

        }

        for (int i = 0; i < staticCount; i++) {
            clss.withField("static" + i, ConstantDescs.CD_short, ClassFile.ACC_STATIC | ClassFile.ACC_PRIVATE);
        }

        // Zero-initialize static variables
        if (staticCount > 0) {
            clss.withMethodBody("<clinit>", MethodTypeDesc.of(ConstantDescs.CD_void),
                ClassFile.ACC_STATIC, cob -> {
                    for (int i = 0; i < staticCount; i++) {
                        cob.iconst_0();
                        cob.putstatic(ClassDesc.of(className), "static" + i, ConstantDescs.CD_short);
                    }
                    cob.return_();
                });
        }

    }


    /**
     * Provides a handler to supply the code of a method body
     */
    private class MethodBodyWriter implements Consumer<CodeBuilder> {

        
	    // The number of arguments and local variables of the current method
        private int argCount, localCount;

        // The CodeBuilder for the current method
        private CodeBuilder code;

        // A table of labels found in the current function, and the corresponding destination that they
        // point to in the Java bytecode
        private HashMap<String, Label> labels = new HashMap<>();


        /**
         * Constructs a new MethodBodyWriter
         * @param nArgs the number of arguments of the method
         * @param nLocals the number of local variables in the method
         */
        public MethodBodyWriter(int nArgs, int nLocals) {
            argCount = nArgs;
            localCount = nLocals + 2; // Reserve space for pointer 0 and pointer 1
        }


        @Override
        public void accept(CodeBuilder code) {
            this.code = code;

            // Insert a call to Thread.sleep() at the beginning of each function; this allows keyboard
            // input to get through despite any recursion
            code.lconst_0();
            code.invokestatic(ClassDesc.of("java.lang.Thread"), "sleep", 
                MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_long));

            // Zero-initialize local variables
            for (int i = 0; i < localCount; i++) {
                code.iconst_0();
                code.istore(code.allocateLocal(TypeKind.SHORT));
            }

            try {
                Command prevCommand = null;

                while (parser.moreLines()) {
                    parser.advance();

                    switch (parser.getType()) {
                        case MATH:
                        writeArithmetic();
                        break;

                        case PUSH:
                        writePush();
                        break;

                        case POP:
                        writePop();
                        break;

                        case LABEL:
                        writeLabel();
                        break;

                        case GOTO:
                        writeGoto();
                        break;

                        case IF:
                        writeIf();
                        break;

                        case FUNC:
                        if (prevCommand != Command.RETURN) {
                            code.ireturn();
                        }
                        return;

                        case RETURN:
		                // In the Hack VM language, all functions return a value
                        code.ireturn();
                        break;

                        case CALL:
                        writeCall();
                        break;
                    }
                }

            } catch (IOException e) {
                ioException = new IOException("Failed to read file: " + inputFile, e);
                return;
            } catch (IllegalArgumentException e) {
                argException = e;
                return;
            }

        }


        /**
         * Writes an arithmetic instruction
         */
        public void writeArithmetic() {

            Label label1, label2;
            switch (parser.getArg1()) {
            case "add":
                code.iadd();
                // The result must be truncated to a short value to match Hack computer specs
                code.i2s();
                break;
                
            case "sub":
                code.isub();
                code.i2s();
                break;
                
            case "neg":
                code.ineg();
                code.i2s();
                break;			
                
            // The JVM doesn't have commands that correspond to Hack VM logical commands
            case "eq":
                label1 = code.newLabel();
                label2 = code.newLabel();
                code.if_icmpeq(label1);
                code.iconst_0();
                code.goto_(label2);
                code.labelBinding(label1);
                code.iconst_m1();
                code.labelBinding(label2);
                break;
                
            case "gt":
                label1 = code.newLabel();
                label2 = code.newLabel();
                code.if_icmpgt(label1);
                code.iconst_0();
                code.goto_(label2);
                code.labelBinding(label1);
                code.iconst_m1();
                code.labelBinding(label2);
                break;
                
            case "lt":
                label1 = code.newLabel();
                label2 = code.newLabel();
                code.if_icmplt(label1);
                code.iconst_0();
                code.goto_(label2);
                code.labelBinding(label1);
                code.iconst_m1();
                code.labelBinding(label2);
                break;
                
            case "and":
                code.iand();
                break;
                
            case "or":
                code.ior();
                break;
                
            case "not":
                // The JVM has no command for bitwise NOT
                code.iconst_1();
                code.iadd();
                code.ineg();
                code.i2s();
                break;
                
            }
        }
        

        /**
         * Writes a label
         */
        public void writeLabel() {
            String label = parser.getArg1();
            if (!labels.containsKey(label)) {
                labels.put(label, code.newBoundLabel());
            }
            else {
                code.labelBinding(labels.get(label));
            }
            // Insert a call to Thread.sleep() after each label; this allows keyboard input
            // to get through during input loops
            code.lconst_0();
            code.invokestatic(ClassDesc.of("java.lang.Thread"), "sleep", 
                MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_long));
        }
        

        /**
         * Writes a goto instruction
         */
        public void writeGoto() {
            String label = parser.getArg1();
            if (!labels.containsKey(label)) {
                // Until we find the label, link to an instruction stored in the labels table
                labels.put(label, code.newLabel());
            }
            code.goto_(labels.get(label));
        }
        

        /**
         * Writes an if-goto instruction
         */
        public void writeIf() {
            String label = parser.getArg1();
            if (!labels.containsKey(label)) {
                // Until we find the label, link to an instruction stored in the labels table
                labels.put(label, code.newLabel());
            }
            code.ifne(labels.get(label));
        }


        /**
         * Writes a push instruction
         */
        public void writePush() {

            String segment = parser.getArg1();
            int index = parser.getArg2();

            switch (segment) {
            
            case "constant":
                code.loadConstant(index);
                break;
                
            case "local":
                // In Java bytecode, arguments and local variables are accessed with the same instruction
                // The first two local variables of the method are reserved for pointer 0 and pointer 1
                code.iload(index + argCount + 2);
                break;
                
            case "argument":
                code.iload(index);
                break;
                
            case "this":
                // load pointer 0
                code.iload(argCount);
                code.loadConstant(index);
                code.iadd();
                code.invokestatic(ClassDesc.of("HackComputer"), "peek", MethodTypeDesc.of(
                    ConstantDescs.CD_short, ConstantDescs.CD_int));
                break;
                
            case "that":
                // load pointer 1
                code.iload(argCount + 1);
                code.loadConstant(index);
                code.iadd();
                code.invokestatic(ClassDesc.of("HackComputer"), "peek", MethodTypeDesc.of(
                    ConstantDescs.CD_short, ConstantDescs.CD_int));
                break;
                
            case "static":
                code.getstatic(ClassDesc.of(className), "static" + index, ConstantDescs.CD_short);
                staticCount = Math.max(staticCount, index + 1);
                break;
                
            case "pointer":
                if (index == 0) {
                    // pointer 0
                    code.iload(argCount);
                }
                else {
                    // pointer 1
                    code.iload(argCount + 1);
                }
                break;
                
            case "temp":
                code.loadConstant(index);
                code.invokestatic(ClassDesc.of("HackComputer"), "pushTemp", MethodTypeDesc.of(
                    ConstantDescs.CD_short, ConstantDescs.CD_int));
                break;
            
            }
        }
        

        /**
         * Writes a pop instruction
         */
        public void writePop() {

            String segment = parser.getArg1();
            int index = parser.getArg2();

			switch (segment) {
			
			case "local":
				// In Java bytecode, arguments and local variables are accessed with the same instruction
				// The first two local variables of the method are reserved for pointer 0 and pointer 1
				code.istore(index + argCount + 2);
				break;
				
			case "argument":
				code.istore(index);
				break;
				
			case "this":
				// pointer 0
				code.iload(argCount);
				code.loadConstant(index);
				code.iadd();
                code.invokestatic(ClassDesc.of("HackComputer"), "poke", MethodTypeDesc.of(
                    ConstantDescs.CD_void, ConstantDescs.CD_int, ConstantDescs.CD_int));
				break;
				
			case "that":
				// pointer 1
				code.iload(argCount + 1);
				code.loadConstant(index);
				code.iadd();
                code.invokestatic(ClassDesc.of("HackComputer"), "poke", MethodTypeDesc.of(
                    ConstantDescs.CD_void, ConstantDescs.CD_int, ConstantDescs.CD_int));
				break;
				
			case "static":
                code.putstatic(ClassDesc.of(className), "static" + index, ConstantDescs.CD_short);
				staticCount = Math.max(staticCount, index + 1);
				break;
				
			case "pointer":
				if (index == 0) {
					// pointer 0
					code.istore(argCount);
				}
				else {
					// pointer 1
					code.istore(argCount + 1);
				}
				break;
				
			case "temp":
				code.loadConstant(index);
                code.invokestatic(ClassDesc.of("HackComputer"), "popTemp", MethodTypeDesc.of(
                    ConstantDescs.CD_void, ConstantDescs.CD_int, ConstantDescs.CD_int));
				break;
				
			}
			
        }
        

        /**
         * Writes a function call
         */
        public void writeCall() {

            String function = parser.getArg1();
            int nArgs = parser.getArg2();
            
            // Replace these specific OS functions with JVM stack operations which are much faster
            if (function.equals("Math.multiply")) {
                code.imul();
                code.i2s();
                return;
            }
            else if (function.equals("Math.divide")) {
                code.idiv();
                code.i2s();
                return;
            }

            String names[] = function.split("\\.");
            
            if (names[1].equals("new")) {
                names[1] = "NEW";
            }
            
            code.invokestatic(ClassDesc.of(names[0]), names[1], MethodTypeDesc.of(
                ConstantDescs.CD_short, Collections.nCopies(nArgs, ConstantDescs.CD_short)));
            
        }

    }
	
}
