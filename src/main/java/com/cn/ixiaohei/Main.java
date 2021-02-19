package com.cn.ixiaohei;

import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) throws IOException {
        if (!Arrays.toString(args).contains("INCONSOLABLE")) {
            if (OSCheck.isWindows()) {
                File bf = new File(Constants.JARRootPath + "start.bat");
                FileWriter fw = new FileWriter(bf);
                fw.write("@echo off\r\njava -jar \"" + Constants.JARPath + "\" -INCONSOLABLE\r\npause");
                fw.flush();
                fw.close();
            } else {
                File bf = new File(Constants.JARRootPath + "start.sh");
                FileWriter fw = new FileWriter(bf);
                fw.write("cd \"" + Constants.JARRootPath +"\" && java -jar \"" + Constants.JARPath + "\" -INCONSOLABLE && sleep 3600000");
                fw.flush();
                fw.close();
            }
            Utils.deleteAll();
            throw new FileNotFoundException("启动文件start.bat / start.sh 已经自动生成，请使用它来启动");                    }

        if (OSCheck.isWindows()) {
            LOGGER.info("正在复制文件 | 如果出现错误，请重新运行");
            Utils.copy("Windows/ansicon.exe");
            Utils.copy("Windows/ANSI32.dll");
            Utils.copy("Windows/ANSI64.dll");
            LOGGER.info("等待复制完成");
            try {
                Runtime.getRuntime().exec(URLDecoder.decode(String.format("%s -i", Utils.getTmpFile("Windows/ansicon.exe").getPath()), StandardCharsets.UTF_8));
            } catch (Exception ignored) {
                LOGGER.info("文件已复制, 请重新运行");
                System.exit(0);
            }
        }

        Config config = new Config();
        MetaData metaData = new MetaData();
        JSONObject p2p_data = metaData.getMetaData(config);
        JSONObject servers = p2p_data.getJSONObject("data");

        if (p2p_data.getInteger("status") != 2) {
            LOGGER.fatal("服务器返回值状态错误");
            System.exit(0);
        } else if (servers.isEmpty()) {
            LOGGER.warn("服务器没有发送的清单中没有任何链接");
            LOGGER.warn("这条消息不是错误，如果您有疑问，可以询问管理员");
            LOGGER.info("程序即将退出。");
            System.exit(0);
        } else if (servers.toJavaObject(Map.class).size() == 1) {
            LOGGER.info("检测到唯一一个节点，程序将直接连接");
            startProcessManager(servers);
        } else {
            int i = 0;
            Map<Integer, String> keyD = new HashMap<>();
            LOGGER.info("==============有以下P2P服务器==============");
            for (String serverName : servers.keySet()) {
                i++;
                keyD.put(i, serverName);
                LOGGER.info(i + ". " + serverName);
            }
            LOGGER.info("=========================================");
            LOGGER.info("请输入希望连接的服务器编号, 多个服务器请使用空格分开");
            System.out.print("> ");
            Scanner scanner = new Scanner(System.in);
            String[] serverSerials = scanner.nextLine().split(" ");
            JSONObject tmp = new JSONObject();
            for (String n : serverSerials) {
                tmp.put(keyD.get(Integer.parseInt(n)), servers.getJSONObject(keyD.get(Integer.parseInt(n))));
            }
            startProcessManager(tmp);
        }
        System.gc();
        Utils.deleteAll();
        LOGGER.info("Main Thread 退出");
        System.exit(0);
    }

    private static void startProcessManager(JSONObject servers) {
        ProcessManager processManager = new ProcessManager(servers);
        processManager.start();
        while (!processManager.isInterrupted()){
            if (!processManager.isAlive()) {
                break;
            }
        }
    }
}
