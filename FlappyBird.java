import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {

    int boardWidth = 360;
    int boardHeight = 640;

    Image backgroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;

    enum GameState { MENU, PLAYING, GAME_OVER }
    GameState gameState = GameState.MENU;

    int birdX = boardWidth / 8;
    int birdY = boardHeight / 2;
    int birdWidth = 34;
    int birdHeight = 24;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    class Pipe {
        int x;
        int y;
        int width = 64;
        int height = 512;
        Image img;
        boolean passed = false;

        Pipe(int x, int y, Image img) {
            this.x = x;
            this.y = y;
            this.img = img;
        }
    }

    Bird bird;
    int velocityX = -4;
    int velocityY = 0;
    int gravity = 1;

    int pipeHeight = 512;
    int openingSpace;

    int level = 1;
    double score = 0;

    ArrayList<Pipe> pipes = new ArrayList<>();
    Random random = new Random();

    Timer gameLoop;
    Timer pipeTimer;

    JButton startButton, restartButton;
    JPanel levelPanel;

    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);
        setLayout(null);

        backgroundImg = new ImageIcon(getClass().getResource("./flappybirdbg.png")).getImage();
        birdImg = new ImageIcon(getClass().getResource("./flappybird.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("./toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("./bottompipe.png")).getImage();

        bird = new Bird(birdImg);

        gameLoop = new Timer(1000 / 60, this);
        pipeTimer = new Timer(1500, e -> placePipes());

        // Start button
        startButton = new JButton("Start Game");
        startButton.setBounds(110, 420, 140, 40);
        add(startButton);

        // Level panel with buttons 1-10
        levelPanel = new JPanel(new GridLayout(2, 5, 10, 10));
        levelPanel.setBounds(40, 280, 280, 120); // Diperbesar agar label tidak terpotong

        for (int i = 1; i <= 10; i++) {
            int selectedLevel = i;
            JButton levelBtn = new JButton("Level " + i);
            levelBtn.setFont(new Font("Arial", Font.PLAIN, 12)); // Font kecil agar muat
            levelBtn.setPreferredSize(new Dimension(80, 40)); // Ukuran tombol
            levelBtn.setMargin(new Insets(2, 2, 2, 2)); // Margin sempit
            levelBtn.addActionListener(e -> {
                level = selectedLevel;
                startGame(level);
            });
            levelPanel.add(levelBtn);
        }

        add(levelPanel);

        // Restart button
        restartButton = new JButton("Restart");
        restartButton.setBounds(110, 420, 140, 40);
        restartButton.addActionListener(e -> resetGame());
        restartButton.setVisible(false);
        add(restartButton);

        openingSpace = boardHeight / 4;
    }

    void startGame(int selectedLevel) {
        level = selectedLevel;
        velocityX = -4 - level;
        openingSpace = Math.max(boardHeight / 4 - level * 5, 100);
        score = 0;
        pipes.clear();
        bird.y = birdY;
        velocityY = 0;

        gameState = GameState.PLAYING;
        showButtons(false);
        gameLoop.start();
        pipeTimer.start();
        requestFocusInWindow();
    }

    void resetGame() {
        gameState = GameState.MENU;
        bird.y = birdY;
        score = 0;
        pipes.clear();
        showButtons(true);
        repaint();
        requestFocusInWindow();
    }

    void showButtons(boolean show) {
        startButton.setVisible(false);
        levelPanel.setVisible(show && gameState == GameState.MENU);
        restartButton.setVisible(gameState == GameState.GAME_OVER);
        revalidate();
        repaint();
    }

    void placePipes() {
        int randomPipeY = -pipeHeight / 4 - random.nextInt(pipeHeight / 2);
        Pipe topPipe = new Pipe(boardWidth, randomPipeY, topPipeImg);
        Pipe bottomPipe = new Pipe(boardWidth, randomPipeY + pipeHeight + openingSpace, bottomPipeImg);
        pipes.add(topPipe);
        pipes.add(bottomPipe);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        g.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);

        if (gameState == GameState.MENU) {
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 32));
            g.drawString("Flappy Bird", 90, 100);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Pilih Level & Tekan Start", 80, 150);
        }

        if (gameState == GameState.PLAYING || gameState == GameState.GAME_OVER) {
            g.drawImage(birdImg, bird.x, bird.y, bird.width, bird.height, null);
            for (Pipe pipe : pipes) {
                g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
            }
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Score: " + (int) score, 10, 30);
            g.drawString("Level: " + level, 10, 60);
        }

        if (gameState == GameState.GAME_OVER) {
            g.setFont(new Font("Arial", Font.BOLD, 32));
            g.setColor(Color.red);
            g.drawString("Game Over!", 95, boardHeight / 2 - 40);
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 26));
            g.drawString("Skor Akhir: " + (int) score, 95, boardHeight / 2);
        }
    }

    public void move() {
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0);

        ArrayList<Pipe> toRemove = new ArrayList<>();
        for (Pipe pipe : pipes) {
            pipe.x += velocityX;

            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                score += 0.5;
                pipe.passed = true;
            }

            if (collision(bird, pipe)) {
                gameOver();
            }

            if (pipe.x + pipe.width < 0) {
                toRemove.add(pipe);
            }
        }
        pipes.removeAll(toRemove);

        if (bird.y > boardHeight) {
            gameOver();
        }
    }

    void gameOver() {
        gameState = GameState.GAME_OVER;
        gameLoop.stop();
        pipeTimer.stop();
        showButtons(false);
        restartButton.setVisible(true);
        revalidate();
        repaint();
    }

    boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.PLAYING) {
            move();
            repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameState == GameState.PLAYING && e.getKeyCode() == KeyEvent.VK_SPACE) {
            velocityY = -9;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Flappy Bird - Level Edition");
        FlappyBird game = new FlappyBird();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
