public class BasicUtil {

    public static String readLine() {
        return System.console().readLine();
    }

    public static String readPassword() {
        return new String(System.console().readPassword());
    }

    public static void print(String str) {
        System.out.println(str);
    }

    public static void print(Object str) {
        System.out.println(str);
    }

    public static String getTimeDescription(long seconds) {
        String timeStr = "";
        if (seconds >= 3600) {
            timeStr += (seconds / 3600) + "小时";
            seconds %= 3600;
        }
        if (seconds >= 60) {
            timeStr += (seconds / 60) + "分钟";
            seconds %= 60;
        }
        if (seconds != 0) {
            timeStr += seconds + "秒";
        }
        return timeStr;
    }

    public static String getFileSizeDescription(long byteNum) {
        String sizeStr = "";
        if (byteNum >= 1073741824L) {
            sizeStr += (byteNum / 1073741824L) + "G";
            byteNum %= 1073741824L;
        }
        if (byteNum >= 1048576L) {
            sizeStr += (byteNum / 1048576L) + "M";
            byteNum %= 1048576L;
        }
        if (byteNum >= 1024) {
            sizeStr += (byteNum / 1024) + "K";
            byteNum %= 1024;
        }
        if (byteNum != 0) {
            sizeStr += byteNum + "B";
        }
        return sizeStr;
    }
}
