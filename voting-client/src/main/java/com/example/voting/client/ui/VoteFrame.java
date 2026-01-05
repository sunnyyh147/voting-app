// voting-client/src/main/java/com/example/voting/client/ui/VoteFrame.java
package com.example.voting.client.ui;

import com.example.voting.client.model.Session;
import com.example.voting.client.net.ApiClient;
import com.example.voting.client.util.SwingAsync;
import com.example.voting.common.dto.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class VoteFrame extends JFrame {
    private final Session session;
    private final ApiClient api;
    private final int pollId;

    private final JLabel titleLabel = new JLabel("加载中...");
    private final JPanel optionsPanel = new JPanel();
    private final ButtonGroup group = new ButtonGroup();

    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"选项", "票数"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable resultTable = new JTable(tableModel);

    private PollDetailData current;

    public VoteFrame(Session session, int pollId) {
        super("投票详情 - #" + pollId);
        this.session = session;
        this.api = new ApiClient(session);
        this.pollId = pollId;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(720, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(buildTop(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        loadDetail();
    }

    private JComponent buildTop() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        top.add(titleLabel, BorderLayout.CENTER);
        return top;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // options
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("请选择一个选项"));

        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        JScrollPane optSp = new JScrollPane(optionsPanel);
        left.add(optSp, BorderLayout.CENTER);

        // results
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("当前结果"));
        resultTable.setRowHeight(24);
        right.add(new JScrollPane(resultTable), BorderLayout.CENTER);

        center.add(left);
        center.add(right);
        return center;
    }

    private JComponent buildBottom() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JButton refresh = new JButton("刷新结果");
        JButton voteBtn = new JButton("投票/改票");
        JButton deleteBtn = new JButton("撤销投票");

        bottom.add(refresh);
        bottom.add(deleteBtn);
        bottom.add(voteBtn);

        refresh.addActionListener(e -> loadDetail());
        voteBtn.addActionListener(e -> doVote());
        deleteBtn.addActionListener(e -> doDelete());

        return bottom;
    }

    private void loadDetail() {
        SwingAsync.run(this,
                () -> api.pollDetail(pollId),
                (ApiResponse<PollDetailData> resp) -> {
                    if (!resp.success || resp.data == null) {
                        JOptionPane.showMessageDialog(this, resp.message == null ? "加载失败" : resp.message);
                        return;
                    }
                    current = resp.data;
                    renderDetail(resp.data);
                },
                ex -> JOptionPane.showMessageDialog(this, "请求失败：" + ex.getMessage()));
    }

    private void renderDetail(PollDetailData d) {
        titleLabel.setText("#" + d.id + "  " + d.title + (d.open ? "  (进行中)" : "  (已关闭)"));

        optionsPanel.removeAll();
        group.clearSelection();

        for (int i = 0; i < d.options.size(); i++) {
            String text = d.options.get(i);
            JRadioButton rb = new JRadioButton(text);
            rb.setActionCommand(String.valueOf(i));
            rb.setAlignmentX(Component.LEFT_ALIGNMENT);
            group.add(rb);
            optionsPanel.add(rb);

            if (d.yourVoteIndex != null && d.yourVoteIndex == i) {
                rb.setSelected(true);
            }
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();

        fillResultTable(d.options, d.counts);
    }

    private void fillResultTable(List<String> options, List<Long> counts) {
        tableModel.setRowCount(0);
        for (int i = 0; i < options.size(); i++) {
            long c = (counts != null && i < counts.size()) ? counts.get(i) : 0;
            tableModel.addRow(new Object[]{options.get(i), c});
        }
    }

    private Integer getSelectedIndex() {
        ButtonModel sel = group.getSelection();
        if (sel == null) return null;
        return Integer.parseInt(sel.getActionCommand());
    }

    private void doVote() {
        if (current == null) return;
        if (!current.open) {
            JOptionPane.showMessageDialog(this, "该投票已关闭，无法投票/改票");
            return;
        }
        Integer idx = getSelectedIndex();
        if (idx == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个选项");
            return;
        }

        SwingAsync.run(this,
                () -> api.vote(pollId, idx),
                (ApiResponse<VoteResultData> resp) -> {
                    if (!resp.success || resp.data == null) {
                        JOptionPane.showMessageDialog(this, resp.message == null ? "投票失败" : resp.message);
                        return;
                    }
                    // 投票成功后刷新详情（更新 yourVoteIndex 与结果）
                    loadDetail();
                },
                ex -> JOptionPane.showMessageDialog(this, "请求失败：" + ex.getMessage()));
    }

    private void doDelete() {
        if (current == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "确定撤销本投票的投票记录？", "确认", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        SwingAsync.run(this,
                () -> api.deleteVote(pollId),
                (ApiResponse<VoteResultData> resp) -> {
                    if (!resp.success || resp.data == null) {
                        JOptionPane.showMessageDialog(this, resp.message == null ? "撤销失败" : resp.message);
                        return;
                    }
                    loadDetail();
                },
                ex -> JOptionPane.showMessageDialog(this, "请求失败：" + ex.getMessage()));
    }
}
