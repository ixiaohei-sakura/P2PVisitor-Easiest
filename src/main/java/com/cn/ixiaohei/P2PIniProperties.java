package com.cn.ixiaohei;

import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class P2PIniProperties {
    private static final Logger LOGGER = LogManager.getLogger();
    private final JSONObject metaData;
    public final String fileName;
    public final String filePath;
    private final File file;

    public P2PIniProperties(JSONObject metaData, String name, int id) {
        this.metaData = metaData;
        this.fileName = String.format("p2pc/%s%d.p2pc.ini.tmp", name, id);
        this.file = Utils.getTmpFile(fileName);
        this.filePath = URLDecoder.decode(Utils.getTmpFile(fileName).getPath(), StandardCharsets.UTF_8);
    }

    public void write() throws IOException {
        LOGGER.info("写文件 {} | 文件路径: {}", this.fileName, this.filePath);
        FileWriter fileWriter = new FileWriter(file);
        String result = Utils.readStream(this.getClass().getResourceAsStream("/templates/config_template.ini"))
                .replaceAll("%name", metaData.getString("name"))
                .replaceAll("%type", metaData.getString("type"))
                .replaceAll("%role", metaData.getString("role"))
                .replaceAll("%sn", metaData.getString("sn"))
                .replaceAll("%sk", metaData.getString("sk"))
                .replaceAll("%la", metaData.getString("laddr"))
                .replaceAll("%lp", metaData.getString("lport"))
                + Utils.readStream(this.getClass().getResourceAsStream("/templates/common_template.ini"))
                .replaceAll("%sp", metaData.getString("sp"))
                .replaceAll("%sa", metaData.getString("sa"))
                .replaceAll("%sup", metaData.getString("sup"))
                .replaceAll("%token", metaData.getString("token"));
        fileWriter.write(result);
        fileWriter.flush();
        fileWriter.close();
    }

    public void remove() {
        if (this.file.delete()) {
            LOGGER.info("删除文件 {} 文件路径: {}", this.fileName, this.filePath);
        }
    }
}
