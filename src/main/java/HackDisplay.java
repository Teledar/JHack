

import java.awt.*;
import javax.swing.*;

public class HackDisplay extends JPanel {

    // Add 2-pixel border
    public static final int PREFERRED_WIDTH = 515;
    public static final int PREFERRED_HEIGHT = 259;

    public HackDisplay() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        //from nand2tetris ScreenComponent
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