import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Modernized Bounce-style Java game.
 *
 * Controls:
 * - Left/Right or A/D: move
 * - Up, W, or Space: jump (double-jump unlockable)
 * - R: restart after game over / win
 */
public class BounceGame extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 640;

    private static final double GRAVITY = 0.65;
    private static final double MOVE_SPEED = 4.2;
    private static final double JUMP_VELOCITY = -12.8;

    private final Timer timer = new Timer(16, this);
    private final Random random = new Random();

    private final Player player = new Player(80, 200, 28);
    private final List<Platform> platforms = new ArrayList<>();
    private final List<Hazard> hazards = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Coin> coins = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();

    private Goal goal;

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;

    private int cameraX;
    private int score;
    private int highScore;
    private int lives = 3;
    private int levelLength;

    private long shieldUntil;
    private long slowMotionUntil;
    private long comboUntil;

    private boolean gameOver;
    private boolean gameWon;

    public BounceGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(15, 18, 30));
        setFocusable(true);
        addKeyListener(this);
        initLevel();
        timer.start();
    }

    private void initLevel() {
        platforms.clear();
        hazards.clear();
        enemies.clear();
        coins.clear();
        powerUps.clear();

        score = 0;
        lives = 3;
        cameraX = 0;
        gameOver = false;
        gameWon = false;
        shieldUntil = 0;
        slowMotionUntil = 0;
        comboUntil = 0;

        player.reset(80, 200);

        levelLength = 5200;

        // Ground and elevated platforms
        platforms.add(new Platform(0, 560, levelLength, 80));
        platforms.add(new Platform(200, 470, 180, 20));
        platforms.add(new Platform(470, 410, 150, 20));
        platforms.add(new Platform(680, 350, 160, 20));
        platforms.add(new Platform(980, 300, 140, 20));
        platforms.add(new Platform(1230, 250, 220, 20));
        platforms.add(new Platform(1600, 450, 250, 20));
        platforms.add(new Platform(2000, 380, 240, 20));
        platforms.add(new Platform(2380, 320, 180, 20));
        platforms.add(new Platform(2700, 280, 180, 20));
        platforms.add(new Platform(3000, 360, 220, 20));
        platforms.add(new Platform(3360, 300, 190, 20));
        platforms.add(new Platform(3680, 240, 220, 20));
        platforms.add(new Platform(4060, 410, 190, 20));
        platforms.add(new Platform(4400, 340, 220, 20));
        platforms.add(new Platform(4700, 280, 250, 20));

        // Hazards
        hazards.add(new Hazard(560, 540, 90, 20));
        hazards.add(new Hazard(1140, 540, 120, 20));
        hazards.add(new Hazard(1850, 540, 110, 20));
        hazards.add(new Hazard(2520, 540, 120, 20));
        hazards.add(new Hazard(3270, 540, 120, 20));
        hazards.add(new Hazard(3960, 540, 140, 20));

        // Moving enemies (drone-like)
        enemies.add(new Enemy(840, 520, 42, 760, 980));
        enemies.add(new Enemy(1700, 430, 42, 1600, 1870));
        enemies.add(new Enemy(2850, 240, 42, 2700, 3030));
        enemies.add(new Enemy(4200, 370, 42, 4060, 4450));

        // Coins
        for (int x = 230; x < 4900; x += 190) {
            int y = 180 + random.nextInt(260);
            coins.add(new Coin(x, y, 12));
        }

        // Power-ups
        powerUps.add(new PowerUp(650, 300, PowerType.SHIELD));
        powerUps.add(new PowerUp(1460, 200, PowerType.SLOW_MOTION));
        powerUps.add(new PowerUp(2600, 230, PowerType.DOUBLE_JUMP));
        powerUps.add(new PowerUp(3900, 190, PowerType.SHIELD));

        goal = new Goal(levelLength - 150, 220, 70, 320);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint();
    }

    private void updateGame() {
        if (gameOver || gameWon) {
            return;
        }

        final long now = System.currentTimeMillis();
        final boolean slowMode = now < slowMotionUntil;
        double dt = slowMode ? 0.6 : 1.0;

        if (leftPressed) player.vx = -MOVE_SPEED;
        else if (rightPressed) player.vx = MOVE_SPEED;
        else player.vx *= 0.8;

        if (jumpPressed) {
            if (player.onGround) {
                player.vy = JUMP_VELOCITY;
                player.onGround = false;
            } else if (player.canDoubleJump && !player.doubleJumpUsed) {
                player.vy = JUMP_VELOCITY * 0.92;
                player.doubleJumpUsed = true;
            }
            jumpPressed = false;
        }

        player.vy += GRAVITY * dt;
        player.x += player.vx * dt;
        player.y += player.vy * dt;

        player.onGround = false;

        Rectangle playerBounds = player.bounds();
        for (Platform p : platforms) {
            Rectangle pb = p.bounds();
            if (playerBounds.intersects(pb)) {
                Rectangle inter = playerBounds.intersection(pb);
                if (inter.height < inter.width) {
                    if (player.vy > 0) {
                        player.y -= inter.height;
                        player.vy = 0;
                        player.onGround = true;
                        player.doubleJumpUsed = false;
                    } else if (player.vy < 0) {
                        player.y += inter.height;
                        player.vy = 0.5;
                    }
                } else {
                    if (player.x < p.x) player.x -= inter.width;
                    else player.x += inter.width;
                    player.vx *= -0.15;
                }
                playerBounds = player.bounds();
            }
        }

        if (player.y > HEIGHT + 160) {
            hitPlayer();
        }

        for (Enemy enemy : enemies) {
            enemy.update(dt);
            if (player.bounds().intersects(enemy.bounds())) {
                hitPlayer();
            }
        }

        for (Hazard hazard : hazards) {
            if (player.bounds().intersects(hazard.bounds())) {
                hitPlayer();
            }
        }

        Iterator<Coin> coinIterator = coins.iterator();
        while (coinIterator.hasNext()) {
            Coin coin = coinIterator.next();
            if (player.bounds().intersects(coin.bounds())) {
                int gain = (now < comboUntil) ? 20 : 10;
                score += gain;
                comboUntil = now + 2500;
                coinIterator.remove();
            }
        }

        Iterator<PowerUp> powerIterator = powerUps.iterator();
        while (powerIterator.hasNext()) {
            PowerUp pu = powerIterator.next();
            if (player.bounds().intersects(pu.bounds())) {
                applyPower(pu.type, now);
                score += 50;
                powerIterator.remove();
            }
        }

        if (player.bounds().intersects(goal.bounds())) {
            gameWon = true;
            score += 500 + lives * 100;
            highScore = Math.max(highScore, score);
        }

        player.x = Math.max(0, Math.min(levelLength - player.size, player.x));
        cameraX = (int) player.x - WIDTH / 3;
        cameraX = Math.max(0, Math.min(levelLength - WIDTH, cameraX));
    }

    private void applyPower(PowerType type, long now) {
        switch (type) {
            case SHIELD -> shieldUntil = now + 7000;
            case SLOW_MOTION -> slowMotionUntil = now + 6500;
            case DOUBLE_JUMP -> player.canDoubleJump = true;
        }
    }

    private void hitPlayer() {
        long now = System.currentTimeMillis();
        if (now < shieldUntil) {
            shieldUntil = 0;
            return;
        }
        lives--;
        if (lives <= 0) {
            gameOver = true;
            highScore = Math.max(highScore, score);
        } else {
            player.reset(Math.max(60, player.x - 120), 120);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // parallax background
        drawBackground(g2);

        g2.translate(-cameraX, 0);

        for (Platform p : platforms) p.draw(g2);
        for (Hazard h : hazards) h.draw(g2);
        for (Enemy enemy : enemies) enemy.draw(g2);
        for (Coin c : coins) c.draw(g2);
        for (PowerUp pu : powerUps) pu.draw(g2);
        goal.draw(g2);
        player.draw(g2, System.currentTimeMillis() < shieldUntil);

        g2.translate(cameraX, 0);

        drawHud(g2);

        if (gameOver) drawOverlay(g2, "GAME OVER", "Press R to restart");
        if (gameWon) drawOverlay(g2, "LEVEL CLEAR!", "Press R to play again");
    }

    private void drawBackground(Graphics2D g2) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(25, 18, 50), 0, HEIGHT, new Color(7, 12, 25));
        g2.setPaint(sky);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(new Color(80, 120, 255, 70));
        for (int i = 0; i < 8; i++) {
            int x = (i * 280 - (cameraX / 4) % 280);
            g2.fillRoundRect(x, 70 + (i % 3) * 35, 170, 36, 20, 20);
        }

        g2.setColor(new Color(110, 70, 180, 110));
        for (int i = 0; i < 12; i++) {
            int x = (i * 210 - (cameraX / 2) % 210);
            int h = 120 + (i % 5) * 30;
            g2.fillRect(x, HEIGHT - h, 90, h);
        }
    }

    private void drawHud(Graphics2D g2) {
        long now = System.currentTimeMillis();

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(16, 16, 330, 102, 16, 16);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("Score: " + score, 30, 44);
        g2.drawString("Lives: " + lives, 30, 70);
        g2.drawString("High Score: " + highScore, 30, 96);

        int x = 360;
        if (now < shieldUntil) drawStatusPill(g2, x, 20, "Shield", new Color(0, 220, 255));
        if (now < slowMotionUntil) drawStatusPill(g2, x, 54, "Slow Motion", new Color(255, 190, 40));
        if (player.canDoubleJump) drawStatusPill(g2, x, 88, "Double Jump", new Color(190, 100, 255));
    }

    private void drawStatusPill(Graphics2D g2, int x, int y, String text, Color color) {
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
        g2.fillRoundRect(x, y, 156, 24, 12, 12);
        g2.setColor(color);
        g2.drawRoundRect(x, y, 156, 24, 12, 12);
        g2.drawString(text, x + 10, y + 17);
    }

    private void drawOverlay(Graphics2D g2, String title, String subtitle) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 52));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (WIDTH - fm.stringWidth(title)) / 2;
        g2.drawString(title, tx, HEIGHT / 2 - 20);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 24));
        fm = g2.getFontMetrics();
        int sx = (WIDTH - fm.stringWidth(subtitle)) / 2;
        g2.drawString(subtitle, sx, HEIGHT / 2 + 22);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = true;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = true;
            case KeyEvent.VK_UP, KeyEvent.VK_W, KeyEvent.VK_SPACE -> jumpPressed = true;
            case KeyEvent.VK_R -> {
                if (gameOver || gameWon) {
                    initLevel();
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = false;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bounce Recharged");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            BounceGame game = new BounceGame();
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static class Player {
        double x;
        double y;
        double vx;
        double vy;
        final int size;
        boolean onGround;
        boolean canDoubleJump;
        boolean doubleJumpUsed;

        Player(double x, double y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }

        void reset(double nx, double ny) {
            x = nx;
            y = ny;
            vx = 0;
            vy = 0;
            onGround = false;
            doubleJumpUsed = false;
        }

        Rectangle bounds() {
            return new Rectangle((int) x, (int) y, size, size);
        }

        void draw(Graphics2D g2, boolean shielded) {
            if (shielded) {
                g2.setColor(new Color(0, 220, 255, 85));
                g2.fill(new Ellipse2D.Double(x - 8, y - 8, size + 16, size + 16));
            }
            g2.setColor(new Color(255, 105, 180));
            g2.fill(new Ellipse2D.Double(x, y, size, size));
            g2.setColor(new Color(255, 245, 250));
            g2.draw(new Ellipse2D.Double(x, y, size, size));
        }
    }

    private record Platform(int x, int y, int w, int h) {
        Rectangle bounds() {
            return new Rectangle(x, y, w, h);
        }

        void draw(Graphics2D g2) {
            g2.setColor(new Color(45, 55, 85));
            g2.fillRoundRect(x, y, w, h, 10, 10);
            g2.setColor(new Color(140, 170, 255));
            g2.drawRoundRect(x, y, w, h, 10, 10);
        }
    }

    private record Hazard(int x, int y, int w, int h) {
        Rectangle bounds() {
            return new Rectangle(x, y, w, h);
        }

        void draw(Graphics2D g2) {
            g2.setColor(new Color(240, 40, 90));
            int spikes = Math.max(4, w / 20);
            for (int i = 0; i < spikes; i++) {
                int sx = x + i * (w / spikes);
                int[] xs = {sx, sx + (w / spikes) / 2, sx + (w / spikes)};
                int[] ys = {y + h, y, y + h};
                g2.fillPolygon(xs, ys, 3);
            }
        }
    }

    private static class Enemy {
        double x;
        final double y;
        final int size;
        final int minX;
        final int maxX;
        double speed = 2.4;

        Enemy(double x, double y, int size, int minX, int maxX) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.minX = minX;
            this.maxX = maxX;
        }

        void update(double dt) {
            x += speed * dt;
            if (x < minX || x + size > maxX) {
                speed *= -1;
            }
        }

        Rectangle bounds() {
            return new Rectangle((int) x, (int) y, size, size - 10);
        }

        void draw(Graphics2D g2) {
            g2.setColor(new Color(255, 100, 80));
            g2.fillRoundRect((int) x, (int) y, size, size - 10, 14, 14);
            g2.setColor(Color.WHITE);
            g2.fillOval((int) x + 10, (int) y + 10, 8, 8);
            g2.fillOval((int) x + 24, (int) y + 10, 8, 8);
        }
    }

    private record Coin(int x, int y, int r) {
        Rectangle bounds() {
            return new Rectangle(x - r, y - r, r * 2, r * 2);
        }

        void draw(Graphics2D g2) {
            g2.setColor(new Color(255, 214, 70));
            g2.fillOval(x - r, y - r, r * 2, r * 2);
            g2.setColor(new Color(255, 246, 190));
            g2.drawOval(x - r, y - r, r * 2, r * 2);
        }
    }

    private enum PowerType { SHIELD, SLOW_MOTION, DOUBLE_JUMP }

    private record PowerUp(int x, int y, PowerType type) {
        Rectangle bounds() {
            return new Rectangle(x - 14, y - 14, 28, 28);
        }

        void draw(Graphics2D g2) {
            Color color = switch (type) {
                case SHIELD -> new Color(0, 230, 255);
                case SLOW_MOTION -> new Color(255, 180, 50);
                case DOUBLE_JUMP -> new Color(195, 110, 255);
            };
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
            g2.fillOval(x - 16, y - 16, 32, 32);
            g2.setColor(color);
            g2.fillRoundRect(x - 10, y - 10, 20, 20, 8, 8);
        }
    }

    private record Goal(int x, int y, int w, int h) {
        Rectangle bounds() {
            return new Rectangle(x, y, w, h);
        }

        void draw(Graphics2D g2) {
            g2.setColor(new Color(80, 255, 160));
            g2.drawRoundRect(x, y, w, h, 14, 14);
            g2.setColor(new Color(80, 255, 160, 40));
            g2.fillRoundRect(x, y, w, h, 14, 14);
            g2.setColor(Color.WHITE);
            g2.drawString("EXIT", x + 16, y + h / 2);
        }
    }
}
