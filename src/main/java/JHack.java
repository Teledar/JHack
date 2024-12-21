import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class JHack extends JFrame implements KeyListener {

    // The screen component
    private HackDisplay display;

    // Used to periodically refresh the screen
    private Timer timer;

    // Whether the SHIFT key is currently pressed
    private boolean shift;

    // Whether CAPS LOCK is toggled on or off; set to true if you want CAPS LOCK on by default
    private boolean caps_lock;

    
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                startGUI();
            }
        });
    }


    public static void startGUI() {
        JHack frame = new JHack("JHack - " + HackApplication.getName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addComponents();
        frame.pack();
        frame.setVisible(true);
        frame.refreshScreen();
    }

    
    public JHack(String name) {
        super(name);
    }

    
    public void addComponents() {
        display = new HackDisplay();
        // This class will now handle keyboard input to the display
        display.addKeyListener(this);
        getContentPane().add(display);
    }


    // Set the timer to periodically refresh the screen
    public void refreshScreen() {
        timer = new Timer(0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                display.repaint();
                display.requestFocusInWindow();
            }
        });
        timer.setRepeats(true);
        // Aprox. 60 FPS
        timer.setDelay(17);
        timer.start();
        // Tell the HackApplication to start
        Worker app = new Worker();
        app.execute();
    }


    // Set the keyboard memory map to contain the key currently pressed
    public void keyPressed(KeyEvent e) {
        short k = (short) e.getKeyCode();
        if (k == KeyEvent.VK_SHIFT) {
            shift = true;
        } else {
            HackComputer.poke(convertKey(k), HackComputer.KBD);
        }
    }


    // Clear the keyboard memory map
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shift = false;
        } 
        else if (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK) {
            caps_lock = !caps_lock;
        }
        HackComputer.poke(0, HackComputer.KBD);
    }


    // This isn't implemented; keyPressed and keyReleased take care of all input
    public void keyTyped(KeyEvent e) {
    }


    // Set up the HackApplication to run on a SwingWorker thread
    private class Worker extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() {
            HackApplication.run();
            return null;
        }
    }


    // Convert the KeyEvent code to JackOS standard
    private short convertKey(int key) {

        // Special keys not affected by SHIFT or CAPS LOCK
        switch (key) {
            case KeyEvent.VK_ENTER:
                return 128;
            case KeyEvent.VK_BACK_SPACE:
                return 129;
            case KeyEvent.VK_LEFT:
                return 130;
            case KeyEvent.VK_UP:
                return 131;
            case KeyEvent.VK_RIGHT:
                return 132;
            case KeyEvent.VK_DOWN:
                return 133;
            case KeyEvent.VK_HOME:
                return 134;
            case KeyEvent.VK_END:
                return 135;
            case KeyEvent.VK_PAGE_UP:
                return 136;
            case KeyEvent.VK_PAGE_DOWN:
                return 137;
            case KeyEvent.VK_INSERT:
                return 138;
            case KeyEvent.VK_DELETE:
                return 139;
            case KeyEvent.VK_ESCAPE:
                return 140;
            case KeyEvent.VK_F1:
                return 141;
            case KeyEvent.VK_F2:
                return 142;
            case KeyEvent.VK_F3:
                return 143;
            case KeyEvent.VK_F4:
                return 144;
            case KeyEvent.VK_F5:
                return 145;
            case KeyEvent.VK_F6:
                return 146;
            case KeyEvent.VK_F7:
                return 147;
            case KeyEvent.VK_F8:
                return 148;
            case KeyEvent.VK_F9:
                return 149;
            case KeyEvent.VK_F10:
                return 150;
            case KeyEvent.VK_F11:
                return 151;
            case KeyEvent.VK_F12:
                return 152;
        }

        // Keys that change if SHIFT is pressed
        if (shift) {
            switch (key) {
            case KeyEvent.VK_1:
                return 33;
            case KeyEvent.VK_QUOTE:
                return 34;
            case KeyEvent.VK_3:
                return 35;
            case KeyEvent.VK_4:
                return 36;
            case KeyEvent.VK_5:
                return 37;
            case KeyEvent.VK_7:
                return 38;
            case KeyEvent.VK_9:
                return 40;
            case KeyEvent.VK_0:
                return 41;
            case KeyEvent.VK_8:
                return 42;
            case KeyEvent.VK_EQUALS:
                return 43;
            case KeyEvent.VK_SEMICOLON:
                return 58;
            case KeyEvent.VK_COMMA:
                return 60;
            case KeyEvent.VK_PERIOD:
                return 62;
            case KeyEvent.VK_SLASH:
                return 63;
            case KeyEvent.VK_2:
                return 64;
            case KeyEvent.VK_6:
                return 94;
            case KeyEvent.VK_MINUS:
                return 95;
            case KeyEvent.VK_OPEN_BRACKET:
                return 123;
            case KeyEvent.VK_BACK_SLASH:
                return 124;
            case KeyEvent.VK_CLOSE_BRACKET:
                return 125;
            case KeyEvent.VK_BACK_QUOTE:
                return 126;
            }
            // If SHIFT and CAPS LOCK are both on, convert letter keys to lowercase
            if (caps_lock && key >= KeyEvent.VK_A && key <= KeyEvent.VK_Z) {
                return (short) (key + 32);
            }
        } 
        else {
            switch (key) {
            case KeyEvent.VK_QUOTE:
                return 39;
            case KeyEvent.VK_BACK_QUOTE:
                return 96;
            }
            // If neither SHIFT nor CAPS LOCK are on, convert letter keys to lowercase
            if (!caps_lock && key >= KeyEvent.VK_A && key <= KeyEvent.VK_Z) {
                return (short) (key + 32);
            }
        }

        // The remaining keyboard keys are already in JackOS standard
        if (key >= KeyEvent.VK_SPACE && key <= KeyEvent.VK_CLOSE_BRACKET) {
            return (short) key;
        }

        // Ignore any other keys
        return 0;
        
    }

    
}
