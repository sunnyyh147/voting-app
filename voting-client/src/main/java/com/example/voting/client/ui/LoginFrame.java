package com.example.voting.client.ui;

import com.example.voting.client.model.Session;
import com.example.voting.client.net.ApiClient;
import com.example.voting.client.util.SwingAsync;
import com.example.voting.common.dto.ApiResponse;
import com.example.voting.common.dto.LoginResponseData;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final Session session;
    private final ApiClient api;

    private final JTextField baseUrlField = new JTextField("http://127.0.0.1:8080");
    private final JTextField userField = new JTextField();
    private final JPasswordField passField = new JPasswordField();

    private final JTextField regUserField = new JTextField();
    private final JPasswordField regPassField = new JPasswordField();

    public LoginFrame(Session session) {
        super("在线投票系统 - 登录");
        this.session = session;
        this.api = new ApiClient(session);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(560, 340);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(buildForm(), BorderLayout.CENTER);
        add(buildTips(), BorderLayout.SOUTH);
    }

    private JComponent buildForm() {
        JPanel root = new JPanel(new GridLayout(1, 2, 10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // 登录
        JPanel login = new JPanel(new GridBagLayout());
        login.setBorder(BorderFactory.createTitledBorder("登录"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.weightx = 0; c.gridwidth = 1;
        login.add(new JLabel("服务器地址"), c);
        c.gridx = 1; c.gridy = 0; c.weightx = 1;
        login.add(baseUrlField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        login.add(new JLabel("用户名"), c);
        c.gridx = 1; c.gridy = 1; c.weightx = 1;
        login.add(userField, c);

        c.gridx = 0; c.gridy = 2; c.weightx = 0;
        login.add(new JLabel("密码"), c);
        c.gridx = 1; c.gridy = 2; c.weightx = 1;
        login.add(passField, c);

        JButton loginBtn = new JButton("登录");
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; c.weightx = 0;
        login.add(loginBtn, c);

        loginBtn.addActionListener(e -> doLogin());
        passField.addActionListener(e -> doLogin());

        // 注册
        JPanel reg = new JPanel(new GridBagLayout());
        reg.setBorder(BorderFactory.createTitledBorder("注册（可选）"));

        GridBagConstraints r = new GridBagConstraints();
        r.insets = new Insets(6, 6, 6, 6);
        r.fill = GridBagConstraints.HORIZONTAL;

        r.gridx = 0; r.gridy = 0; r.weightx = 0; r.gridwidth = 1;
        reg.add(new JLabel("用户名"), r);
        r.gridx = 1; r.gridy = 0; r.weightx = 1;
        reg.add(regUserField, r);

        r.gridx = 0; r.gridy = 1; r.weightx = 0;
        reg.add(new JLabel("密码"), r);
        r.gridx = 1; r.gridy = 1; r.weightx = 1;
        reg.add(regPassField, r);

        JButton regBtn = new JButton("注册");
        r.gridx = 0; r.gridy = 2; r.gridwidth = 2; r.weightx = 0;
        reg.add(regBtn, r);
        regBtn.addActionListener(e -> doRegister());

        root.add(login);
        root.add(reg);
        return root;
    }

    private JComponent buildTips() {
        JLabel tips = new JLabel("提示：普通用户 test / 123456；管理员 root / root123456（服务端首次启动自动创建）");
        tips.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        return tips;
    }

    private void doLogin() {
        String baseUrl = baseUrlField.getText().trim();
        String u = userField.getText().trim();
        String p = new String(passField.getPassword());

        if (baseUrl.isBlank() || u.isBlank() || p.isBlank()) {
            JOptionPane.showMessageDialog(this, "服务器地址/用户名/密码不能为空");
            return;
        }
        session.baseUrl = baseUrl;

        SwingAsync.run(this,
                () -> api.login(u, p),
                (ApiResponse<LoginResponseData> resp) -> {
                    if (!resp.success || resp.data == null) {
                        JOptionPane.showMessageDialog(this, resp.message == null ? "登录失败" : resp.message);
                        return;
                    }
                    session.token = resp.data.token;
                    session.username = resp.data.username;
                    session.admin = resp.data.admin;

                    PollListFrame f = new PollListFrame(session);
                    f.setVisible(true);
                    dispose();
                },
                ex -> JOptionPane.showMessageDialog(this, "登录请求失败：" + ex.getMessage()));
    }

    private void doRegister() {
        String baseUrl = baseUrlField.getText().trim();
        String u = regUserField.getText().trim();
        String p = new String(regPassField.getPassword());

        if (baseUrl.isBlank() || u.isBlank() || p.isBlank()) {
            JOptionPane.showMessageDialog(this, "服务器地址/用户名/密码不能为空");
            return;
        }
        session.baseUrl = baseUrl;

        SwingAsync.run(this,
                () -> api.register(u, p),
                resp -> {
                    if (!resp.success) {
                        JOptionPane.showMessageDialog(this, resp.message == null ? "注册失败" : resp.message);
                        return;
                    }
                    JOptionPane.showMessageDialog(this, "注册成功，请在左侧登录");
                },
                ex -> JOptionPane.showMessageDialog(this, "注册请求失败：" + ex.getMessage()));
    }
}
