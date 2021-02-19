package com.cn.ixiaohei;

public class OSCheck {
    private enum EPlatform {
        Linux("Linux"),
        Mac_OS("MacOS"),
        Windows("Windows");

        private EPlatform(String desc){
            this.description = desc;
        }

        public String toString(){
            return description;
        }

        private final String description;
    }

    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static OSCheck _instance = new OSCheck();
    private EPlatform platform;
    OSCheck(){}

    public static boolean isLinux(){
        return OS.contains("linux");
    }

    public static boolean isMacOS(){
        return OS.contains("mac");
    }

    public static boolean isWindows(){
        return OS.contains("windows");
    }
}
