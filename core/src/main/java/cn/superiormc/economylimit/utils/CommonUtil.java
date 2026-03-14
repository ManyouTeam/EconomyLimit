package cn.superiormc.economylimit.utils;

public final class CommonUtil {

    private CommonUtil() {
    }

    public static boolean getClass(String className) {
        try {
            Class.forName(className);
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
}
