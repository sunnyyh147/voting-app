// voting-client/src/main/java/com/example/voting/client/ClientMain.java
package com.example.voting.client;

import com.example.voting.client.model.Session;
import com.example.voting.client.ui.LoginFrame;

import javax.swing.*;

public class ClientMain {
    public static void main(String[] args) {
        // 美观一点：Nimbus
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            Session session = new Session("http://127.0.0.1:8080");
            LoginFrame f = new LoginFrame(session);
            f.setVisible(true);
        });
    }
}
