import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Queue;

public class CPUScheduler extends JFrame {

    // ─────────────── Data Models ───────────────
    static class Process {
        int id, arrival, burst, priority;
        int remaining, completion, waiting, turnaround;

        Process(int id, int arrival, int burst, int priority) {
            this.id = id; this.arrival = arrival;
            this.burst = burst; this.priority = priority;
            this.remaining = burst;
        }

        Process copy() {
            return new Process(id, arrival, burst, priority);
        }
    }

    static class GanttBlock {
        int pid; // -1 = idle
        int start, end;
        GanttBlock(int pid, int start, int end) {
            this.pid = pid; this.start = start; this.end = end;
        }
    }

    // ─────────────── Constants ───────────────
    static final Color BG         = new Color(13, 17, 30);
    static final Color PANEL_BG   = new Color(22, 28, 48);
    static final Color CARD_BG    = new Color(30, 38, 62);
    static final Color BORDER_CLR = new Color(50, 65, 100);
    static final Color TEXT_PRI   = new Color(230, 235, 255);
    static final Color TEXT_SEC   = new Color(130, 145, 185);
    static final Color ACCENT     = new Color(80, 140, 255);
    static final Color SUCCESS    = new Color(50, 200, 130);
    static final Color DANGER     = new Color(220, 70, 80);

    static final Color[] PROC_COLORS = {
        new Color(80,  140, 255), new Color(50,  200, 130),
        new Color(255, 160,  60), new Color(200,  80, 200),
        new Color(70,  210, 220), new Color(255, 100, 100),
        new Color(180, 230,  80), new Color(255, 200,  60),
        new Color(130, 100, 255), new Color(255, 130, 180)
    };

    // ─────────────── UI Components ───────────────
    private DefaultTableModel inputModel;
    private DefaultTableModel resultModel;
    private GanttChartPanel   ganttPanel;
    private JComboBox<String> algoBox;
    private JSpinner          quantumSpinner;
    private JPanel            quantumRow;
    private JLabel            avgWTLabel, avgTATLabel, cpuLabel;
    private JLabel            statusLabel;

    // ══════════════════════════════════════════════
    //  ENTRY POINT
    // ══════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(CPUScheduler::new);
    }

    public CPUScheduler() {
        super("CPU Scheduling Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1000, 700));
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),     BorderLayout.NORTH);
        add(buildMainArea(),   BorderLayout.CENTER);
        add(buildStatusBar(),  BorderLayout.SOUTH);

        loadDefaults();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ══════════════════════════════════════════════
    //  HEADER
    // ══════════════════════════════════════════════
    JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(PANEL_BG);
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CLR));

        JLabel title = new JLabel("  ⚙  CPU Scheduling Simulator");
        title.setFont(new Font("Monospaced", Font.BOLD, 18));
        title.setForeground(TEXT_PRI);
        title.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 0));

        JLabel sub = new JLabel("FCFS · SJF · SRTF · Priority · Round Robin  ");
        sub.setFont(new Font("Monospaced", Font.PLAIN, 12));
        sub.setForeground(TEXT_SEC);
        sub.setHorizontalAlignment(SwingConstants.RIGHT);

        hdr.add(title, BorderLayout.WEST);
        hdr.add(sub,   BorderLayout.EAST);
        return hdr;
    }

    // ══════════════════════════════════════════════
    //  MAIN LAYOUT
    // ══════════════════════════════════════════════
    JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBackground(BG);
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // LEFT: controls
        main.add(buildControlPanel(), BorderLayout.WEST);

        // CENTER: output (gantt + table)
        main.add(buildOutputArea(), BorderLayout.CENTER);
        return main;
    }

    // ══════════════════════════════════════════════
    //  LEFT CONTROL PANEL
    // ══════════════════════════════════════════════
    JPanel buildControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(360, 0));
        panel.setBackground(BG);

        panel.add(buildInputTable(),  BorderLayout.CENTER);
        panel.add(buildAlgoSection(), BorderLayout.SOUTH);
        return panel;
    }

    JPanel buildInputTable() {
        JPanel card = createCard("Process Table");

        String[] cols = {"PID", "Arrival", "Burst", "Priority"};
        inputModel = new DefaultTableModel(cols, 0) {
            public Class<?> getColumnClass(int c) { return Integer.class; }
            public boolean isCellEditable(int r, int c) { return c != 0; }
        };

        JTable table = new JTable(inputModel);
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(65);
        table.getColumnModel().getColumn(2).setPreferredWidth(55);
        table.getColumnModel().getColumn(3).setPreferredWidth(65);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(CARD_BG);
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_CLR));
        card.add(scroll, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        btnRow.setBackground(CARD_BG);

        JButton add    = mkBtn("+ Add",   SUCCESS);
        JButton remove = mkBtn("− Remove", DANGER);
        JButton clear  = mkBtn("Clear",   new Color(80,80,120));

        add.addActionListener(e -> {
            int pid = inputModel.getRowCount() + 1;
            inputModel.addRow(new Object[]{pid, 0, new Random().nextInt(8)+1, 1});
            setStatus("Process P" + pid + " added.");
        });
        remove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) { inputModel.removeRow(row); renumberPIDs(); setStatus("Process removed."); }
        });
        clear.addActionListener(e -> { inputModel.setRowCount(0); setStatus("All processes cleared."); });

        btnRow.add(add); btnRow.add(remove); btnRow.add(clear);
        card.add(btnRow, BorderLayout.SOUTH);
        return card;
    }

    JPanel buildAlgoSection() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.setBackground(BG);

        // Algorithm selector card
        JPanel algoCard = createCard("Algorithm");
        algoCard.setLayout(new BoxLayout(algoCard, BoxLayout.Y_AXIS));

        String[] algos = {
            "1. FCFS – First Come First Serve",
            "2. SJF – Shortest Job First",
            "3. SRTF – Shortest Remaining Time",
            "4. Priority Scheduling",
            "5. Round Robin (RR)"
        };
        algoBox = new JComboBox<>(algos);
        algoBox.setFont(new Font("Monospaced", Font.PLAIN, 12));
        algoBox.setBackground(PANEL_BG);
        algoBox.setForeground(TEXT_PRI);
        algoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        algoBox.addActionListener(e -> quantumRow.setVisible(algoBox.getSelectedIndex() == 4));

        // Quantum row
        quantumRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        quantumRow.setBackground(CARD_BG);
        JLabel qLbl = new JLabel("Time Quantum:");
        qLbl.setForeground(TEXT_SEC);
        qLbl.setFont(new Font("Monospaced", Font.PLAIN, 12));
        quantumSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 999, 1));
        styleSpinner(quantumSpinner);
        quantumRow.add(qLbl); quantumRow.add(quantumSpinner);
        quantumRow.setVisible(false);

        algoCard.add(Box.createVerticalStrut(4));
        algoCard.add(algoBox);
        algoCard.add(Box.createVerticalStrut(4));
        algoCard.add(quantumRow);
        algoCard.add(Box.createVerticalStrut(4));

        // Run button
        JButton runBtn = new JButton("▶  RUN SIMULATION");
        runBtn.setFont(new Font("Monospaced", Font.BOLD, 14));
        runBtn.setBackground(ACCENT);
        runBtn.setForeground(Color.WHITE);
        runBtn.setFocusPainted(false);
        runBtn.setBorderPainted(false);
        runBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        runBtn.setPreferredSize(new Dimension(0, 46));
        runBtn.addActionListener(e -> runSimulation());
        runBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { runBtn.setBackground(new Color(60,120,240)); }
            public void mouseExited (MouseEvent e) { runBtn.setBackground(ACCENT); }
        });

        // Stats card
        JPanel statsCard = createCard("Statistics");
        statsCard.setLayout(new GridLayout(3, 2, 4, 4));
        statsCard.add(lbl("Avg Waiting Time:", TEXT_SEC));
        avgWTLabel  = lbl("—", TEXT_PRI); statsCard.add(avgWTLabel);
        statsCard.add(lbl("Avg Turnaround Time:", TEXT_SEC));
        avgTATLabel = lbl("—", TEXT_PRI); statsCard.add(avgTATLabel);
        statsCard.add(lbl("CPU Utilization:", TEXT_SEC));
        cpuLabel    = lbl("—", TEXT_PRI); statsCard.add(cpuLabel);

        wrapper.add(algoCard,  BorderLayout.NORTH);
        wrapper.add(runBtn,    BorderLayout.CENTER);
        wrapper.add(statsCard, BorderLayout.SOUTH);
        return wrapper;
    }

    // ══════════════════════════════════════════════
    //  RIGHT OUTPUT AREA
    // ══════════════════════════════════════════════
    JPanel buildOutputArea() {
        JPanel area = new JPanel(new BorderLayout(0, 8));
        area.setBackground(BG);

        // Gantt Chart
        JPanel ganttCard = createCard("Gantt Chart");
        ganttPanel = new GanttChartPanel();
        ganttPanel.setPreferredSize(new Dimension(0, 120));
        JScrollPane ganttScroll = new JScrollPane(ganttPanel,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ganttScroll.setBackground(CARD_BG);
        ganttScroll.getViewport().setBackground(CARD_BG);
        ganttScroll.setBorder(null);
        ganttCard.add(ganttScroll, BorderLayout.CENTER);

        // Result Table
        JPanel tableCard = createCard("Scheduling Results");
        String[] cols = {"Process","Arrival","Burst","Priority","Completion","Waiting","Turnaround"};
        resultModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable resultTable = new JTable(resultModel);
        styleTable(resultTable);

        // Color first column by process
        resultTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(CARD_BG);
                setForeground(PROC_COLORS[row % PROC_COLORS.length]);
                setFont(new Font("Monospaced", Font.BOLD, 13));
                setHorizontalAlignment(CENTER);
                return this;
            }
        });

        JScrollPane resultScroll = new JScrollPane(resultTable);
        resultScroll.setBackground(CARD_BG);
        resultScroll.getViewport().setBackground(CARD_BG);
        resultScroll.setBorder(BorderFactory.createLineBorder(BORDER_CLR));
        tableCard.add(resultScroll, BorderLayout.CENTER);

        // Split gantt top, table bottom
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ganttCard, tableCard);
        split.setDividerLocation(195);
        split.setDividerSize(6);
        split.setBackground(BG);
        split.setBorder(null);

        area.add(split, BorderLayout.CENTER);
        return area;
    }

    JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(10, 14, 24));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_CLR));
        statusLabel = new JLabel("  Ready — add processes and run a simulation.");
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_SEC);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    // ══════════════════════════════════════════════
    //  SIMULATION ENGINE
    // ══════════════════════════════════════════════
    void runSimulation() {
        if (inputModel.getRowCount() == 0) {
            setStatus("⚠  No processes to schedule!");
            return;
        }

        // Stop cell editing
        if (getFocusOwner() instanceof JTable) {
            JTable t = (JTable) getFocusOwner();
            if (t.isEditing()) t.getCellEditor().stopCellEditing();
        }

        List<Process> procs = readProcesses();
        if (procs == null) return;

        List<GanttBlock> gantt;
        String algoName;
        int algo = algoBox.getSelectedIndex();

        switch (algo) {
            case 0: gantt = scheduleFCFS(procs);     algoName = "FCFS"; break;
            case 1: gantt = scheduleSJF(procs);      algoName = "SJF (Non-Preemptive)"; break;
            case 2: gantt = scheduleSTRF(procs);     algoName = "SRTF (Preemptive)"; break;
            case 3: gantt = schedulePriority(procs); algoName = "Priority (Non-Preemptive)"; break;
            default:
                int q = (Integer) quantumSpinner.getValue();
                gantt = scheduleRR(procs, q);
                algoName = "Round Robin (quantum=" + q + ")";
        }

        computeTimes(procs, gantt);
        updateGantt(gantt);
        updateResultTable(procs);
        updateStats(procs, gantt);
        setStatus("✔  Simulation complete — Algorithm: " + algoName);
    }

    List<Process> readProcesses() {
        List<Process> list = new ArrayList<>();
        for (int r = 0; r < inputModel.getRowCount(); r++) {
            try {
                int pid  = (Integer) inputModel.getValueAt(r, 0);
                int arr  = (Integer) inputModel.getValueAt(r, 1);
                int bst  = (Integer) inputModel.getValueAt(r, 2);
                int pri  = (Integer) inputModel.getValueAt(r, 3);
                if (bst <= 0) { setStatus("⚠  Burst time must be > 0 (row " + (r+1) + ")"); return null; }
                list.add(new Process(pid, arr, bst, pri));
            } catch (Exception e) {
                setStatus("⚠  Invalid data in row " + (r+1));
                return null;
            }
        }
        return list;
    }

    // ── FCFS ──────────────────────────────────────
    List<GanttBlock> scheduleFCFS(List<Process> procs) {
        List<GanttBlock> gantt = new ArrayList<>();
        List<Process> sorted = new ArrayList<>(procs);
        sorted.sort(Comparator.comparingInt(p -> p.arrival));

        int time = 0;
        for (Process p : sorted) {
            if (time < p.arrival) { gantt.add(new GanttBlock(-1, time, p.arrival)); time = p.arrival; }
            gantt.add(new GanttBlock(p.id, time, time + p.burst));
            time += p.burst;
        }
        return gantt;
    }

    // ── SJF (Non-Preemptive) ──────────────────────
    List<GanttBlock> scheduleSJF(List<Process> procs) {
        List<GanttBlock> gantt = new ArrayList<>();
        List<Process> remaining = new ArrayList<>(procs);
        remaining.sort(Comparator.comparingInt(p -> p.arrival));
        int time = 0;
        List<Process> ready = new ArrayList<>();

        while (!remaining.isEmpty() || !ready.isEmpty()) {
            // Admit arrived processes
            Iterator<Process> it = remaining.iterator();
            while (it.hasNext()) { Process p = it.next(); if (p.arrival <= time) { ready.add(p); it.remove(); } }

            if (ready.isEmpty()) {
                int next = remaining.get(0).arrival;
                gantt.add(new GanttBlock(-1, time, next)); time = next; continue;
            }
            // Pick shortest burst
            ready.sort(Comparator.comparingInt(p -> p.burst));
            Process p = ready.remove(0);
            if (time < p.arrival) time = p.arrival;
            gantt.add(new GanttBlock(p.id, time, time + p.burst));
            time += p.burst;
            // Re-admit newly arrived
            Iterator<Process> it2 = remaining.iterator();
            while (it2.hasNext()) { Process q = it2.next(); if (q.arrival <= time) { ready.add(q); it2.remove(); } }
        }
        return gantt;
    }

    // ── SRTF (Preemptive SJF) ─────────────────────
    List<GanttBlock> scheduleSTRF(List<Process> procs) {
        List<GanttBlock> gantt = new ArrayList<>();
        // Copy processes
        List<Process> all = new ArrayList<>();
        for (Process p : procs) all.add(p.copy());

        int n = all.size();
        int time = 0;
        int done = 0;
        int prev = -1;
        int blockStart = 0;

        int maxTime = all.stream().mapToInt(p -> p.arrival + p.burst).max().orElse(0) + 1;

        while (done < n && time <= maxTime) {
            // Find process with shortest remaining time that has arrived
            Process cur = null;
            for (Process p : all) {
                if (p.arrival <= time && p.remaining > 0) {
                    if (cur == null || p.remaining < cur.remaining ||
                       (p.remaining == cur.remaining && p.arrival < cur.arrival))
                        cur = p;
                }
            }

            int curId = (cur == null) ? -1 : cur.id;
            if (curId != prev) {
                if (prev != -1 || (prev == -1 && curId == -1))
                    gantt.add(new GanttBlock(prev, blockStart, time));
                blockStart = time; prev = curId;
            }

            if (cur != null) { cur.remaining--; if (cur.remaining == 0) done++; }
            time++;
        }
        if (prev != -1 || blockStart < time)
            gantt.add(new GanttBlock(prev, blockStart, time));

        // Merge consecutive same-process blocks
        return mergeBlocks(gantt);
    }

    // ── Priority (Non-Preemptive, lower number = higher priority) ──
    List<GanttBlock> schedulePriority(List<Process> procs) {
        List<GanttBlock> gantt = new ArrayList<>();
        List<Process> remaining = new ArrayList<>(procs);
        remaining.sort(Comparator.comparingInt(p -> p.arrival));
        int time = 0;
        List<Process> ready = new ArrayList<>();

        while (!remaining.isEmpty() || !ready.isEmpty()) {
            Iterator<Process> it = remaining.iterator();
            while (it.hasNext()) { Process p = it.next(); if (p.arrival <= time) { ready.add(p); it.remove(); } }

            if (ready.isEmpty()) {
                int next = remaining.get(0).arrival;
                gantt.add(new GanttBlock(-1, time, next)); time = next; continue;
            }
            // Lowest priority number = highest priority; tie-break by arrival
            ready.sort(Comparator.comparingInt((Process p) -> p.priority)
                              .thenComparingInt(p -> p.arrival));
            Process p = ready.remove(0);
            if (time < p.arrival) time = p.arrival;
            gantt.add(new GanttBlock(p.id, time, time + p.burst));
            time += p.burst;
            Iterator<Process> it2 = remaining.iterator();
            while (it2.hasNext()) { Process q = it2.next(); if (q.arrival <= time) { ready.add(q); it2.remove(); } }
        }
        return gantt;
    }

    // ── Round Robin ───────────────────────────────
    List<GanttBlock> scheduleRR(List<Process> procs, int quantum) {
        List<GanttBlock> gantt = new ArrayList<>();
        List<Process> all = new ArrayList<>();
        for (Process p : procs) all.add(p.copy());
        all.sort(Comparator.comparingInt(p -> p.arrival));

        Queue<Process> queue = new LinkedList<>();
        int time = 0, idx = 0;

        // Enqueue first batch
        while (idx < all.size() && all.get(idx).arrival <= time) queue.add(all.get(idx++));

        while (!queue.isEmpty() || idx < all.size()) {
            if (queue.isEmpty()) {
                int next = all.get(idx).arrival;
                gantt.add(new GanttBlock(-1, time, next));
                time = next;
                while (idx < all.size() && all.get(idx).arrival <= time) queue.add(all.get(idx++));
                continue;
            }
            Process p = queue.poll();
            int exec = Math.min(quantum, p.remaining);
            gantt.add(new GanttBlock(p.id, time, time + exec));
            time += exec;
            p.remaining -= exec;

            // Admit newly arrived processes
            while (idx < all.size() && all.get(idx).arrival <= time) queue.add(all.get(idx++));
            if (p.remaining > 0) queue.add(p);
        }
        return mergeBlocks(gantt);
    }

    // ── Utilities ─────────────────────────────────
    void computeTimes(List<Process> procs, List<GanttBlock> gantt) {
        // Completion time = last block end for each process
        Map<Integer, Integer> ct = new HashMap<>();
        for (GanttBlock b : gantt) {
            if (b.pid != -1) ct.put(b.pid, b.end);
        }
        for (Process p : procs) {
            p.completion  = ct.getOrDefault(p.id, 0);
            p.turnaround  = p.completion - p.arrival;
            p.waiting     = p.turnaround - p.burst;
        }
    }

    List<GanttBlock> mergeBlocks(List<GanttBlock> raw) {
        List<GanttBlock> merged = new ArrayList<>();
        for (GanttBlock b : raw) {
            if (b.start == b.end) continue;
            if (!merged.isEmpty()) {
                GanttBlock last = merged.get(merged.size()-1);
                if (last.pid == b.pid && last.end == b.start) { last.end = b.end; continue; }
            }
            merged.add(new GanttBlock(b.pid, b.start, b.end));
        }
        return merged;
    }

    void updateGantt(List<GanttBlock> gantt) {
        ganttPanel.setBlocks(gantt);
    }

    void updateResultTable(List<Process> procs) {
        resultModel.setRowCount(0);
        for (Process p : procs) {
            resultModel.addRow(new Object[]{
                "P" + p.id, p.arrival, p.burst, p.priority,
                p.completion, p.waiting, p.turnaround
            });
        }
    }

    void updateStats(List<Process> procs, List<GanttBlock> gantt) {
        double avgWT  = procs.stream().mapToInt(p -> p.waiting).average().orElse(0);
        double avgTAT = procs.stream().mapToInt(p -> p.turnaround).average().orElse(0);
        int totalTime = gantt.isEmpty() ? 0 : gantt.get(gantt.size()-1).end;
        int busyTime  = gantt.stream().filter(b -> b.pid != -1).mapToInt(b -> b.end - b.start).sum();
        double cpu    = totalTime > 0 ? (busyTime * 100.0 / totalTime) : 0;

        avgWTLabel.setText(String.format("%.2f ms", avgWT));
        avgTATLabel.setText(String.format("%.2f ms", avgTAT));
        cpuLabel.setText(String.format("%.1f%%", cpu));
    }

    void renumberPIDs() {
        for (int r = 0; r < inputModel.getRowCount(); r++)
            inputModel.setValueAt(r + 1, r, 0);
    }

    void setStatus(String msg) { statusLabel.setText("  " + msg); }

    void loadDefaults() {
        Object[][] defaults = {
            {1, 0, 6, 2}, {2, 1, 4, 1}, {3, 2, 8, 3},
            {4, 3, 3, 2}, {5, 4, 5, 1}
        };
        for (Object[] row : defaults) inputModel.addRow(row);
        setStatus("Default processes loaded. Select an algorithm and click Run.");
    }

    // ══════════════════════════════════════════════
    //  GANTT CHART PANEL
    // ══════════════════════════════════════════════
    class GanttChartPanel extends JPanel {
        private List<GanttBlock> blocks = new ArrayList<>();
        private static final int ROW_H    = 52;
        private static final int LABEL_H  = 20;
        private static final int PAD      = 14;
        private static final int MIN_W    = 30;

        GanttChartPanel() {
            setBackground(CARD_BG);
        }

        void setBlocks(List<GanttBlock> b) {
            this.blocks = b;
            int total = b.isEmpty() ? 0 : b.get(b.size()-1).end;
            int minWidth = Math.max(800, total * MIN_W + PAD * 2);
            setPreferredSize(new Dimension(minWidth, ROW_H + LABEL_H + PAD * 2));
            revalidate(); repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (blocks.isEmpty()) {
                g.setColor(TEXT_SEC);
                g.setFont(new Font("Monospaced", Font.PLAIN, 14));
                String msg = "Run a simulation to see the Gantt chart";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg, (getWidth()-fm.stringWidth(msg))/2, getHeight()/2+5);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int total = blocks.get(blocks.size()-1).end;
            double scale = (double)(getWidth() - PAD * 2) / Math.max(total, 1);
            int barY = PAD;

            for (GanttBlock b : blocks) {
                int x  = PAD + (int)(b.start * scale);
                int w  = Math.max(2, (int)((b.end - b.start) * scale));
                Color c = (b.pid == -1) ? new Color(40,40,60) : PROC_COLORS[(b.pid - 1) % PROC_COLORS.length];

                // Block fill with gradient
                GradientPaint gp = new GradientPaint(x, barY, c.brighter(), x, barY + ROW_H, c.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(x, barY, w, ROW_H, 6, 6);

                // Border
                g2.setColor(c.darker().darker());
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(x, barY, w, ROW_H, 6, 6);

                // Label
                if (w > 20) {
                    String label = (b.pid == -1) ? "IDLE" : "P" + b.pid;
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Monospaced", Font.BOLD, w > 40 ? 13 : 10));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = x + (w - fm.stringWidth(label)) / 2;
                    int ty = barY + ROW_H/2 + fm.getAscent()/2 - 2;
                    if (tx > x) g2.drawString(label, tx, ty);
                }
            }

            // Time axis
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.setColor(TEXT_SEC);
            Set<Integer> drawn = new HashSet<>();
            for (GanttBlock b : blocks) {
                for (int t : new int[]{b.start, b.end}) {
                    if (drawn.contains(t)) continue;
                    drawn.add(t);
                    int tx = PAD + (int)(t * scale);
                    g2.setColor(new Color(60,80,120));
                    g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0, new float[]{2,2}, 0));
                    g2.drawLine(tx, barY, tx, barY + ROW_H);
                    g2.setColor(TEXT_SEC);
                    g2.setStroke(new BasicStroke(1));
                    String ts = String.valueOf(t);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(ts, tx - fm.stringWidth(ts)/2, barY + ROW_H + 14);
                }
            }
        }
    }

    // ══════════════════════════════════════════════
    //  HELPERS / STYLING
    // ══════════════════════════════════════════════
    JPanel createCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(CARD_BG);
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_CLR, 1), " " + title + " ");
        border.setTitleColor(ACCENT);
        border.setTitleFont(new Font("Monospaced", Font.BOLD, 12));
        card.setBorder(border);
        return card;
    }

    void styleTable(JTable t) {
        t.setBackground(CARD_BG);
        t.setForeground(TEXT_PRI);
        t.setGridColor(BORDER_CLR);
        t.setFont(new Font("Monospaced", Font.PLAIN, 13));
        t.setRowHeight(30);
        t.setSelectionBackground(new Color(50, 90, 180));
        t.setSelectionForeground(Color.WHITE);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader hdr = t.getTableHeader();
        hdr.setBackground(new Color(18, 24, 42));
        hdr.setForeground(ACCENT);
        hdr.setFont(new Font("Monospaced", Font.BOLD, 12));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CLR));
        hdr.setReorderingAllowed(false);

        // Center all columns
        DefaultTableCellRenderer center = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(CENTER);
                if (!sel) { setBackground(r%2==0 ? CARD_BG : new Color(26, 33, 55)); setForeground(TEXT_PRI); }
                setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                return this;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setCellRenderer(center);
    }

    JButton mkBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Monospaced", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    void styleSpinner(JSpinner s) {
        s.setBackground(PANEL_BG);
        s.setForeground(TEXT_PRI);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setBackground(PANEL_BG);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setForeground(TEXT_PRI);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setFont(new Font("Monospaced", Font.PLAIN, 12));
        s.setPreferredSize(new Dimension(70, 26));
    }

    JLabel lbl(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 12));
        l.setForeground(color);
        l.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        return l;
    }
}