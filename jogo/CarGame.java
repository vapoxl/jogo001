import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;

public class CarGame extends JPanel implements ActionListener, KeyListener, MouseListener {
    // Configurações do jogo
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int ROAD_WIDTH = 400;
    private static final int LANE_COUNT = 4;
    private static final int LANE_WIDTH = ROAD_WIDTH / LANE_COUNT;
    
    // Carro do jogador
    private int playerX;
    private int playerY = 450;
    private int playerWidth = 50;
    private int playerHeight = 80;
    private int playerSpeed = 8;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    
    // Sistema de obstáculos
    private ArrayList<Obstacle> obstacles;
    private ArrayList<ExplosionParticle> explosionParticles;
    private Random random;
    
    // Controle do jogo - CORREÇÃO: usar javax.swing.Timer explicitamente
    private javax.swing.Timer gameTimer;
    private int score = 0;
    private int highScore = 0;
    private int gameSpeed = 5;
    private boolean gameRunning = true;
    private boolean showingExplosion = false;
    private int explosionTimer = 0;
    
    // Cores
    private Color[] obstacleColors = {Color.BLUE, Color.MAGENTA, Color.CYAN, Color.ORANGE, Color.GREEN};
    private Color roadColor = new Color(80, 80, 80);
    private Color grassColor = new Color(0, 150, 0);
    
    // Botão restart
    private Rectangle restartButton;
    private boolean mouseOverButton = false;

    public CarGame() {
        // Inicialização
        random = new Random();
        obstacles = new ArrayList<>();
        explosionParticles = new ArrayList<>();
        restartButton = new Rectangle(WIDTH/2 - 100, HEIGHT/2 + 80, 200, 50);
        
        // Configuração do painel
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(grassColor);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        
        // Posicionar jogador no centro
        playerX = getLaneCenter(1);
        
        // Iniciar jogo
        startGame();
    }

    // Classe para obstáculos
    class Obstacle {
        int x, y, width, height, lane;
        Color color;
        int type; // 0: carro normal, 1: caminhão, 2: carro esportivo
        
        Obstacle(int lane, int y) {
            this.lane = lane;
            this.x = getLaneCenter(lane) - 25;
            this.y = y;
            this.type = random.nextInt(3);
            
            switch(type) {
                case 0: // Carro normal
                    width = 50;
                    height = 80;
                    break;
                case 1: // Caminhão
                    width = 60;
                    height = 100;
                    break;
                case 2: // Carro esportivo
                    width = 45;
                    height = 70;
                    break;
            }
            
            this.color = obstacleColors[random.nextInt(obstacleColors.length)];
        }
        
        Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    // Classe para partículas de explosão
    class ExplosionParticle {
        double x, y;
        double velocityX, velocityY;
        double size;
        int life;
        Color color;
        
        ExplosionParticle(double x, double y) {
            this.x = x;
            this.y = y;
            this.velocityX = (random.nextDouble() - 0.5) * 8;
            this.velocityY = (random.nextDouble() - 0.5) * 8;
            this.size = random.nextDouble() * 15 + 5;
            this.life = random.nextInt(40) + 30;
            this.color = new Color(
                255,
                random.nextInt(155) + 100,
                random.nextInt(100),
                random.nextInt(200) + 55
            );
        }
        
        void update() {
            x += velocityX;
            y += velocityY;
            velocityY += 0.1; // Gravidade
            life--;
            size *= 0.97; // Reduz tamanho
        }
    }

    private int getLaneCenter(int lane) {
        int roadStart = (WIDTH - ROAD_WIDTH) / 2;
        return roadStart + (lane * LANE_WIDTH) - (LANE_WIDTH / 2);
    }

    public void startGame() {
        gameRunning = true;
        showingExplosion = false;
        score = 0;
        gameSpeed = 5;
        obstacles.clear();
        explosionParticles.clear();
        playerX = getLaneCenter(1);
        
        if (gameTimer != null) {
            gameTimer.stop();
        }
        
        // CORREÇÃO: usar javax.swing.Timer explicitamente
        gameTimer = new javax.swing.Timer(16, this);
        gameTimer.start();
        
        requestFocus();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Melhorar qualidade gráfica
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        drawRoad(g2d);
        drawObstacles(g2d);
        drawPlayerCar(g2d);
        
        if (showingExplosion) {
            drawExplosion(g2d);
        }
        
        drawHUD(g2d);
        
        if (!gameRunning && !showingExplosion) {
            drawGameOverScreen(g2d);
        }
    }

    private void drawRoad(Graphics2D g2d) {
        int roadX = (WIDTH - ROAD_WIDTH) / 2;
        
        // Estrada principal
        g2d.setColor(roadColor);
        g2d.fillRect(roadX, 0, ROAD_WIDTH, HEIGHT);
        
        // Bordas da estrada
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(roadX - 5, 0, 5, HEIGHT);
        g2d.fillRect(roadX + ROAD_WIDTH, 0, 5, HEIGHT);
        
        // Linhas divisórias
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < HEIGHT; i += 40) {
            for (int lane = 1; lane < LANE_COUNT; lane++) {
                int lineX = roadX + (lane * LANE_WIDTH) - 2;
                g2d.fillRect(lineX, i, 4, 20);
            }
        }
        
        // Linhas centrais tracejadas
        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0);
        g2d.setStroke(dashed);
        g2d.setColor(Color.YELLOW);
        g2d.drawLine(roadX + ROAD_WIDTH/2, 0, roadX + ROAD_WIDTH/2, HEIGHT);
        g2d.setStroke(new BasicStroke(1));
    }

    private void drawPlayerCar(Graphics2D g2d) {
        // Sombra
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(playerX - 5, playerY + playerHeight - 10, playerWidth + 10, 15);
        
        // Corpo do carro
        GradientPaint carPaint = new GradientPaint(
            playerX, playerY, Color.RED,
            playerX, playerY + playerHeight, new Color(200, 0, 0)
        );
        g2d.setPaint(carPaint);
        g2d.fillRoundRect(playerX, playerY, playerWidth, playerHeight, 15, 15);
        
        // Vidro
        g2d.setColor(new Color(150, 200, 255, 150));
        g2d.fillRect(playerX + 5, playerY + 10, playerWidth - 10, 20);
        
        // Faróis
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(playerX + 5, playerY, 8, 5);
        g2d.fillRect(playerX + playerWidth - 13, playerY, 8, 5);
        
        // Rodas
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillOval(playerX - 5, playerY + 15, 10, 20);
        g2d.fillOval(playerX + playerWidth - 5, playerY + 15, 10, 20);
        g2d.fillOval(playerX - 5, playerY + playerHeight - 35, 10, 20);
        g2d.fillOval(playerX + playerWidth - 5, playerY + playerHeight - 35, 10, 20);
    }

    private void drawObstacles(Graphics2D g2d) {
        for (Obstacle obstacle : obstacles) {
            // Sombra
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillOval(obstacle.x - 5, obstacle.y + obstacle.height - 10, obstacle.width + 10, 15);
            
            // Corpo do veículo
            GradientPaint obstaclePaint = new GradientPaint(
                obstacle.x, obstacle.y, obstacle.color,
                obstacle.x, obstacle.y + obstacle.height, obstacle.color.darker()
            );
            g2d.setPaint(obstaclePaint);
            
            switch(obstacle.type) {
                case 0: // Carro normal
                    g2d.fillRoundRect(obstacle.x, obstacle.y, obstacle.width, obstacle.height, 10, 10);
                    break;
                case 1: // Caminhão
                    g2d.fillRect(obstacle.x, obstacle.y, obstacle.width, obstacle.height);
                    // Carroceria
                    g2d.setColor(obstacle.color.darker().darker());
                    g2d.fillRect(obstacle.x + 10, obstacle.y + 20, obstacle.width - 20, obstacle.height - 40);
                    break;
                case 2: // Carro esportivo (baixo)
                    g2d.fillRoundRect(obstacle.x, obstacle.y, obstacle.width, obstacle.height, 20, 20);
                    break;
            }
            
            // Vidro
            g2d.setColor(new Color(200, 230, 255, 180));
            g2d.fillRect(obstacle.x + 5, obstacle.y + 8, obstacle.width - 10, 15);
            
            // Faróis
            g2d.setColor(Color.WHITE);
            g2d.fillRect(obstacle.x + 5, obstacle.y, 6, 4);
            g2d.fillRect(obstacle.x + obstacle.width - 11, obstacle.y, 6, 4);
        }
    }

    private void drawExplosion(Graphics2D g2d) {
        for (ExplosionParticle particle : explosionParticles) {
            if (particle.life > 0) {
                float alpha = (float) particle.life / 70.0f;
                AlphaComposite alphaComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
                g2d.setComposite(alphaComp);
                
                g2d.setColor(particle.color);
                g2d.fill(new Ellipse2D.Double(particle.x, particle.y, particle.size, particle.size));
                
                // Brilho interno
                if (alpha > 0.5f) {
                    g2d.setColor(Color.YELLOW);
                    g2d.fill(new Ellipse2D.Double(particle.x + 2, particle.y + 2, particle.size - 4, particle.size - 4));
                }
            }
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        
        // Placar
        g2d.drawString("SCORE: " + score, 20, 30);
        g2d.drawString("HIGH SCORE: " + highScore, 20, 60);
        g2d.drawString("SPEED: " + gameSpeed, 20, 90);
        
        // Velocímetro
        int speedX = WIDTH - 120;
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRoundRect(speedX, 20, 80, 20, 10, 10);
        g2d.setColor(Color.RED);
        int speedWidth = (gameSpeed * 80) / 15;
        g2d.fillRoundRect(speedX, 20, Math.min(speedWidth, 80), 20, 10, 10);
        g2d.setColor(Color.WHITE);
        g2d.drawString("SPD", speedX + 30, 35);
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        // Overlay escuro
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Título
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("GAME OVER", WIDTH/2 - 140, HEIGHT/2 - 60);
        
        // Estatísticas
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Final Score: " + score, WIDTH/2 - 100, HEIGHT/2 - 10);
        g2d.drawString("High Score: " + highScore, WIDTH/2 - 100, HEIGHT/2 + 20);
        
        // Estrelas de classificação
        int stars = Math.min(5, score / 1000);
        g2d.setColor(Color.YELLOW);
        for (int i = 0; i < 5; i++) {
            if (i < stars) {
                g2d.fillPolygon(new int[]{WIDTH/2 - 100 + i*40, WIDTH/2 - 80 + i*40, WIDTH/2 - 60 + i*40}, 
                               new int[]{HEIGHT/2 + 50, HEIGHT/2 + 30, HEIGHT/2 + 50}, 3);
                g2d.fillPolygon(new int[]{WIDTH/2 - 100 + i*40, WIDTH/2 - 80 + i*40, WIDTH/2 - 60 + i*40}, 
                               new int[]{HEIGHT/2 + 50, HEIGHT/2 + 70, HEIGHT/2 + 50}, 3);
            } else {
                g2d.setColor(Color.GRAY);
                g2d.fillPolygon(new int[]{WIDTH/2 - 100 + i*40, WIDTH/2 - 80 + i*40, WIDTH/2 - 60 + i*40}, 
                               new int[]{HEIGHT/2 + 50, HEIGHT/2 + 30, HEIGHT/2 + 50}, 3);
                g2d.fillPolygon(new int[]{WIDTH/2 - 100 + i*40, WIDTH/2 - 80 + i*40, WIDTH/2 - 60 + i*40}, 
                               new int[]{HEIGHT/2 + 50, HEIGHT/2 + 70, HEIGHT/2 + 50}, 3);
            }
        }
        
        // Botão restart
        Color buttonColor = mouseOverButton ? new Color(200, 50, 50) : new Color(220, 20, 60);
        GradientPaint buttonPaint = new GradientPaint(
            restartButton.x, restartButton.y, buttonColor,
            restartButton.x, restartButton.y + restartButton.height, buttonColor.darker()
        );
        g2d.setPaint(buttonPaint);
        g2d.fillRoundRect(restartButton.x, restartButton.y, restartButton.width, restartButton.height, 25, 25);
        
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(restartButton.x, restartButton.y, restartButton.width, restartButton.height, 25, 25);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        String buttonText = "🔄 REINICIAR JOGO";
        int textWidth = g2d.getFontMetrics().stringWidth(buttonText);
        g2d.drawString(buttonText, restartButton.x + (restartButton.width - textWidth)/2, restartButton.y + 32);
        
        // Instruções
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("Pressione ESPAÇO ou clique no botão", WIDTH/2 - 140, HEIGHT/2 + 150);
    }

    private void updateGame() {
        if (!gameRunning) return;
        
        // Movimento do jogador
        if (movingLeft && playerX > (WIDTH - ROAD_WIDTH) / 2) {
            playerX -= playerSpeed;
        }
        if (movingRight && playerX < (WIDTH + ROAD_WIDTH) / 2 - playerWidth) {
            playerX += playerSpeed;
        }
        
        // Atualizar obstáculos
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.y += gameSpeed;
            
            if (obstacle.y > HEIGHT) {
                obstacles.remove(i);
                score += 10;
            }
        }
        
        // Gerar novos obstáculos
        if (random.nextInt(60 - gameSpeed * 2) == 0) {
            int lane = random.nextInt(LANE_COUNT);
            obstacles.add(new Obstacle(lane, -100));
        }
        
        // Verificar colisões
        Rectangle playerRect = new Rectangle(playerX, playerY, playerWidth, playerHeight);
        for (Obstacle obstacle : obstacles) {
            if (playerRect.intersects(obstacle.getBounds())) {
                createExplosion(playerX + playerWidth/2, playerY + playerHeight/2);
                gameOver();
                return;
            }
        }
        
        // Aumentar dificuldade
        if (score > 0 && score % 500 == 0) {
            gameSpeed = Math.min(15, 5 + score / 500);
        }
    }

    private void createExplosion(int x, int y) {
        showingExplosion = true;
        explosionTimer = 60;
        
        for (int i = 0; i < 100; i++) {
            explosionParticles.add(new ExplosionParticle(x, y));
        }
    }

    private void updateExplosion() {
        explosionTimer--;
        
        for (int i = explosionParticles.size() - 1; i >= 0; i--) {
            ExplosionParticle particle = explosionParticles.get(i);
            particle.update();
            
            if (particle.life <= 0) {
                explosionParticles.remove(i);
            }
        }
        
        if (explosionTimer <= 0) {
            showingExplosion = false;
        }
    }

    private void gameOver() {
        gameRunning = false;
        gameTimer.stop();
        
        if (score > highScore) {
            highScore = score;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning) {
            updateGame();
        }
        
        if (showingExplosion) {
            updateExplosion();
        }
        
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        if (keyCode == KeyEvent.VK_LEFT) {
            movingLeft = true;
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            movingRight = true;
        } else if (keyCode == KeyEvent.VK_SPACE) {
            if (!gameRunning && !showingExplosion) {
                startGame();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        if (keyCode == KeyEvent.VK_LEFT) {
            movingLeft = false;
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            movingRight = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!gameRunning && !showingExplosion && restartButton.contains(e.getPoint())) {
            startGame();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {
        mouseOverButton = false;
    }

    public void mouseMoved(MouseEvent e) {
        mouseOverButton = restartButton.contains(e.getPoint());
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("🚗 ULTIMATE CAR GAME - AGORA VAI! 🚗");
            CarGame game = new CarGame();
            
            // Adicionar mouse motion listener para hover effect
            frame.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    // Converter coordenadas da janela para coordenadas do painel
                    Point point = SwingUtilities.convertPoint(frame, e.getPoint(), game);
                    game.mouseMoved(new MouseEvent(game, e.getID(), e.getWhen(), e.getModifiersEx(), 
                                                 point.x, point.y, e.getClickCount(), e.isPopupTrigger()));
                }
            });
            
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}