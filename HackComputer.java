import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class HackComputer extends JPanel {

    Timer timer;

    public Short ram[]; //Addresses from 2048 to 24576
    
    //Pointer to the beginning of the screen memory map
    final int SCREEN = 16384 - 2048;

    //Pointer to the keyboard memory map
    final int KBD = 24576 - 2048;

    HackComputer() {
        initram();
        setOpaque(true);
        setBackground(Color.WHITE);
        //setForeground(Color.BLACK);
        refreshScreen();
    }

    public void run() {
        HackApplication.run(ram);
        //timer.stop();
    }

    private void initram() {
        ram = new Short[22529];
        for (int i = 0; i < 22529; i++) {
            ram[i] = 0;
        }
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
                if (ram[i] == -32768) // draw a full line
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

    private void refreshScreen() {
        timer = new Timer(0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        timer.setRepeats(true);
        // Aprox. 60 FPS
        timer.setDelay(17);
        timer.start();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(512, 256);
    }

}