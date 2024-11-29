import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class JHack {

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                startGUI();
            }
        });

    }

    public static void startGUI() {

        JFrame frame = new JFrame("JHack");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        HackComputer computer = new HackComputer();
        //emptyLabel.setPreferredSize(new Dimension(175, 100));
        frame.getContentPane().add(computer);
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
        computer.run();
    }

}