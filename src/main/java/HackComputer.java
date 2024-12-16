import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class HackComputer extends JPanel {

    public short ram[]; //Addresses from 0 to 24576
    
    //Pointer to the beginning of the screen memory map
    final int SCREEN = 16384;

    //Pointer to the keyboard memory map
    final int KBD = 24576;

    HackComputer(short ram_array[]) {
        ram = ram_array;
        setOpaque(true);
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        //from nand2tetris ScreenComponent
        super.paintComponent(g);
        int x, y;
        for (int i = SCREEN; i < KBD; i++) {
            if (ram[i] != 0) {
                x = ((i - SCREEN) % 32) * 16;
                y = (i - SCREEN) / 32;
                if (ram[i] == -1) // draw a full line
                    g.drawLine(x, y, x + 15, y);
                else {
                    short value = ram[i];
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
        return new Dimension(512, 256);
    }

}