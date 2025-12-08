// st944 - 12-08-2025
// Clean + fixed ConnectionPanel for Milestone 3 UI integration.
// Handles username, host, port, connects to server, sends handshake,
// and waits for the server to send CLIENT_ID to open the game panel.

package Project.Client;

import java.awt.*;
import javax.swing.*;

public class ConnectionPanel extends JPanel {

    private final Client client;
    private final RpsClientFrame parentFrame;

    private JTextField txtName;
    private JTextField txtHost;
    private JTextField txtPort;

    private JButton btnConnect;
    private JLabel lblStatus;

    public ConnectionPanel(Client client, RpsClientFrame parent) {
        this.client = client;
        this.parentFrame = parent;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Rock Paper Scissors — Connect");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(lblTitle, gbc);

        // ========== USERNAME ==========
        gbc.gridwidth = 1;
        gbc.gridy++;
        add(new JLabel("Name:"), gbc);

        txtName = new JTextField(12);
        gbc.gridx = 1;
        add(txtName, gbc);

        // ========== HOST ==========
        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Host:"), gbc);

        txtHost = new JTextField("localhost", 12);
        gbc.gridx = 1;
        add(txtHost, gbc);

        // ========== PORT ==========
        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Port:"), gbc);

        txtPort = new JTextField("3000", 12);
        gbc.gridx = 1;
        add(txtPort, gbc);

        // ========== CONNECT BUTTON ==========
        btnConnect = new JButton("Connect");
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        btnConnect.addActionListener(e -> doConnect());
        add(btnConnect, gbc);

        // Status label
        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.BLUE);
        gbc.gridy++;
        add(lblStatus, gbc);
    }

    // ==========================================================
    // Attempt to connect using UI values
    // ==========================================================
    private void doConnect() {
        String name = txtName.getText().trim();
        String host = txtHost.getText().trim();
        String portStr = txtPort.getText().trim();

        if (name.isEmpty()) {
            lblStatus.setText("Enter a username.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (Exception e) {
            lblStatus.setText("Invalid port number.");
            return;
        }

        lblStatus.setText("Connecting...");

        // ---- ACTUAL SIMPLE + CORRECT CONNECTION ----
        boolean ok = client.connect(host, port);
        if (!ok) {
            lblStatus.setText("Failed to connect.");
            return;
        }

        // ---- SEND HANDSHAKE (REQUIRED!) ----
        client.sendClientName(name);
        lblStatus.setText("Connected! Waiting for server handshake...");

        // Server will respond with CLIENT_ID, triggering:
        // parentFrame.onConnected(id)
    }
}
