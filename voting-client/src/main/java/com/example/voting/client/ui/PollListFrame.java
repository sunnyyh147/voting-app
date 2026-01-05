package com.example.voting.client.ui;

import com.example.voting.client.model.Session;
import com.example.voting.client.net.ApiClient;
import com.example.voting.client.util.SwingAsync;
import com.example.voting.common.dto.ApiResponse;
import com.example.voting.common.dto.PollSummary;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PollListFrame extends JFrame {
    private final Session session;
    private final ApiClient api;

    private final DefaultListModel<PollSummary> model = new DefaultListModel<>();
    private final JList<PollSummary> list = new JList<>(model);

    public PollListFrame(Session session) {
        super("在线投票系统 - 投票列表（用户：" + session.username + (session.admin ? " / 管理员" : "") + "）");
        this.session = session;
        this.api = new ApiClient(session);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(680, 440);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);

        refreshPolls();
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        JButton refresh = new JButton("刷新");
        JButton open = new JButton("打开投票");
        JButton logout = new JButton("退出登录");

        bar.add(refresh);
        bar.add(open);

        if (session.admin) {
            JButton adminBtn = new JButton("管理后台");
            adminBtn.addActionListener(e -> {
                AdminFrame af = new AdminFrame(session);
                af.setVisible(true);
            });
            bar.add(adminBtn);
        }

        bar.add(logout);

        refresh.addActionListener(e -> refreshPolls());
        open.addActionListener(e -> openSelected());
        logout.addActionListener(e -> logout());

        return bar;
    }

    private JComponent buildCenter() {
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(list.getFont().deriveFont(14f));
        JScrollPane sp = new JScrollPane(list);

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        p.add(sp, BorderLayout.CENTER);

        list.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) openSelected();
            }
        });

        return p;
    }

    private void refreshPolls() {
        SwingAsync.run(this,
                api::listPolls,
                (ApiResponse<List<PollSummary>> resp) -> {
                    if (!resp.success || resp.data == null) {
                        JOptionPane.showMessageDialog(this, resp.message == null ? "获取列表失败" : resp.message);
                        return;
                    }
                    model.clear();
                    for (PollSummary s : resp.data) model.addElement(s);
                },
                ex -> JOptionPane.showMessageDialog(this, "请求失败：" + ex.getMessage()));
    }

    private void openSelected() {
        PollSummary sel = list.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个投票");
            return;
        }
        VoteFrame vf = new VoteFrame(session, sel.id);
        vf.setVisible(true);
    }

    private void logout() {
        session.token = null;
        session.username = null;
        session.admin = false;
        LoginFrame lf = new LoginFrame(session);
        lf.setVisible(true);
        dispose();
    }
}
