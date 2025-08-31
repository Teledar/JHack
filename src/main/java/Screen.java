/**
 * JHack - https://github.com/Teledar/JHack
 * This file implements the Nand to Tetris JackOS Screen class for the JHack emulator
 * Nand to Tetris - https://www.nand2tetris.org/
 */

/**
* A library of functions for displaying graphics on the screen.
* The Hack physical screen consists of 512 rows (indexed 0..511, top to bottom)
* of 256 pixels each (indexed 0..255, left to right). The top left pixel on 
* the screen is indexed (0,0).
*/
public class Screen {
	private static short color;

	/** 
	 * Initializes the Screen.
     * @return The return value of this method is ignored.
	 */
	public static short init() {
		color = -1;
		return 0;
	}

	/** 
	 * Erases the entire screen.
     * @return The return value of this method is ignored.
	 */
	public static short clearScreen() {
		for (int i = HackComputer.SCREEN; i < HackComputer.KBD; i++ ) {
			HackComputer.poke(0, i);
		}
		return 0;
	}

	/** 
	 * Sets the current color, to be used for all subsequent drawXXX commands.
	 * Black is represented by true, white by false.
     * @return The return value of this method is ignored.
	 */
	public static short setColor(short b) {
		color = b;
		return 0;
	}

	/** 
	 * Draws the (x,y) pixel, using the current color.
     * @return The return value of this method is ignored.
	 */
	public static short drawPixel(short x, short y) {

		if ((x < 0) | (x > 511) | (y < 0) | (y > 255)) {
			Sys.error((short) 7);
			return 0;
		}

		y = (short) ((x / 16) + (y * 32) + HackComputer.SCREEN);
		x = (short) java.lang.Math.pow(2, x & 0x0F);
		
		if (color == -1) {
			HackComputer.poke(x | HackComputer.peek(y), y);
		} else {
			HackComputer.poke(~x & HackComputer.peek(y), y);
		}
     
		return 0;
	}

	/** 
	 * Draws a line from pixel (x1,y1) to pixel (x2,y2), using the current color.
     * @return The return value of this method is ignored.
	 */
	public static short drawLine(short x1, short y1, short x2, short y2) {
		int diff, ix, iy, dx, dy;

		if ((x1 < 0) | (x1 > 511) | (y1 < 0) | (y1 > 255) | (x1 < 0) | (x1 > 511) | (y1 < 0) | (y1 > 255)) {
			Sys.error((short) 8);
			return 0;
		}

		if (y1 == y2) {
			//dot
			if (x1 == x2) {
				drawPixel(x1, y1);
				return 0;
			}

			//horizontal line

			if (x1 > x2) {
				diff = x1;
				x1 = x2;
				x2 = (short) diff;
			}

			dy = y1;
			
			y1 = (short) -java.lang.Math.pow(2, x1 & 15);
			y2 = (short) (java.lang.Math.pow(2, x2 & 15) - 1);

			x1 = (short) ((x1 / 16) + (dy * 32) + HackComputer.SCREEN);
			x2 = (short) ((x2 / 16) + (dy * 32) + HackComputer.SCREEN);
         
			if (x1 == x2) {
				if (color == -1) {
					HackComputer.poke(y1 & y2 | HackComputer.peek(x1), x1);
				} else {
					HackComputer.poke(~(y1 & y2) & HackComputer.peek(x1), x1);
				}
				return 0;
			}

			if (color == -1) {
				HackComputer.poke(HackComputer.peek(x1) | y1, x1);
				x1++;
				while (x1 < x2) {
					HackComputer.poke(-1, x1);
					x1++;
				}
				HackComputer.poke(HackComputer.peek(x1) | y2, x1);
			} else {
				y1 = (short) ~y1;
				y2 = (short) ~y2;
				HackComputer.poke(HackComputer.peek(x1) & y1, x1);
	            x1++;
             	while (x1 < x2) {
             		HackComputer.poke(0, x1);
             		x1++;
             	}
             	HackComputer.poke(HackComputer.peek(x1) & y2, x1);
			}
     
			return 0;


		} else if (x1 == x2) {
			//vertical line

			if (y1 > y2) {
				x1 = y1;
				y1 = y2;
				y2 = x1;
			}
			x1 = (short) ((x2 / 16) + (y1 * 32) + HackComputer.SCREEN);
			x2 = (short) java.lang.Math.pow(2, x2 & 15);
			y2++;
			if (color == -1) {
				while (y1 < y2) {
					HackComputer.poke(HackComputer.peek(x1) | x2, x1);
					y1++;
					x1 += 32;
				}
			} else {
				x2 = (short) ~x2;
				while (y1 < y2) {
					HackComputer.poke(HackComputer.peek(x1) & x2, x1);
                 	y1++;
                 	x1 += 32;
				}
			}

		} else {
			//diagonal line
			diff = 0;

			if (x2 > x1) {
				ix = 1;
			} else {
				ix = -1;
			}

			if (y2 > y1) {
				iy = 1;
			} else {
				iy = -1;
			}

			dx = Math.abs((short) (x2 - x1));
			dy = Math.abs((short) (y2 - y1));

			while (!((x1 == x2) && (y1 == y2))) {
				drawPixel(x1, y1);
				if (diff < 0) {
					x1 += ix;
					diff += dy;
				} else {
					y1 += iy;
					diff -= dx;
				}
			}
		}
		return 0;
	}

	/** 
	 * Draws a filled rectangle whose top left corner is (x1, y1)
	 * and bottom right corner is (x2,y2), using the current color.
     * @return The return value of this method is ignored.
	 */
 	public static short drawRectangle(short x1, short y1, short x2, short y2) {

 		if ((x1 < 0) | (x1 > 511) | (y1 < 0) | (y1 > 255) | (x1 < 0) | (x1 > 511) | (y1 < 0) | (y1 > 255)) {
 			Sys.error((short) 9);
 			return 0;
 		}

 		y2++;
 		while (y1 < y2) {
 			drawLine(x1, y1, x2, y1);
 			y1++;
 		}
 		return 0;
 	}

 	/** 
	 * Draws a filled circle of radius r<=181 around (x,y), using the current color.
     * @return The return value of this method is ignored.
	 */
 	public static short drawCircle(short x, short y, short r) {
 		int dy, dx, rr;
     
 		if (r < 1) {
 			Sys.error((short) 13);
 			return 0;
 		}

 		if ((x < 0) | (x > 511) | (y < 0) | (y > 255)) {
 			Sys.error((short) 12);
 			return 0;
 		}

 		dy = -r;
 		rr = r * r;
     	r++;
     	while (dy < r) {
     		dx = (short) java.lang.Math.sqrt(rr - (dy * dy));
         	drawLine((short) (x - dx & 511), (short) (y + dy & 255), (short) (x + dx & 511), (short) (y + dy & 255));
        	dy++;
     	}
     	return 0;
 	}
}
