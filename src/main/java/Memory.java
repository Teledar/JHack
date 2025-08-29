/**
 * This library provides two services: direct access to the computer's main
 * memory (RAM), and allocation and recycling of memory blocks. The Hack RAM
 * consists of 32,768 words, each holding a 16-bit binary number.
 */ 
public class Memory {

	//This implementation of Memory uses a one-word overhead per block instead
	//of the two-word overhead required by the recommended Nand2Tetris implementation.
	//Each block is prefaced with a word that gives the length of the block. The length
	//allows the OS to compute where the next block of memory begins. If the block is
	//not free, the length indicator is negated.
	
	//The address to start looking for free blocks
    private static int start;

    /** Initializes the class. */
    public static short init() {
    	
        start = HackComputer.HEAP_START;
        
        //Create one block that contains all the space between the beginning of the heap
        //and the screen memory map
        HackComputer.poke(HackComputer.SCREEN - start - 2, start);
        
        //Assuming that the keyboard memory map is immediately after the screen memory map,
        //mark the screen memory map and keyboard memory map as already allocated
        HackComputer.poke(-8193, HackComputer.SCREEN - 1);

        //Assuming that the keyboard memory map is immediately after the screen memory map,
        //create another block after the keyboard memory map if there is any available RAM there
        if (HackComputer.RAM_END > HackComputer.KBD) {
            HackComputer.poke(HackComputer.RAM_END - HackComputer.KBD - 1,
            		HackComputer.KBD + 1);
        }
        return 0;
        
    }

    /** Returns the RAM value at the given address. */
    public static short peek(short address) {
        return HackComputer.peek(address);
    }

    /** Sets the RAM value at the given address to the given value. */
    public static short poke(short address, short value) {
        HackComputer.poke(value, address);
        return 0;
    }

    /** Finds an available RAM block of the given size and returns
     *  a reference to its base address. */
    public static short alloc(short size) {

    	int current = -1;
    	int next = current + Math.abs(HackComputer.peek(start)) + 1;
    	
        int best_addr = -1, best_size = 32767;

        if (size < 1) {
            Sys.error((short) 5);
            return -1;
        }

        next = start;
        
        while (next <= HackComputer.RAM_END) {
            if (HackComputer.peek(next) >= size && HackComputer.peek(next) < best_size) {
                best_addr = next;
                best_size = HackComputer.peek(next);
            }
            current = next;
            next = current + Math.abs(HackComputer.peek(current)) + 1;
        }

        if (best_addr < 0) {
            Sys.error((short) 6);
            return -1;
        }

        if (best_size - size > Math.min(size, (short) 4)) {
            //split the block
        	HackComputer.poke(size, best_addr);
        	HackComputer.poke(best_size - size - 1, best_addr + size + 1);
        }

        if (best_addr == start) {
            start = HackComputer.peek(best_addr) + best_addr + 1;
        }

        HackComputer.poke(-HackComputer.peek(best_addr), best_addr);
        
        return (short) (best_addr + 1);
    }

    /** De-allocates the given object (cast as an array) by making
     *  it available for future allocations. */
    public static short deAlloc(short o) {
    	o--;
        HackComputer.poke(-HackComputer.peek(o), o);
        if (o < start) {
        	start = o;
        }
        defrag(o);
        return 0;
    }
    
    //Defrag the memory around the provided address, which has just been freed
    private static void defrag(short free) {
    	int current = start;
    	int next = current + Math.abs(HackComputer.peek(current)) + 1;
    	int parent = -1, child = -1;
    	
    	//Find the "parent" block
    	while (next < free) {
    		current = next;
    		next = current + Math.abs(HackComputer.peek(current)) + 1;
    	}
    	
    	if (next == free && HackComputer.peek(current) >= 0) {
    		parent = current;
    	}
    	
    	//Find the "child" block
    	current = free;
		next = current + Math.abs(HackComputer.peek(current)) + 1;
		if (next < HackComputer.RAM_END && HackComputer.peek(next) >= 0) {
			child = next;
		}
		
		//Combine the newly-freed block with the "parent" if the parent is free
		if (parent >= 0) {
			HackComputer.poke(HackComputer.peek(free) + HackComputer.peek(parent) + 1, parent);
			current = parent;
		}
		
		//Combine the "child" block with the freed block or the "parent" if the child is free
		if (child >= 0) {
			HackComputer.poke(HackComputer.peek(child) + HackComputer.peek(current) + 1, current);
		}
    }
}
