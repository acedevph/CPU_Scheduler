# ⚙️ CPU Scheduling Simulator

A single-file Java Swing desktop application that simulates five classic CPU scheduling algorithms with a live Gantt chart, results table, and performance statistics.

---

## 📋 Description

The **CPU Scheduling Simulator** is an educational tool built with Java Swing that visually demonstrates how an operating system's CPU scheduler decides the order in which processes are executed. Users can define their own set of processes (with custom arrival times, burst times, and priorities), choose from five scheduling algorithms, and instantly see the results rendered as a color-coded Gantt chart alongside a detailed performance metrics table.

Designed for students and educators studying operating systems concepts, the simulator makes abstract scheduling theory concrete and interactive — no additional dependencies, no setup beyond a standard JDK.

---

## ✨ Features

- **5 Scheduling Algorithms** — FCFS, SJF, SRTF, Priority, and Round Robin
- **Live Gantt Chart** — color-coded per process with gradient blocks, idle slots, and a time axis
- **Results Table** — per-process Completion Time, Waiting Time, and Turnaround Time
- **Statistics Panel** — Average Waiting Time, Average Turnaround Time, CPU Utilization %
- **Dynamic Process Management** — add, remove, and clear processes on the fly
- **Configurable Time Quantum** — adjustable spinner shown only for Round Robin
- **Dark Theme UI** — modern dark interface with 10 distinct process colors
- **Zero Dependencies** — single `.java` file, runs on any standard JDK 8+

---

## 🚀 Getting Started

### Prerequisites

- Java Development Kit (JDK) 8 or later
- Any terminal / command prompt

### Compile

```bash
javac CPUScheduler.java
```

### Run

```bash
java CPUScheduler
```

---

## 🖥️ How to Use

1. **Add Processes** — Click `+ Add` to insert a new row. Each process has:
   - **PID** — auto-assigned, read-only
   - **Arrival** — time the process arrives in the ready queue
   - **Burst** — total CPU time required
   - **Priority** — lower number = higher priority (used by Priority algorithm)

2. **Select an Algorithm** — Choose from the dropdown in the Algorithm panel.

3. **Set Time Quantum** *(Round Robin only)* — A spinner appears when RR is selected.

4. **Run** — Click **▶ RUN SIMULATION** to execute.

5. **Read the Results** — The Gantt chart and table update immediately.

---

## 📐 Scheduling Algorithms

### 1. First Come First Serve (FCFS)
Processes are executed in the order they arrive. Non-preemptive. The simplest algorithm — fair but can lead to the **convoy effect** where short processes wait behind a long one.

| Property | Value |
|----------|-------|
| Type | Non-preemptive |
| Selection | Earliest arrival time |
| Overhead | Minimal |
| Starvation | None |

---

### 2. Shortest Job First (SJF)
Among all processes that have arrived, the one with the **shortest burst time** runs next. Non-preemptive — once a process starts, it runs to completion. Minimizes average waiting time but requires knowing burst times in advance.

| Property | Value |
|----------|-------|
| Type | Non-preemptive |
| Selection | Shortest burst time |
| Overhead | Low |
| Starvation | Possible (long processes may wait indefinitely) |

---

### 3. Shortest Remaining Time First (SRTF)
The preemptive version of SJF. At every clock tick, the scheduler checks if any newly arrived process has a shorter remaining time than the currently running one. If so, it preempts immediately. Optimal average waiting time but high context-switch overhead.

| Property | Value |
|----------|-------|
| Type | Preemptive |
| Selection | Shortest remaining burst time |
| Overhead | High (potential preemption every tick) |
| Starvation | Possible |

---

### 4. Priority Scheduling
Each process is assigned a numeric priority. The process with the **lowest priority number** (highest urgency) runs next. Non-preemptive — ties are broken by arrival time. Can suffer from **starvation** of low-priority processes.

| Property | Value |
|----------|-------|
| Type | Non-preemptive |
| Selection | Lowest priority number |
| Overhead | Low |
| Starvation | Possible (aging not implemented) |

---

### 5. Round Robin (RR)
Each process gets a fixed slice of CPU time called the **time quantum**. If a process doesn't finish within its quantum, it is preempted and moved to the back of the ready queue. Newly arrived processes join the queue after the current quantum ends. Fair to all processes and widely used in real operating systems.

| Property | Value |
|----------|-------|
| Type | Preemptive |
| Selection | Cyclic, fixed time quantum |
| Overhead | Moderate (depends on quantum size) |
| Starvation | None |

> **Tip:** A very small quantum approaches SRTF behavior; a very large quantum approaches FCFS.

---

## 📊 Output Explained

### Gantt Chart
A horizontal timeline where each colored block represents a process occupying the CPU. Dark/gray blocks are **idle** periods. The bottom axis shows absolute clock time.

### Results Table

| Column | Description |
|--------|-------------|
| **Process** | Process identifier (P1, P2, …) |
| **Arrival** | Time the process entered the ready queue |
| **Burst** | Total CPU time required |
| **Priority** | Priority value (lower = higher urgency) |
| **Completion Time** | Clock time when the process finished |
| **Waiting Time** | Time spent waiting in the ready queue = Turnaround − Burst |
| **Turnaround Time** | Total time from arrival to completion = Completion − Arrival |

### Statistics
- **Avg Waiting Time** — mean waiting time across all processes
- **Avg Turnaround Time** — mean turnaround time across all processes
- **CPU Utilization** — percentage of total time the CPU was busy (not idle)

---

## 🗂️ Project Structure

```
CPUScheduler.java          ← entire application (single file)
README.md                  ← this file
```

### Key Classes (all nested inside `CPUScheduler`)

| Class | Role |
|-------|------|
| `Process` | Data model: stores PID, arrival, burst, priority, and computed metrics |
| `GanttBlock` | Represents one colored segment on the Gantt chart (process ID + start/end time) |
| `GanttChartPanel` | Custom `JPanel` that paints the Gantt chart using Java 2D |
| `CPUScheduler` | Main `JFrame`; hosts the UI and all five scheduling algorithm methods |

---

## 🧠 Algorithm Complexity

| Algorithm | Time Complexity | Space |
|-----------|----------------|-------|
| FCFS | O(n log n) | O(n) |
| SJF | O(n²) | O(n) |
| SRTF | O(n · T) where T = total time | O(n) |
| Priority | O(n²) | O(n) |
| Round Robin | O(n · T/q) | O(n) |

---

## 📌 Limitations & Notes

- **Burst times must be known in advance** — this matches the theoretical model used in OS coursework. Real schedulers use prediction heuristics.
- **Priority Scheduling** in this simulator is **non-preemptive**. Preemptive priority (where a higher-priority arrival immediately preempts the CPU) is not implemented.
- **Starvation prevention** (aging) is not implemented for SJF, SRTF, or Priority algorithms.
- All time values are treated as **integers** (milliseconds or abstract units).

---

## Author
**acedevph**

---

## 📄 License

This project is released for educational use. Feel free to modify and redistribute with attribution.

---

*Built with Java Swing · No external dependencies · JDK 8+ compatible*
