import sun.font.TrueTypeFont;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Date;
import java.util.Random;

/**
 */
public class Gol implements Runnable {

    private static final int BOARD_SIZE_X = 500;
    private static final int BOARD_SIZE_Y = 500;
    private static final int GAP_SIZE_X = 10;
    private static final int GAP_SIZE_Y = 10;
    private static long PROTECTED_CELL_TRESH = 500;

    private long lastProtectedCell = 0;
    private int deleteMode = 0;

    private Random random = new Random();

    private Color[] colors = new Color[] {
        Color.orange,
        Color.red,
        Color.black,
        Color.green
    };

    private int delay = 1;
    private final DrawPanel drawPanel;

    private int gapSizeX;
    private int gapSizeY;

    private boolean paused = false;

    private final int boardSizeX;
    private final int boardSizeY;

    private final int[][] board;
    private final int[][] neighbours;

    private final Thread gameThread;

    public Gol() {
        this(BOARD_SIZE_X, BOARD_SIZE_Y,
                GAP_SIZE_X, GAP_SIZE_Y);
    }

    public Gol(int boardSizeX, int boardSizeY,
               int gapSizeX, int gapSizeY) {
        this.drawPanel = new DrawPanel();
        this.drawPanel.addMouseListener(new MouseHandler());
        this.drawPanel.addMouseMotionListener(new MouseHandler());
        this.drawPanel.addMouseWheelListener(new MouseWheelHandler());

        this.gapSizeX = gapSizeX;
        this.gapSizeY = gapSizeY;
        this.boardSizeX = boardSizeX;
        this.boardSizeY = boardSizeY;

        this.board = new int[boardSizeX][boardSizeY];
        this.neighbours = new int[boardSizeX][boardSizeY];

        this.clearNeighbours();
        this.clearBoard();

        this.board[80][50] = 1;
        this.board[80][51] = 1;
        this.board[80][52] = 1;

        gameThread = new Thread(this);
    }

    class MouseWheelHandler extends MouseInputAdapter {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int oldGapX = gapSizeX;
            int oldGapY = gapSizeY;

            int amount;
            if (e.getWheelRotation() < 0) {
                amount = 1;
            } else {
                amount = -1;
            }

            gapSizeX = gapSizeX + amount;
            gapSizeY = gapSizeY + amount;

            if (gapSizeX < 0) {
                gapSizeX = 1;
            }

            if (gapSizeY < 0) {
                gapSizeY = 1;
            }

            drawPanel.repaint();
        }
    }

    class MouseHandler extends MouseInputAdapter {
        private void addPoint(int x, int y) {
            int i = x / gapSizeX;
            int j = y / gapSizeY;
            int b = board[i][j];

            if (b == 0) {
                if ( deleteMode >= 0 ) {
                    board[i][j] = 1;
                    deleteMode = 1;
                }
            } else {
                if (deleteMode <= 0) {
                    board[i][j] = 0;
                    deleteMode = -1;
                }
            }
            drawPanel.repaint();
            lastProtectedCell = new Date().getTime();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            this.addPoint(e.getX(), e.getY());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            deleteMode = 0;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            this.addPoint(e.getX(), e.getY());
        }
    }

    public void run() {
        while (gameThread != null) {
            while (paused) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace(); // FIXME
                }
            }
            calculateNeighbours();
            calculateBoard();
            drawPanel.repaint();
            clearNeighbours();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace(); // FIXME
            }
        }
    }

    public void start() {
        this.gameThread.start();
    }

    public void resume() {
        this.paused = false;
    }

    public void pause() {
        this.paused = true;
    }

    public void clearNeighbours() {
        for (int i = 0; i < boardSizeX; i++) {
            for (int j = 0; j < boardSizeY; j++) {
                neighbours[i][j] = 0;
            }
        }
    }

    public void clearBoard() {
        for (int i = 0; i < boardSizeX; i++) {
            for (int j = 0; j < boardSizeY; j++) {
                board[i][j] = 0;
            }
        }
    }

    public void display() throws InterruptedException {
        final JFrame frame;

        final JButton pauseButton = new JButton("Pause");
        final JButton resumeButton = new JButton("Resume");
        final JSlider speedSlider = new JSlider(1, 1000, 500);

        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                delay = 10000 / speedSlider.getValue();
            }
        });

        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pause();
            }
        });

        resumeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resume();
            }
        });

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(pauseButton);
        buttonPanel.add(resumeButton);
        buttonPanel.add(speedSlider);

        frame = new JFrame("Gol");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(drawPanel);

        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        frame.setSize(600, 600);
        frame.setVisible(true);
    }

    public void calculateNeighbours() {
        for (int i = 0; i < boardSizeX; i++) {
            for (int j = 0; j < boardSizeY; j++) {
                // increment neighbour counts
                if (board[i][j] == 1) {
                    if(i > 0) {
                        neighbours[i-1][j] += 1;
                        if (j > 0) {
                            neighbours[i-1][j-1] += 1;
                        }
                        if (j < boardSizeY - 1) {
                            neighbours[i-1][j+1] += 1;
                        }
                    }
                    if(i < boardSizeX - 1) {
                        neighbours[i+1][j] += 1;
                        if (j > 0) {
                            neighbours[i+1][j-1] += 1;
                        }
                        if (j < boardSizeY - 1) {
                            neighbours[i+1][j+1] += 1;
                        }
                    }
                    if ( j > 0 ) {
                        neighbours[i][j-1] += 1;
                    }

                    if (j < boardSizeY - 1) {
                        neighbours[i][j+1] += 1;
                    }
                }
            }
        }
    }

    public void calculateBoard() {
        int nCount;
        for (int i = 0; i < boardSizeX; i++) {
            for (int j = 0; j < boardSizeY; j++) {
                nCount = neighbours[i][j];
                if (board[i][j] == 1) {
                    System.out.println(nCount);
                    if (nCount < 2 || nCount > 3) {
                        board[i][j] = 0;
                    }
                } else {
                    if (neighbours[i][j] == 3) {
                        board[i][j] = 1;
                    }
                }
            }
        }
    }

    class DrawPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            for (int i = 0; i < boardSizeX; i++) {
                for (int j = 0; j < boardSizeY; j++) {
                    float rc = random.nextFloat();
                    float gc = random.nextFloat();
                    float bc = random.nextFloat();

                    if (board[i][j] == 1) {
                        g.setColor(new Color(rc, gc, bc));
                    } else {
                        g.setColor(Color.white);
                    }

                    g.fillRect(
                            i * gapSizeX,
                            j * gapSizeY,
                            gapSizeX,
                            gapSizeY);
                }
            }
            g.setColor(Color.black);
            for (int i = 0; i < boardSizeY; i++) {
                g.drawLine(
                        0,
                        i * gapSizeY,
                        boardSizeX * gapSizeX,
                        i * gapSizeY);
            }
            for (int j = 0; j < boardSizeX; j++) {
                g.drawLine(
                        j * gapSizeX,
                        0,
                        j * gapSizeX,
                        boardSizeY * gapSizeY);
            }
        }
    }

    public static void main(String[] args) {
        final Gol gol = new Gol();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    gol.display();
                    gol.start();
                } catch (InterruptedException e) {
                    e.printStackTrace(); // FIXME
                }
            }
        });
    }

}
