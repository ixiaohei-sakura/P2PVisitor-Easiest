package com.cn.ixiaohei;

import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ProcessManager extends Thread {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, Runnable> events = new HashMap<>();
    private final Map<String, Process> processMap = new HashMap<>();
    private final Console consoleThread = new Console();
    private final JSONObject servers;

    public ProcessManager(JSONObject servers) {
        super();
        this.setDaemon(true);
        this.setName("Process Manager Thread");
        this.servers = servers;
    }

    public void addConsoleEventListener(String event, Runnable runnable) {
        this.events.put(event, runnable);
    }

    @Override
    public void run() {
        LOGGER.info("进程管理器启动");
        LOGGER.info("在列表中的有以下服务器:");
        for (String name : servers.keySet()) {
            LOGGER.info("   {}", name);
        }
        LOGGER.info("创建守护线程");
        this.creatDaemonThreads();
        if (OSCheck.isWindows()) {
            LOGGER.info("系统: Windows x64");
        } else if (OSCheck.isLinux()) {
            LOGGER.info("系统: Linux amd64");
        } else if (OSCheck.isMacOS()) {
            LOGGER.info("系统: MacOs darwin 64");
        }
        this.startDaemonThreads();
        LOGGER.info("全部进程已启动");
        this.consoleThread.start();
        while (!this.isInterrupted()) {
            try {
                TimeUnit.MICROSECONDS.sleep(200);
            } catch (InterruptedException ignored) {

            }
            if (!alive()) {
                System.gc();
                LOGGER.warn("线程列表内没有运行的线程, 线程管理器退出");
                LOGGER.info("等待其余线程停止");
                endDaemonThreads();
                while (alive()){
                    if (!alive()) {
                        break;
                    }
                }
                this.consoleThread.interrupt();
                this.interrupt();
            }
        }
        LOGGER.info("{} 退出", this.getName());
    }

    private boolean alive() {
        boolean alive = false;
        for (Map.Entry<String, Process> thread : processMap.entrySet()) {
            alive |= thread.getValue().isAlive();
        }
        return alive;
    }

    private void listAllDaemons() {
        LOGGER.info("有以下客户端:");
        for (Map.Entry<String, Process> thread : processMap.entrySet()) {
            LOGGER.info("   {}. 名称: {}", thread.getValue().id, thread.getValue().name);
        }
    }

    private void creatDaemonThreads() {
        int count = 0;
        for (Map.Entry<String, Object> server : servers.entrySet()) {
            count ++;
            this.processMap.put(server.getKey(), new Process(count, server.getKey(), servers.getJSONObject(server.getKey()), this));
        }
    }

    private void startDaemonThreads() {
        for (Map.Entry<String, Process> thread : processMap.entrySet()) {
            thread.getValue().start();
        }
    }

    private void endDaemonThreads() {
        for (Map.Entry<String, Process> thread : processMap.entrySet()) {
            try {
                thread.getValue().endDaemon();
            } catch (Exception e) {
                LOGGER.warn("线程停止失败: ", e);
            }
        }
    }

    private void restartDaemonThreads() {
        for (Map.Entry<String, Process> thread : processMap.entrySet()) {
            thread.getValue().restartProcess();
        }
    }

    private class Console extends Thread {
        private final Scanner scanner = new Scanner(System.in);

        public Console() {
            super();
            this.setDaemon(true);
            this.setName("Console Thread");
        }

        private boolean parser(String command) {
            boolean flag = false;
            for (Map.Entry<String, Runnable> event : events.entrySet()) {
                if (command.contains(event.getKey())) {
                    flag = true;
                    event.getValue().run();
                }
            }
            return flag;
        }

        private void printHelp() {
            LOGGER.info("==============可用的命令==============");
            LOGGER.info("   list: 列出");
            LOGGER.info("   stop_all: 停止所有");
            LOGGER.info("   restart_all: 重启所有");
            LOGGER.info("   #[id]stop/restart: 作用于指定进程");
            LOGGER.info("=====================================");
        }

        @Override
        public void run() {
            LOGGER.info("{} 启动", this.getName());
            while (scanner.hasNext()) {
                String s = scanner.nextLine();
                if (s.contains("stop_all")) {
                    LOGGER.info("收到停止命令");
                    LOGGER.info("等待其余线程停止");
                    endDaemonThreads();
                    while (alive()){
                        if (!alive()) {
                            break;
                        }
                    }
                    LOGGER.info("进程管理器退出");
                    LOGGER.info("等待主线程结束");
                    ProcessManager.this.interrupt();
                    return;
                } else if (s.contains("restart_all")) {
                    LOGGER.info("收到重启命令");
                    LOGGER.info("等待停止");
                    restartDaemonThreads();
                } else if (s.contains("list")) {
                    LOGGER.info("列出客户端");
                    listAllDaemons();
                } else if (s.contains("help")) {
                    this.printHelp();
                } else {
                    if (!parser(s)) {
                        LOGGER.warn("没有此命令");
                        this.printHelp();
                    }
                }
            }
        }
    }
}

