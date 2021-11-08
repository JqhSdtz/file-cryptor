public class AppConfig {

    private static String title = "文件加密";

    private static int width = 600;

    private static int height = 400;
    
    // 密码输入时是否有回显
    private static boolean isPasswordEcho = true;

    public static int getHeight() {
        return height;
    }

    public static void setHeight(int height) {
        AppConfig.height = height;
    }

    public static int getWidth() {
        return width;
    }

    public static void setWidth(int width) {
        AppConfig.width = width;
    }

    public static String getTitle() {
        return title;
    }

    public static void setTitle(String title) {
        AppConfig.title = title;
    }
    
    public static void setPasswordEcho(boolean isEnable) {
        isPasswordEcho = isEnable;
    }

    public static boolean isPasswordEcho() {
        return isPasswordEcho;
    }
}
