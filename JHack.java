import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class JHack extends JFrame implements KeyListener {

    private static short ram[];

    private HackComputer computer;
    private Timer timer;
    private boolean shift;
    private boolean caps_lock;

    public static void main(String[] args) {
        initram();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                startGUI();
            }
        });
    }

    private static void initram() {
        ram = new short[24577];
        for (int i = 0; i < 24577; i++) {
            ram[i] = 0;
        }
    }

    public static void startGUI() {

        JHack frame = new JHack("JHack - " + HackApplication.name);
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
        computer = new HackComputer(ram);
        computer.addKeyListener(this);
        getContentPane().add(computer);
    }

    public void refreshScreen() {
        timer = new Timer(0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                computer.repaint();
                computer.requestFocusInWindow();
            }
        });
        timer.setRepeats(true);
        // Aprox. 60 FPS
        timer.setDelay(17);
        timer.start();
        Worker app = new Worker();
        app.execute();
    }

    public void keyPressed(KeyEvent e) {
        short k = (short) e.getKeyCode();
        if (k == KeyEvent.VK_SHIFT) {
            shift = true;
        } else {
            ram [24576] = convertKey(k);
        }
    }
     
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shift = false;
        } 
        else if (e.getKeyCode() == KeyEvent.VK_CAPS_LOCK) {
            caps_lock = !caps_lock;
        }
        ram[24576] = 0;
    }
    
    public void keyTyped(KeyEvent e) {
    }

    private class Worker extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() {
            HackApplication.run(ram);
            return null;
        }
    }

    private short convertKey(int key) {
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
            if (caps_lock && key >= KeyEvent.VK_A && key <= KeyEvent.VK_Z) {
                return (short) (key + 32);
            }
        } else {
            switch (key) {
            case KeyEvent.VK_QUOTE:
                return 39;
            case KeyEvent.VK_BACK_QUOTE:
                return 96;
            }
            if (!caps_lock && key >= KeyEvent.VK_A && key <= KeyEvent.VK_Z) {
                return (short) (key + 32);
            }
        }
        if (key >= KeyEvent.VK_SPACE && key <= KeyEvent.VK_CLOSE_BRACKET) {
            return (short) key;
        }
        return 0;
    }
}