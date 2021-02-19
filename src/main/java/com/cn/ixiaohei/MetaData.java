package com.cn.ixiaohei;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MetaData {
    public static final Logger LOGGER = LogManager.getLogger();
    public Socket socket;
    public InputStream inputStream;
    public OutputStream outputStream;
    public BufferedReader br;

    public JSONObject getMetaData(Config config) {
        try {
            LOGGER.info("正在与元数据服务器建立连接 | 地址: {}:{}", config.get("metaDataServer"), config.get("metaDataServerPort"));
            socket = new Socket((String) config.get("metaDataServer"), (Integer) config.get("metaDataServerPort"));
            LOGGER.info("链接已建立，正在读取元数据");
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            br = new BufferedReader(new InputStreamReader(inputStream));
        } catch (Exception e) {
            LOGGER.fatal("MetaData.java: 出现错误致命错误，退出", e);
            System.exit(0);
        }
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("status", 1);
        JSONObject responseData = null;
        try {
            LOGGER.info("读取成功，向服务器发送请求数据");
            outputStream.write(new JSONObject(metaData).toJSONString().getBytes());
            outputStream.flush();
            LOGGER.info("解析数据中");
            StringBuilder response = new StringBuilder();
            String line;
            while((line = br.readLine())!=null){
                response.append(line);
            }
            responseData = JSON.parseObject(response.toString());
            LOGGER.info("数据解析成功");
            LOGGER.debug(response.toString());
            socket.shutdownInput();
            socket.close();
        } catch (Exception e) {
            LOGGER.fatal("MetaData.java: 出现致命错误，退出", e);
            System.exit(0);
        }
        return responseData;
    }
}
