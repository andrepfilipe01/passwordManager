import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellEditor;
import javax.swing.AbstractCellEditor;

public class PasswordGUI {

    private JFrame mainFrame;
    private JPanel loginPanel;
    private JPanel registerPanel;
    private JPanel principalPage;
    private JPanel passwordPage;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private int currentUserId; // Store the current user's ID
    private JTable passwordsTable; // Table to display passwords
    private DefaultTableModel tableModel; // Model for the table
    private java.util.HashMap<Integer, String> passwordsMap; // Map to store row index to password

    public PasswordGUI() {

        mainFrame = new JFrame("Password Manager");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(600, 600);
        mainFrame.setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        passwordsMap = new java.util.HashMap<>();

        createLoginPanel();
        createRegisterPanel();
        createPrincipalPage();
        createPasswordPage();

        mainPanel.add(loginPanel, "login");
        mainPanel.add(registerPanel, "register");
        mainPanel.add(principalPage, "principalPage");
        mainPanel.add(passwordPage, "passwordPage");

        cardLayout.show(mainPanel, "login");

        mainFrame.add(mainPanel);
    }

    private void createPrincipalPage(){
        principalPage = new JPanel();
        principalPage.setLayout(new BorderLayout());

        // Title at the top
        JLabel titleLabel = new JLabel("Your Passwords", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        principalPage.add(titleLabel, BorderLayout.NORTH);

        // Create the table model with columns
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Only make button column editable
                return column == 1;
            }
        };
        tableModel.addColumn("Site");
        tableModel.addColumn("Action");

        // Create the table
        passwordsTable = new JTable(tableModel);
        passwordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set column widths
        passwordsTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        passwordsTable.getColumnModel().getColumn(1).setPreferredWidth(100);

        // Set up button column
        TableColumn buttonColumn = passwordsTable.getColumnModel().getColumn(1);
        buttonColumn.setCellRenderer(new ButtonRenderer());
        buttonColumn.setCellEditor(new ButtonEditor());

        // Add table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(passwordsTable);
        principalPage.add(scrollPane, BorderLayout.CENTER);

        // Button panel at the bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton createPasswordButton = new JButton("Add New Password");
        createPasswordButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "passwordPage");
            }
        });

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadUserPasswords();
            }
        });

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Clear user data before logout
                currentUserId = 0;
                tableModel.setRowCount(0);
                passwordsMap.clear();
                cardLayout.show(mainPanel, "login");
            }
        });

        buttonPanel.add(createPasswordButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(logoutButton);

        principalPage.add(buttonPanel, BorderLayout.SOUTH);
    }

    // Custom button renderer for the table
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Copy Password");
            return this;
        }
    }

    // Custom button editor for the table
    class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private JButton button;
        private int currentRow;

        public ButtonEditor() {
            button = new JButton("Copy Password");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // Copy the password for this row to clipboard
                    String password = passwordsMap.get(currentRow);
                    if (password != null) {
                        StringSelection stringSelection = new StringSelection(password);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(stringSelection, null);
                        JOptionPane.showMessageDialog(mainFrame, "Password copied to clipboard!");
                    }
                    fireEditingStopped();
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            return button;
        }

        public Object getCellEditorValue() {
            return "Copy Password";
        }
    }

    private void loadUserPasswords() {
        // Clear existing data
        tableModel.setRowCount(0);
        passwordsMap.clear();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, site, encrypted_password FROM passwords WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                ResultSet rs = stmt.executeQuery();

                int rowIndex = 0;
                while (rs.next()) {
                    String site = rs.getString("site");
                    String encryptedPassword = rs.getString("encrypted_password");
                    String decryptedPassword = PasswordUtils.decryptPassword(encryptedPassword);

                    // Store the password in the map with the row index as key
                    passwordsMap.put(rowIndex, decryptedPassword);

                    // Add row to table with site and a button placeholder
                    tableModel.addRow(new Object[] { site, "Copy Password" });
                    rowIndex++;
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame, "Error loading passwords: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deletePassword(int passwordId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM passwords WHERE id = ? AND user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, passwordId);
                stmt.setInt(2, currentUserId);
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(mainFrame, "Password deleted successfully!");
                    loadUserPasswords(); // Refresh the table
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Failed to delete password.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame, "Error deleting password: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void createAndShowGUI() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                PasswordGUI gui = new PasswordGUI();
                gui.show();
            }
        });
    }

    private void createPasswordPage() {
        passwordPage = new JPanel();
        passwordPage.setLayout(null);

        JLabel titleLabel = new JLabel("Create New Password");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBounds(180, 30, 250, 30);
        passwordPage.add(titleLabel);

        JLabel siteLabel = new JLabel("Site/App Name:");
        siteLabel.setBounds(150, 80, 100, 25);
        JTextField siteField = new JTextField();
        siteField.setBounds(250, 80, 200, 25);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(150, 120, 100, 25);
        JTextField passwordField = new JTextField();
        passwordField.setBounds(250, 120, 200, 25);

        JLabel lengthLabel = new JLabel("Length:");
        lengthLabel.setBounds(150, 160, 100, 25);
        JSpinner lengthSpinner = new JSpinner(new SpinnerNumberModel(12, 6, 30, 1));
        lengthSpinner.setBounds(250, 160, 60, 25);

        JButton generateButton = new JButton("Generate Password");
        generateButton.setBounds(320, 160, 150, 25);
        generateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int length = (Integer) lengthSpinner.getValue();
                String generatedPassword = GeneratePassword.generatePassword(length);
                passwordField.setText(generatedPassword);
            }
        });

        JButton saveButton = new JButton("Save Password");
        saveButton.setBounds(200, 210, 150, 30);
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String site = siteField.getText();
                String password = passwordField.getText();

                if (site.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(mainFrame, "Please fill in all fields",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                savePassword(site, password);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBounds(360, 210, 100, 30);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                siteField.setText("");
                passwordField.setText("");
                cardLayout.show(mainPanel, "principalPage");
            }
        });

        passwordPage.add(siteLabel);
        passwordPage.add(siteField);
        passwordPage.add(passwordLabel);
        passwordPage.add(passwordField);
        passwordPage.add(lengthLabel);
        passwordPage.add(lengthSpinner);
        passwordPage.add(generateButton);
        passwordPage.add(saveButton);
        passwordPage.add(cancelButton);
    }

    private void savePassword(String site, String password) {
        try (Connection conn = DatabaseConnection.getConnection()) {

            String encryptedPassword = PasswordUtils.encryptPassword(password);

            String sql = "INSERT INTO passwords (user_id, site, encrypted_password) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                stmt.setString(2, site);
                stmt.setString(3, encryptedPassword); // Store encrypted password for security

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(mainFrame, "Password saved successfully!");
                    cardLayout.show(mainPanel, "principalPage");
                    loadUserPasswords(); // Refresh the passwords table
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Failed to save password.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame, "Error saving password: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
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
                    String sql = "SELECT id, password_hash FROM users WHERE username = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, username);
                        ResultSet rs = stmt.executeQuery();

                        if (rs.next()) {
                            String storedHash = rs.getString("password_hash");
                            if (PasswordUtils.verifyPassword(password, storedHash)) {
                                // Store the user ID
                                currentUserId = rs.getInt("id");
                                // Load passwords before showing the page
                                loadUserPasswords();
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