package com.example.voting.client.ui;

import com.example.voting.client.model.Session;
import com.example.voting.client.net.ApiClient;
import com.example.voting.client.util.SwingAsync;
import com.example.voting.common.dto.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AdminFrame extends JFrame {
    private final Session session;
    private final ApiClient api;

    // 发布投票
    private final JTextField titleField = new JTextField();
    private final JTextArea optionsArea = new JTextArea(8, 30);

    // 删除投票
    private final DefaultListModel<PollSummary> pollModel = new DefaultListModel<>();
    private final JList<PollSummary> pollList = new JList<>(pollModel);

    // 用户信息
    private final DefaultTableModel userTableModel = new DefaultTableModel(new Object[]{"ID", "用户名", "管理员"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable userTable = new JTable(userTableModel);

    // 投票明细
    private final JComboBox<PollSummary> votePollCombo = new JComboBox<>();
    private final DefaultTableModel voteTableModel = new DefaultTableModel(new Object[]{"用户", "选项序号", "选项内容", "投票时间"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable voteTable = new JTable(voteTableModel);

    public AdminFrame(Session session) {
        super("管理后台（root）");
        this.session = session;
        this.api = new ApiClient(session);

        if (!session.admin) {
            throw new IllegalStateException("Only admin can open AdminFrame");
        }

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(860, 560);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("发布投票", buildCreateTab());
        tabs.addTab("删除投票", buildDeleteTab());
        tabs.addTab("用户信息", buildUsersTab());
        tabs.addTab("投票明细", buildVotesTab());

        add(tabs, BorderLayout.CENTER);

        // 初次加载数据
        refreshPollsForAdmin();
        refreshUsers();
        refreshPollsForVotesCombo();
    }

    // ========== Tab1：发布 ==========
    private JComponent buildCreateTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        form.add(new JLabel("投票标题"), c);
        c.gridx = 1; c.gridy = 0; c.weightx = 1;
        form.add(titleField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0; c.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("选项(每行一个)"), c);
        c.gridx = 1; c.gridy = 1; c.weightx = 1; c.fill = GridBagConstraints.BOTH; c.weighty = 1;
        optionsArea.setLineWrap(true);
        form.add(new JScrollPane(optionsArea), c);

        p.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton publish = new JButton("发布");
        JButton clear = new JButton("清空");
        btns.add(clear);
        btns.add(publish);
        p.add(btns, BorderLayout.SOUTH);

        clear.addActionListener(e -> {
            titleField.setText("");
            optionsArea.setText("");
        });

        publish.addActionListener(e -> doPublish());

        return p;
    }

    private void doPublish() {
        String title = titleField.getText().trim();
        String raw = optionsArea.getText();

        if (title.isBlank()) {
            JOptionPane.showMessageDialog(this, "标题不能为空");
            return;
        }

        List<String> opts = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            String s = line.trim();
            if (!s.isBlank()) opts.add(s);
        }
        if (opts.size() < 2) {
            JOptionPane.showMessageDialog(this, "选项至少 2 个（每行一个）");
            return;
        }

        SwingAsync.run(this,
                () -> api.adminCreatePoll(title, opts),
                (ApiResponse<CreatePollResponseData> resp) -> {
                    if (!resp.success || resp.data == null) {
                        JOptionPane.showMessageDialog(this, resp.message == null ? "发布失败" : resp.message);
                        return;
                    }
                    JOptionPane.showMessageDialog(this, "发布成功，新投票 ID = " + resp.data.id);
                    refreshPollsForAdmin();
                    refreshPollsForVotesCombo();
                },
                ex -> JOptionPane.showMessageDialog(this, "请求失败：" + ex.getMessage()));
    }

    // ========== Tab2：删除 ==========
    private JComponent buildDeleteTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        pollList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pollList.setFont(pollList.getFont().deriveFont(14f));
        p.add(new JScrollPane(pollList), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("刷新");
        JButton del = new JButton("删除选中的投票");
        btns.add(refresh);
        btns.add(del);
        p.add(btns, BorderLayout.SOUTH);

        refresh.addActionListener(e -> refreshPollsForAdmin());
        del.addActionListener(e -> doDeletePoll());

        return p;
    }

    private void doDeletePoll() {
        PollSummary sel = pollList.getSelectedValue();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "请选择一个投票");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "确定删除投票 #" + sel.id + "？（会同时删除该投票的所有投票记录）",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        SwingAsync.run(this,
                () -> api.adminDeletePoll(sel.id),
                (ApiResponse<Void> resp) -> {
                    if (!resp.success) {
                        JOptionPane.showMessageDialog(this, resp.message == null ? "删除失败" : resp.message);
                        return;
                    }
                    JOptionPane.showMessageDialog(this, "删除成功");
                    refreshPollsForAdmin();
                    refreshPollsForVotesCombo();
                    voteTableModel.setRowCount(0);
                },
                ex -> JOptionPane.showMessageDialog(this, "请求失败：" + ex.getMessage()));
    }

    private void refreshPollsForAdmin() {
        SwingAsync.run(this,
                api::listPolls,
                (ApiResponse<List<PollSummary>> resp) -> {
                    if (!resp.success || resp.data == null) return;
                    pollModel.clear();
                    for (PollSummary s : resp.data) pollModel.addElement(s);
                },
                ex -> {});
    }

    // ========== Tab3：用户信息 ==========
    private JComponent buildUsersTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        userTable.setRowHeight(24);
        p.add(new JScrollPane(userTable), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("刷新");
        btns.add(refresh);
        p.add(btns, BorderLayout.SOUTH);

        refresh.addActionListener(e -> refreshUsers());
        return p;
    }

    private void refreshUsers() {
        SwingAsync.run(this,
                api::adminListUsers,
                (ApiResponse<List<UserInfo>> resp) -> {
                    if (!resp.success || resp.data == null) {
                        JOptionPane.showMessageDialog(this, resp.message == null ? "获取失败" : resp.message);
                        return;
                    }
                    userTableModel.setRowCount(0);
                    for (UserInfo u : resp.data) {
                        userTableModel.addRow(new Object[]{u.id, u.username, u.admin ? "是" : "否"});
                    }
                },
                ex -> JOptionPane.showMessageDialog(this, "请求失败：" + ex.getMessage()));
    }

    // ========== Tab4：投票明细 ==========
    private JComponent buildVotesTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshPolls = new JButton("刷新投票列表");
        JButton loadVotes = new JButton("加载投票明细");
        top.add(new JLabel("选择投票："));
        top.add(votePollCombo);
        top.add(refreshPolls);
        top.add(loadVotes);

        p.add(top, BorderLayout.NORTH);

        voteTable.setRowHeight(24);
        p.add(new JScrollPane(voteTable), BorderLayout.CENTER);

        refreshPolls.addActionListener(e -> refreshPollsForVotesCombo());
        loadVotes.addActionListener(e -> loadVotes());

        return p;
    }

    private void refreshPollsForVotesCombo() {
        SwingAsync.run(this,
                api::listPolls,
                (ApiResponse<List<PollSummary>> resp) -> {
                    if (!resp.success || resp.data == null) return;
                    votePollCombo.removeAllItems();
                    for (PollSummary s : resp.data) votePollCombo.addItem(s);
                },
                ex -> {});
    }

    private void loadVotes() {
        PollSummary sel = (PollSummary) votePollCombo.getSelectedItem();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "请选择一个投票");
            return;
        }

        SwingAsync.run(this,
                () -> api.adminListVotes(sel.id),
                (ApiResponse<List<VoteRecord>> resp) -> {
                    if (!resp.success || resp.data == null) {
                        JOptionPane.showMessageDialog(this, resp.message == null ? "获取失败" : resp.message);
                        return;
                    }
                    voteTableModel.setRowCount(0);
                    for (VoteRecord r : resp.data) {
                        voteTableModel.addRow(new Object[]{r.username, r.optionIndex, r.optionText, r.voteTime});
                    }
                },
                ex -> JOptionPane.showMessageDialog(this, "请求失败：" + ex.getMessage()));
    }
}
