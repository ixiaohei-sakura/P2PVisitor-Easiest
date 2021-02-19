package com.cn.ixiaohei;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class Constants {
    public static final String JARRootPath = getJARRootPathPath() + "/";
    public static final String JARPath = getJARPath();
    public static final String ConfigFilePath = JARRootPath + "/config.json";
    public static final Logger LOGGER = LogManager.getLogger();

    public static String getJARPath(){
        File j = new File(Constants.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        return URLDecoder.decode(j.getPath(), StandardCharsets.UTF_8);
    }

    public static String getJARRootPathPath(){
        String path;
        try{
            URL location = Constants.class.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(URLDecoder.decode(location.getPath(), StandardCharsets.UTF_8));
            if(file.isDirectory()){
                path = file.getAbsolutePath();
            } else {
                path = file.getParent();
            }
            return path;
        }catch (Exception e){
            assert LOGGER != null;
            LOGGER.fatal("错误:", e);
            System.exit(0);
        }
        return "";
    }
}
