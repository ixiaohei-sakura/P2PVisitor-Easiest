package com.cn.ixiaohei;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
    public static String getResourcePath(String fileName) {
        return Constants.JARRootPath + fileName;
    }

    private static InputStream getResource(String location) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        InputStream in = resolver.getResource(location).getInputStream();
        byte[] byteArray = IOUtils.toByteArray(in);
        in.close();
        return new ByteArrayInputStream(byteArray);
    }

    public static File getTmpFile(String tmpFile) {
        File tmp = new File(getResourcePath("tmp/" + tmpFile));
        if (!tmp.exists()) {
            if (!new File(tmp.getParent()).exists()) {
                if (new File(tmp.getParent()).mkdirs()) {
                    Constants.LOGGER.debug("文件创建: {}", URLDecoder.decode(tmp.getPath(), StandardCharsets.UTF_8));
                }
            }
            try {
                if (tmp.createNewFile()) {
                    Constants.LOGGER.info("文件 {} 创建", URLDecoder.decode(tmp.getPath(), StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                Constants.LOGGER.error("创建文件错误: ", e);
            }
        }
        return tmp;
    }

    /**
     * 获取项目所在文件夹的绝对路径
     * @return String 路径
     */
    private static String getCurrentDirPath() {
        URL url = FileCopyUtils.class.getProtectionDomain().getCodeSource().getLocation();
        String path = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
        if(path.startsWith("file:")) {
            path = path.replace("file:", "");
        }
        if(path.contains(".jar!/")) {
            path = path.substring(0, path.indexOf(".jar!/")+4);
        }

        File file = new File(path);
        path = URLDecoder.decode(file.getParentFile().getAbsolutePath(), StandardCharsets.UTF_8);
        return path;
    }

    private static Path getDistFile(String path) throws IOException {
        String currentRealPath = getCurrentDirPath();
        Path dist = Paths.get(currentRealPath + File.separator + path);
        Path parent = dist.getParent();
        if(parent != null) {
            Files.createDirectories(parent);
        }
        try {
            Files.deleteIfExists(dist);
        } catch (Exception ignored) {

        }
        return dist;
    }

    /**
     * 复制classpath下的文件到jar包的同级目录下
     * @param location 相对路径文件,例如kafka/kafka_client_jaas.conf
     */
    public static void copy(String location) throws IOException {
        InputStream in = getResource("classpath:"+location);
        Path dist = getDistFile("tmp/" + location);
        try {
            Files.copy(in, dist);
        } catch (Exception ignored) {

        }
        in.close();
        if (!OSCheck.isWindows()) {
            if (dist.toFile().setExecutable(true)) {
                Constants.LOGGER.debug("权限设定为可执行: {}", URLDecoder.decode(dist.toFile().getPath(), StandardCharsets.UTF_8));
            }
        }
    }

    public static void delete(String path) throws IOException {
        if (getDistFile("tmp/" + path).toFile().delete()) {
            Constants.LOGGER.debug("文件删除: {}", path);
        }
    }

    public static String readStream(InputStream stream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
        String line;
        StringBuilder data = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            data.append(line).append("\n");
        }
        bufferedReader.close();
        return data.toString();
    }

    public static void deleteAll() {
        if (!OSCheck.isWindows()) {
            Utils.deleteDir(new File(Constants.JARRootPath + "tmp"));
            Constants.LOGGER.info("删除缓存文件");
        }
    }

    public static void deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children == null) {
                return;
            }
            for (String child : children) {
                deleteDir(new File(dir, child));
            }
        }
        try {
            FileDeleteStrategy.FORCE.delete(dir);
        } catch (Exception ignored) {

        }
    }
}
