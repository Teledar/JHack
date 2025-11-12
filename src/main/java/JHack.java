/**
 * JHack - https://github.com/Teledar/JHack
 * This file is the entry point for the JHack emulator, a Java-based emulator of the
 * Nand to Tetris Hack computer.
 * Nand to Tetris - https://www.nand2tetris.org/
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.concurrent.ExecutionException;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * Defines a JFrame to display the screen of the Hack emulator
 */
public class JHack extends JFrame implements KeyListener {

	// The screen component
    private HackDisplay display;

    // Used to periodically refresh the screen
    private Timer timer;

    // Whether the SHIFT key is currently pressed
    private boolean shift;

    // Whether CAPS LOCK is toggled on or off; set to true if you want CAPS LOCK on by default
    private boolean caps_lock;

    /**
     * The entry point of the JHack emulator program
     * @param args currenly unused
     */
    public static void main(java.lang.String[] args) {

        // Set the scale to something sensible
        if (System.getProperty("sun.java2d.uiScale") == null) {
            System.setProperty("sun.java2d.uiScale", "2.0");
        }

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                startGUI();
            }
        });
    }


    /**
     * Creates a new JFrame to display the screen of the Hack emulator,
     * and starts the Jack program
     */
    public static void startGUI() {
        JHack frame = new JHack("JHack");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addComponents();
        frame.pack();
        frame.setVisible(true);
        frame.refreshScreen();
    }

    
    /**
     * Creates a new JFrame to display the screen of the Hack emulator
     * @param name the window title
     */
    public JHack(java.lang.String name) {
        super(name);
    }

    
    /**
     * Adds a HackDisply to the JFrame and sets this class as the KeyEvent handler
     */
    public void addComponents() {
        display = new HackDisplay();
        // This class will now handle keyboard input to the display
        display.addKeyListener(this);
        getContentPane().add(display);
    }


    /**
     * Sets a timer to periodically refresh the screen
     */
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
        // Tell the Jack program to start
        Worker app = new Worker();
        app.execute();
    }


    /** 
     * Sets the Hack keyboard memory map to contain the key currently pressed, and updates
     * the status of the SHIFT and CAPS LOCK keys
     */
    public void keyPressed(KeyEvent e) {
        short k = (short) e.getKeyCode();
        if (k == KeyEvent.VK_SHIFT) {
            shift = true;
        } else {
            HackComputer.poke(convertKey(k), HackComputer.KBD);
        }
    }


    /**
     * Clears the Hack keyboard memory map and updates the status of SHIFT and CAPS LOCK
     */
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shift = false;
        } 
        else if (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK) {
            caps_lock = !caps_lock;
        }
        HackComputer.poke((short) 0, HackComputer.KBD);
    }


    /**
     * Not implemented; keyPressed() and keyReleased() handle all input
     */
    public void keyTyped(KeyEvent e) {
    }


    /**
     * Sets up the Jack program to run on a SwingWorker thread
     */
    private class Worker extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() {
            Sys.init();
            return null;
        }
        
        @Override
        protected void done() {
        	try {
				get();
			} catch (InterruptedException e) {
				throw new RuntimeException("Unexpected interrupt");
			} catch (ExecutionException e) {
				// Print any exception that happened while the task executed 
                if (e.getCause().getStackTrace()[0].getMethodName().equals("halt")) {
                    setTitle(getTitle() + " - HALTED");
                } else {
                    setTitle(getTitle() + " - ERROR");
                    e.getCause().printStackTrace();
                }
			}
        }
    }



    /**
     * Converts a KeyEvent code to the corresponding JackOS character.
     * Modifies the character if SHIFT is pressed or CAPS LOCK is activated.
     * @param key the KeyEvent code to convert
     * @return the appropriate JackOS character
     */
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
