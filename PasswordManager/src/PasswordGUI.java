import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PasswordGUI {
    
    private JFrame mainFrame;
    private JPanel loginPanel;
    private JPanel registerPanel;
    private JPanel principalPage;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    public PasswordGUI() {
        
        mainFrame = new JFrame("Password Manager");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(600, 400);
        mainFrame.setLocationRelativeTo(null); 
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        createLoginPanel();
        createRegisterPanel();
        createPrincipalPage();

        mainPanel.add(loginPanel, "login");
        mainPanel.add(registerPanel, "register");
        mainPanel.add(principalPage, "principalPage");

        cardLayout.show(mainPanel, "login");

        mainFrame.add(mainPanel);
    }
    
    public static void createAndShowGUI() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                PasswordGUI gui = new PasswordGUI();
                gui.show();
            }
        });
    }
    
    private void createPrincipalPage(){
        
        principalPage = new JPanel();
        principalPage.setLayout(null);

        JLabel titleLabel = new JLabel("Password Manager - Main Page");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBounds(180, 30, 300, 30);
        principalPage.add(titleLabel);
        
        JLabel welcomeLabel = new JLabel("Welcome! You are logged in.");
        welcomeLabel.setBounds(200, 80, 200, 25);
        principalPage.add(welcomeLabel);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setBounds(250, 120, 100, 30);
        logoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "login");
            }
        });
        principalPage.add(logoutButton);
        
        JLabel infoLabel = new JLabel("This is where your password list will appear");
        infoLabel.setBounds(150, 170, 300, 25);
        principalPage.add(infoLabel);
    }
    
    private void createLoginPanel() {
        loginPanel = new JPanel();
        loginPanel.setLayout(null);
        
        JLabel titleLabel = new JLabel("Password Manager - Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBounds(180, 30, 250, 30);
        
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(150, 100, 100, 25);
        JTextField usernameField = new JTextField();
        usernameField.setBounds(250, 100, 200, 25);
        
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(150, 140, 100, 25);
        JPasswordField passwordField = new JPasswordField();
        passwordField.setBounds(250, 140, 200, 25);
        
        JButton loginButton = new JButton("Login");
        loginButton.setBounds(250, 190, 100, 30);
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "SELECT password_hash FROM users WHERE username = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, username);
                        ResultSet rs = stmt.executeQuery();

                        if (rs.next()) {
                            String storedHash = rs.getString("password_hash");
                            if (PasswordUtils.verifyPassword(password, storedHash)) {
                                JOptionPane.showMessageDialog(mainFrame, "Login successful!");
                                cardLayout.show(mainPanel, "principalPage");
                            } else {
                                JOptionPane.showMessageDialog(mainFrame, "Invalid password!", "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, "User not found!", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainFrame, "Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });


        JButton registerLink = new JButton("Not registered? Create an account");
        registerLink.setBorderPainted(false);
        registerLink.setContentAreaFilled(false);
        registerLink.setForeground(Color.BLUE);
        registerLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerLink.setBounds(200, 240, 250, 30);
        registerLink.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "register");
            }
        });
        
        loginPanel.add(titleLabel);
        loginPanel.add(usernameLabel);
        loginPanel.add(usernameField);
        loginPanel.add(passwordLabel);
        loginPanel.add(passwordField);
        loginPanel.add(loginButton);
        loginPanel.add(registerLink);
    }
    
    private void createRegisterPanel() {
        registerPanel = new JPanel();
        registerPanel.setLayout(null); 
        
        JLabel titleLabel = new JLabel("Password Manager - Register");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBounds(180, 30, 300, 30);
        
        JLabel usernameLabel = new JLabel("New Username:");
        usernameLabel.setBounds(150, 80, 100, 25);
        JTextField usernameField = new JTextField();
        usernameField.setBounds(250, 80, 200, 25);
       
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setBounds(150, 120, 100, 25);
        JTextField emailField = new JTextField();
        emailField.setBounds(250, 120, 200, 25);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(150, 160, 100, 25);
        JPasswordField passwordField = new JPasswordField();
        passwordField.setBounds(250, 160, 200, 25);
        
        JLabel confirmLabel = new JLabel("Confirm:");
        confirmLabel.setBounds(150, 200, 100, 25);
        JPasswordField confirmField = new JPasswordField();
        confirmField.setBounds(250, 200, 200, 25);
        
        JButton registerButton = new JButton("Register");
        registerButton.setBounds(250, 250, 100, 30);
        registerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String email = emailField.getText();
                String password = new String(passwordField.getPassword());
                String confirmPassword = new String(confirmField.getPassword());

                if (!password.equals(confirmPassword)) {
                    JOptionPane.showMessageDialog(mainFrame, "Passwords do not match!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String hashedPassword = PasswordUtils.hashPassword(password); // Hash password before storing

                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, username);
                        stmt.setString(2, email);
                        stmt.setString(3, hashedPassword);
                        stmt.executeUpdate();
                        JOptionPane.showMessageDialog(mainFrame, "Registration successful!");
                        cardLayout.show(mainPanel, "login");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainFrame, "Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton cancel = new JButton("Cancel");
        cancel.setBounds(360, 250, 100, 30);
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "login");
            }
        });
        
        JButton loginLink = new JButton("Already have an account? Login");
        loginLink.setBorderPainted(false);
        loginLink.setContentAreaFilled(false);
        loginLink.setForeground(Color.BLUE);
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.setBounds(200, 300, 250, 30);
        loginLink.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "login");
            }
        });
        
        registerPanel.add(titleLabel);
        registerPanel.add(usernameLabel);
        registerPanel.add(usernameField);
        registerPanel.add(emailLabel);
        registerPanel.add(emailField);
        registerPanel.add(passwordLabel);
        registerPanel.add(passwordField);
        registerPanel.add(confirmLabel);
        registerPanel.add(confirmField);
        registerPanel.add(registerButton);
        registerPanel.add(cancel);
        registerPanel.add(loginLink);
    }
    
    public void show() {
        mainFrame.setVisible(true);
    }
}