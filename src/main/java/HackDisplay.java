/**
 * JHack - https://github.com/Teledar/JHack
 * This file defines a display screen for the JHack emulator, a Java-based emulator of the
 * Nand to Tetris Hack computer.
 * Nand to Tetris - https://www.nand2tetris.org/
 */

import java.awt.*;
import javax.swing.*;

/**
 * Defines a panel to display the screen of the Hack emulator
 */
public class HackDisplay extends JPanel {

    // Add 2-pixel border; the Hack display is 512px x 256px
    public static final int PREFERRED_WIDTH = 515;
    public static final int PREFERRED_HEIGHT = 259;

    /**
     * Constructs a new HackDisplay
     */
    public HackDisplay() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // from nand2tetris ScreenComponent
        super.paintComponent(g);
        int x, y;
        for (short i = HackComputer.SCREEN; i < HackComputer.KBD; i++) {
            if (HackComputer.peek(i) != 0) {
                x = ((i - HackComputer.SCREEN) % 32) * 16 + 2; // add 2-pixel border
                y = (i - HackComputer.SCREEN) / 32 + 2;
                if (HackComputer.peek(i) == -1) // draw a full line
                    g.drawLine(x, y, x + 15, y);
                else {
                    short value = HackComputer.peek(i);
                    for (int j = 0; j < 16; j++) {
                        if ((value & 0x1) == 1)
                            // since there's no drawPixel, uses drawLine to draw one pixel
                            g.drawLine(x + j, y, x + j, y);

                        value = (short)(value >> 1);
                    }
                }
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
    }

}