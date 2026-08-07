import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ModernGuessingGame extends JFrame {

    // --- Modern Dark Theme Colors ---
    private static final Color COLOR_BG = new Color(25, 25, 28);        // Deep Charcoal
    private static final Color COLOR_FG = new Color(230, 230, 230);      // Off-White
    private static final Color COLOR_ACCENT = new Color(80, 160, 245);   // Electric Blue
    private static final Color COLOR_PANEL_BG = new Color(40, 40, 45);   // Lighter Dark
    private static final Color COLOR_SUCCESS = new Color(60, 180, 75);    // Neon Green
    private static final Color COLOR_WARNING = new Color(230, 140, 30);   // Vibrant Orange

    // --- GUI Components ---
    private JRadioButton easyBtn, mediumBtn, hardBtn;
    private ButtonGroup difficultyGroup;
    private JTextField guessInput;
    private JButton guessButton, resetButton;
    private JLabel feedbackLabel, attemptsLabel, streakLabel, titleLabel, bestScoreLabel;
    private JPanel gamePanel;
    private CelebrationOverlay celebrationOverlay;

    // --- Game Logic States ---
    private Difficulty currentDifficulty = Difficulty.MEDIUM;
    private int targetNumber;
    private int remainingAttempts;
    private int currentStreak = 0;
    private int currentBestScore = Integer.MAX_VALUE;
    private boolean isGameActive = false;
    private final Random random = new Random();
    private SoundManager soundManager;

    // --- Difficulty Configurations (Tighter / Minimum Trials) ---
    private enum Difficulty {
        EASY(55, 8, "Easy (1-55, 8 Tries)"),
        MEDIUM(100, 7, "Medium (1-100, 7 Tries)"),
        HARD(200, 6, "Hard (1-200, 6 Tries)");

        final int maxRange;
        final int maxAttempts;
        @SuppressWarnings("unused")
        final String displayName;
        
        Difficulty(int maxRange, int maxAttempts, String displayName) {
            this.maxRange = maxRange;
            this.maxAttempts = maxAttempts;
            this.displayName = displayName;
        }
    }

    public ModernGuessingGame() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Elite Guesser Voice Pro + Crackers");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 620);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        soundManager = new SoundManager();
        initializeComponents();
        setupLayout();

        // Attach Celebration Glass Pane Overlay
        celebrationOverlay = new CelebrationOverlay();
        setGlassPane(celebrationOverlay);

        startNewGame(Difficulty.MEDIUM);

        setVisible(true);
    }

    private void initializeComponents() {
        Font titleFont = new Font("SansSerif", Font.BOLD, 28);
        Font mainFont = new Font("SansSerif", Font.PLAIN, 15);
        Font boldFont = new Font("SansSerif", Font.BOLD, 15);

        titleLabel = createStyledLabel("ELITE GUESSER PRO", titleFont, COLOR_ACCENT);
        bestScoreLabel = createStyledLabel("BEST: --", mainFont, COLOR_WARNING);

        attemptsLabel = createStyledLabel("Attempts: 0/0", boldFont, COLOR_FG);
        streakLabel = createStyledLabel("Streak: 0", mainFont, COLOR_FG);

        feedbackLabel = createStyledLabel("Make a guess to start!", boldFont, COLOR_FG);
        feedbackLabel.setOpaque(true);
        feedbackLabel.setBackground(COLOR_PANEL_BG);
        feedbackLabel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_FG, 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Live Toggle Radio Buttons for Modes
        easyBtn = createRadioToggle("Easy (1-55 | 8 Tries)");
        mediumBtn = createRadioToggle("Medium (1-100 | 7 Tries)");
        hardBtn = createRadioToggle("Hard (1-200 | 6 Tries)");
        mediumBtn.setSelected(true);

        difficultyGroup = new ButtonGroup();
        difficultyGroup.add(easyBtn);
        difficultyGroup.add(mediumBtn);
        difficultyGroup.add(hardBtn);

        // Listeners for live dynamic switching
        easyBtn.addActionListener(e -> changeDifficultyLive(Difficulty.EASY));
        mediumBtn.addActionListener(e -> changeDifficultyLive(Difficulty.MEDIUM));
        hardBtn.addActionListener(e -> changeDifficultyLive(Difficulty.HARD));

        guessInput = new JTextField(6);
        styleTextField(guessInput);

        guessButton = createStyledButton("GUESS", COLOR_ACCENT);
        guessButton.addActionListener(new GuessListener());

        resetButton = createStyledButton("RESTART GAME", COLOR_FG);
        resetButton.addActionListener(e -> startNewGame(currentDifficulty));
    }

    private void setupLayout() {
        // --- Header Panel ---
        JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        headerPanel.setBackground(COLOR_BG);
        headerPanel.setBorder(new EmptyBorder(15, 10, 5, 10));
        headerPanel.add(titleLabel);
        headerPanel.add(bestScoreLabel);

        // --- Live Mode Switch Bar ---
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        modePanel.setBackground(COLOR_BG);
        modePanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(COLOR_ACCENT, 1), "Live Mode Switch", 0, 0,
                new Font("SansSerif", Font.BOLD, 12), COLOR_ACCENT));
        modePanel.add(easyBtn);
        modePanel.add(mediumBtn);
        modePanel.add(hardBtn);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBackground(COLOR_BG);
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(modePanel, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // --- Core Game Panel ---
        gamePanel = new JPanel(new GridBagLayout());
        gamePanel.setBackground(COLOR_BG);
        gamePanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Stats Row
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.5;
        gamePanel.add(attemptsLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        gamePanel.add(streakLabel, gbc);

        // Input Row
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JPanel inputContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        inputContainer.setBackground(COLOR_BG);
        inputContainer.add(createStyledLabel("Enter Number:", new Font("SansSerif", Font.BOLD, 16), COLOR_FG));
        inputContainer.add(guessInput);
        inputContainer.add(guessButton);
        gamePanel.add(inputContainer, gbc);

        // Feedback Row
        gbc.gridy = 2; gbc.insets = new Insets(20, 8, 8, 8);
        gamePanel.add(feedbackLabel, gbc);

        add(gamePanel, BorderLayout.CENTER);

        // --- Bottom Control Panel ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(COLOR_BG);
        bottomPanel.setBorder(new EmptyBorder(5, 10, 15, 10));
        bottomPanel.add(resetButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ==========================================
    // ============ Game Logic & State ==========
    // ==========================================

    private void changeDifficultyLive(Difficulty newDifficulty) {
        if (this.currentDifficulty == newDifficulty && isGameActive) return;
        this.currentDifficulty = newDifficulty;
        startNewGame(newDifficulty);
    }

    private void startNewGame(Difficulty difficulty) {
        this.currentDifficulty = difficulty;
        this.targetNumber = random.nextInt(difficulty.maxRange) + 1;
        this.remainingAttempts = difficulty.maxAttempts;
        this.isGameActive = true;

        if (celebrationOverlay != null) {
            celebrationOverlay.stopCelebration();
        }

        updateFeedback("Mode: " + difficulty.name() + " | Guess 1 to " + difficulty.maxRange, COLOR_FG, COLOR_PANEL_BG);
        guessInput.setText("");
        guessInput.setEnabled(true);
        guessButton.setEnabled(true);

        updateStatsDisplay();
        soundManager.speakAndPlay("Game started. Range 1 to " + difficulty.maxRange, "start");
        guessInput.requestFocus();
    }

    private void processGuess() {
        if (!isGameActive) return;

        String input = guessInput.getText().trim();
        guessInput.setText("");

        int guess;
        try {
            guess = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            updateFeedback("🚫 Invalid input! Type a number.", COLOR_FG, COLOR_PANEL_BG);
            soundManager.speakAndPlay("Invalid input", "wrong");
            return;
        }

        if (guess < 1 || guess > currentDifficulty.maxRange) {
            updateFeedback("⚠️ Out of range! (1-" + currentDifficulty.maxRange + ")", COLOR_FG, COLOR_PANEL_BG);
            soundManager.speakAndPlay("Out of range", "wrong");
            return;
        }

        remainingAttempts--;
        updateStatsDisplay();

        if (guess == targetNumber) {
            handleWin();
        } else if (remainingAttempts <= 0) {
            handleLoss();
        } else {
            String hintText = (guess < targetNumber) ? "Too low!" : "Too high!";
            int diff = Math.abs(guess - targetNumber);

            if (diff <= 5) {
                updateFeedback(hintText + " 🔥 Burning Hot! (±5)", COLOR_BG, new Color(255, 100, 100));
            } else if (diff <= 10) {
                updateFeedback(hintText + " ♨️ Getting Warm (±10)", COLOR_BG, COLOR_WARNING);
            } else {
                updateFeedback(hintText, COLOR_FG, COLOR_PANEL_BG);
            }

            soundManager.speakAndPlay(hintText, "wrong");
        }
    }

    private void handleWin() {
        isGameActive = false;
        currentStreak++;
        int attemptsTaken = currentDifficulty.maxAttempts - remainingAttempts;

        updateFeedback("🎉 CORRECT! The number was " + targetNumber, Color.WHITE, COLOR_SUCCESS);

        if (attemptsTaken < currentBestScore) {
            currentBestScore = attemptsTaken;
            bestScoreLabel.setText("🏆 BEST: " + currentBestScore + " Tries (" + currentDifficulty.name() + ")");
        }

        guessInput.setEnabled(false);
        guessButton.setEnabled(false);

        // TRIGGER FIREWORK CELEBRATION CRACKERS
        celebrationOverlay.triggerFireworks();

        soundManager.speakAndPlay("Congratulations! You guessed the correct number!", "correct");
    }

    private void handleLoss() {
        isGameActive = false;
        currentStreak = 0;

        updateFeedback("💥 GAME OVER! The number was " + targetNumber, COLOR_FG, Color.DARK_GRAY);
        guessInput.setEnabled(false);
        guessButton.setEnabled(false);
        soundManager.speakAndPlay("Game Over! You ran out of attempts.", "failure");
    }

    private void updateStatsDisplay() {
        attemptsLabel.setText("Attempts: " + (currentDifficulty.maxAttempts - remainingAttempts) + "/" + currentDifficulty.maxAttempts);
        streakLabel.setText("Streak: " + currentStreak);
    }

    private void updateFeedback(String text, Color fg, Color bg) {
        feedbackLabel.setText(text);
        feedbackLabel.setForeground(fg);
        feedbackLabel.setBackground(bg);
    }

    // ==========================================
    // ============ Styling Utilities ===========
    // ==========================================

    private JLabel createStyledLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private JRadioButton createRadioToggle(String text) {
        JRadioButton btn = new JRadioButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(COLOR_FG);
        btn.setBackground(COLOR_BG);
        btn.setFocusPainted(false);
        return btn;
    }

    private JButton createStyledButton(String text, Color accentColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setForeground(accentColor);
        button.setBackground(COLOR_PANEL_BG);
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(accentColor, 4),
                new EmptyBorder(10, 17, 10, 17)
        ));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(accentColor);
                button.setForeground(COLOR_PANEL_BG);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(COLOR_PANEL_BG);
                button.setForeground(accentColor);
            }
        });
        return button;
    }

    private void styleTextField(JTextField textField) {
        textField.setFont(new Font("SansSerif", Font.BOLD, 24));
        textField.setBackground(COLOR_PANEL_BG);
        textField.setForeground(COLOR_ACCENT);
        textField.setCaretColor(COLOR_ACCENT);
        textField.setHorizontalAlignment(JTextField.CENTER);
        textField.setBorder(new LineBorder(COLOR_FG, 3));
        textField.addActionListener(e -> processGuess());
    }

    // ==========================================
    // ===== Animated Firework Screen Crackers ==
    // ==========================================

    private class CelebrationOverlay extends JComponent {
        private final List<Particle> particles = new ArrayList<>();
        private Timer animTimer;
        private final Random rand = new Random();

        public CelebrationOverlay() {
            setOpaque(false);
            animTimer = new Timer(16, e -> updateAndRepaint()); // ~60 FPS
        }

        public void triggerFireworks() {
            particles.clear();
            int width = Math.max(getWidth(), 400);
            int height = Math.max(getHeight(), 400);

            // Create 5 distinct explosion hubs across the window
            int[] xCenters = {width / 4, width / 2, (3 * width) / 4, width / 3, (2 * width) / 3};
            int[] yCenters = {height / 3, height / 4, height / 3, (2 * height) / 5, height / 2};

            Color[] crackerColors = {
                new Color(255, 50, 50),   // Bright Red
                new Color(255, 215, 0),  // Gold/Yellow
                new Color(50, 255, 100),  // Bright Green
                new Color(0, 191, 255),  // Deep Sky Blue
                new Color(255, 105, 180), // Neon Pink
                new Color(255, 140, 0)   // Orange
            };

            for (int i = 0; i < xCenters.length; i++) {
                int cx = xCenters[i];
                int cy = yCenters[i];

                // Create 60 spark particles per explosion
                for (int p = 0; p < 60; p++) {
                    double angle = rand.nextDouble() * 2 * Math.PI;
                    double speed = 2 + rand.nextDouble() * 7;
                    Color col = crackerColors[rand.nextInt(crackerColors.length)];
                    particles.add(new Particle(cx, cy, Math.cos(angle) * speed, Math.sin(angle) * speed, col));
                }
            }

            setVisible(true);
            animTimer.start();
        }

        public void stopCelebration() {
            animTimer.stop();
            particles.clear();
            setVisible(false);
            repaint();
        }

        private void updateAndRepaint() {
            boolean activeParticlesLeft = false;

            for (Particle p : particles) {
                p.update();
                if (p.alpha > 0) {
                    activeParticlesLeft = true;
                }
            }

            if (!activeParticlesLeft) {
                animTimer.stop();
                setVisible(false);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (Particle p : particles) {
                if (p.alpha > 0) {
                    g2d.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), p.alpha));
                    g2d.fillOval((int) p.x, (int) p.y, p.size, p.size);
                }
            }
            g2d.dispose();
        }

       private class Particle {
            double x, y;
            double vx, vy;
            Color color;
            int alpha = 255;
            int size;

            Particle(double x, double y, double vx, double vy, Color color) {
                this.x = x;
                this.y = y;
                this.vx = vx;
                this.vy = vy;
                this.color = color;
                this.size = 5 + (int) (Math.random() * 4);
            }

            void update() {
                x += vx;
                y += vy;
                vy += 0.15; // Simulated gravity pulling cracker sparks down
                vx *= 0.98; // Air resistance drag
                vy *= 0.98;

                alpha = Math.max(0, alpha - 4); // Smoothly fade opacity
            }
        }
    }

    // ==========================================
    // ===== Dual Audio & Voice Speech Manager ==
    // ==========================================

    private class SoundManager {

        void speakAndPlay(String spokenText, String midiSoundType) {
            new Thread(() -> {
                playMidiSound(midiSoundType);
                speakText(spokenText);
            }).start();
        }

        private void speakText(String text) {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;

                if (os.contains("win")) {
                    String script = "Add-Type -AssemblyName System.Speech; " +
                            "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                            "$synth.Speak('" + text.replace("'", "") + "');";
                    pb = new ProcessBuilder("powershell", "-Command", script);
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("say", text);
                } else {
                    pb = new ProcessBuilder("espeak", text);
                }
                pb.start();
            } catch (IOException e) {
                // Speech non-fatal fallback
            }
        }

        private void playMidiSound(String soundType) {
            try {
                Synthesizer synth = MidiSystem.getSynthesizer();
                synth.open();
                MidiChannel channel = synth.getChannels()[0];

                switch (soundType) {
                    case "start":
                        channel.noteOn(60, 80); Thread.sleep(80);
                        channel.noteOn(64, 80); Thread.sleep(80);
                        channel.noteOn(67, 80);
                        break;
                    case "wrong":
                        channel.noteOn(40, 90); Thread.sleep(120);
                        break;
                    case "correct":
                        channel.noteOn(72, 100); Thread.sleep(100);
                        channel.noteOn(76, 100); Thread.sleep(100);
                        channel.noteOn(84, 100);
                        break;
                    case "failure":
                        channel.noteOn(45, 100); Thread.sleep(150);
                        channel.noteOn(41, 100);
                        break;
                }
                Thread.sleep(150);
                synth.close();
            } catch (Exception e) {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    private class GuessListener implements ActionListener {
        @Override public void actionPerformed(ActionEvent e) { processGuess(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModernGuessingGame::new);
    }
}