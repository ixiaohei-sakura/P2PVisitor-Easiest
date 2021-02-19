package com.cn.ixiaohei;

import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public class Process {
    private static final Logger LOGGER = LogManager.getLogger();
    private final P2PIniProperties p2PIniProperties;
    private final File pidTmpFile;
    private final JSONObject properties;
    public LinkedList<java.lang.Process> historyList = new LinkedList<>();
    public java.lang.Process process;
    public DaemonThread daemonThread;
    public String name;
    public int id;

    public Process(int id, String name, JSONObject properties, ProcessManager parent) {
        this.id = id;
        this.name = name;
        this.properties = properties;
        this.p2PIniProperties = new P2PIniProperties(properties, name, id);
        this.pidTmpFile = Utils.getTmpFile(String.format("pid/%s%d.pid.tmp", name, id));
        parent.addConsoleEventListener(String.format("#%dstop", id), this::endDaemon);
        parent.addConsoleEventListener(String.format("#%drestart", id), this::restartProcess);
    }

    public void start() {
        StringBuilder cmd = new StringBuilder();
        try {
            if (OSCheck.isWindows()) {
                Utils.copy("Windows/frpc.exe");
                cmd.append(URLDecoder.decode(Utils.getTmpFile("Windows/frpc.exe").getPath(), StandardCharsets.UTF_8));
                cmd.append(" -c ");
                cmd.append(p2PIniProperties.filePath);
            } else if (OSCheck.isLinux()) {
                Utils.copy("LinuxAmd64/frpc");
                cmd.append(URLDecoder.decode(Utils.getTmpFile("LinuxAmd64/frpc").getPath(), StandardCharsets.UTF_8));
                cmd.append(" -c ");
                cmd.append(p2PIniProperties.filePath);
            } else if (OSCheck.isMacOS()) {
                Utils.copy("MacOS/frpc");
                cmd.append(URLDecoder.decode(Utils.getTmpFile("MacOS/frpc").getPath(), StandardCharsets.UTF_8));
                cmd.append(" -c ");
                cmd.append(p2PIniProperties.filePath);
            }
        } catch (Exception e) {
            LOGGER.trace("复制文件时失败: ", e);
        }
        LOGGER.info("启动命令: {}", cmd.toString());
        this.daemonThread = new DaemonThread(cmd.toString(), this.id);
        this.daemonThread.start();
    }

    public synchronized void restartProcess() {this.daemonThread.restartProcess(); }

    public synchronized void endDaemon() {
        this.daemonThread.interrupt();
    }

    public boolean isAlive() { return this.daemonThread.isAlive(); }

    private class DaemonThread extends Thread {
        public String cmd;
        private boolean interrupted = false;

        public DaemonThread(String cmd, int id) {
            this.setDaemon(true);
            this.setName("P2PDaemon Thread #" + id);
            this.cmd = cmd;
        }

        public synchronized void interrupt() {
            this.interrupted = true;
            p2PIniProperties.remove();
            try {
                if (OSCheck.isWindows()) {
                    Utils.delete("tmp/Windows/frpc.exe");
                } else if (OSCheck.isLinux()) {
                    Utils.delete("tmp/LinuxAmd64/frpc");
                } else if (OSCheck.isMacOS()) {
                    Utils.delete("tmp/MacOS/frpc");
                }
            } catch (Exception e) {
                LOGGER.trace("错误: ", e);
            }
            LOGGER.info("等待P2P进程停止");
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException ignored) {
            }
            LOGGER.info("P2P进程守护线程停止");
        }

        @Override
        public void run() {
            LOGGER.info("守护线程 {} #{} 启动", name, id);
            while (!interrupted) {
                this.startProcess();
                BufferedReader read = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while(!this.isInterrupted()){
                    try {
                        if ((line = read.readLine()) == null) break;
                    } catch (IOException e) {
                        LOGGER.error("进程运行时错误, 线程退出", e);
                    }
                    LOGGER.info(line);
                }
            }
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                LOGGER.error("进程运行时错误", e);
            }
        }

        public synchronized void restartProcess() {
            try {
                process.destroy();
                process.waitFor();
            } catch (InterruptedException ignored) {

            }
        }

        private void startProcess() {
            try {
                p2PIniProperties.remove();
                FileWriter pidTmpW = new FileWriter(pidTmpFile);
                BufferedReader fr = new BufferedReader(new FileReader(pidTmpFile));
                LOGGER.info("P2P进程正在启动, 等待旧进程停止");
                try {
                    process.destroy();
                    process.waitFor();
                } catch (NullPointerException | InterruptedException ignored) {

                }
                if (OSCheck.isWindows()) {
                    Runtime.getRuntime().exec("taskkill -pid " + fr.readLine());
                } else {
                    Runtime.getRuntime().exec("kill " + fr.readLine());
                }
                p2PIniProperties.write();
                process = Runtime.getRuntime().exec(cmd);
                historyList.add(process);
                LOGGER.info("进程启动, PID: {} | 名称: {} | 本地IP: {} | 本地端口: {}", process.pid(), name,
                        properties.getString("laddr"),
                        properties.getString("lport"));
                pidTmpW.write(String.valueOf(process.pid()));
                pidTmpW.flush();
                pidTmpW.close();
                BufferedReader read = new BufferedReader(new InputStreamReader(process.getInputStream()));
                LOGGER.info(read.readLine());
            } catch (IOException e) {
                LOGGER.error("进程启动错误: ", e);
            }
        }
    }
}
