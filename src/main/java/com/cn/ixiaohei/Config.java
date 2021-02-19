package com.cn.ixiaohei;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Config {
    public static final Logger LOGGER = Constants.LOGGER;
    public JSONObject config = new JSONObject();
    private File configFile = new File(Constants.ConfigFilePath);
    private BufferedReader bufferedReader;
    private FileWriter fileWriter;
    private Map configMap = new HashMap<>();

    public Config() {
        this.loadFile();
        this.configMap = config.toJavaObject(Map.class);
    }

    public JSONObject getConfig() {
        return config;
    }

    public Map getConfigMap() {
        return configMap;
    }

    public Object get(Object key) {
        return config.get(key);
    }

    private void loadFile() {
        try {
            bufferedReader = new BufferedReader(new FileReader(configFile));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine())!=null) {
                stringBuilder.append(line);
            }
            this.config = JSON.parseObject(stringBuilder.toString());
        } catch (FileNotFoundException e) {
            LOGGER.warn("没有找到配置文件，程序将自动创建并退出");
            try {
                if (configFile.createNewFile()) {
                    this.configMap.put("metaDataServer", "cn.ixiaohei.com.cn");
                    this.configMap.put("metaDataServerPort", 8956);
                    this.fileWriter = new FileWriter(configFile);
                    LOGGER.info("正在写入");
                    this.fileWriter.write(new GsonBuilder().setPrettyPrinting().create().toJson(configMap));
                    this.fileWriter.flush();
                    LOGGER.info("成功");

                } else {
                    LOGGER.fatal("致命错误，配置文件无法写入");
                }
                System.exit(0);
            } catch (IOException ioException) {
                LOGGER.error("配置文件创建失败，可能是权限不足，程序退出。错误如下: ", e);
                System.exit(0);
            }
        } catch (IOException e) {
            LOGGER.fatal(e);
            System.exit(0);
        }
    }
}
