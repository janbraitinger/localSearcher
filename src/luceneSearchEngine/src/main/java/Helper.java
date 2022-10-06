import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

class SimilarObject {
    String term;
    double similarity;

}

class IndexTuple {
    String term;
    int position;
}

class Console {

    public static final int LEVEL_INFO = 0;
    public static final int LEVEL_WARNING = 1;
    public static final int LEVEL_ERROR = 2;

    private static final ThreadLocal<SimpleDateFormat> formatter = ThreadLocal.withInitial(() -> new SimpleDateFormat("HH:mm:ss"));


    public static void print(String messageToPrefix, int errorLevel) {
        StringBuilder message = new StringBuilder();
        message.append("[").append(formatter.get().format(new Date())).append("] ");
        switch (errorLevel) {
            case LEVEL_INFO:
                message.append("[Info] ");
                break;
            case LEVEL_WARNING:
                message.append("[Warning] ");
                break;
            case LEVEL_ERROR:
                message.append("[Error] ");
                break;
        }
        message.append(messageToPrefix);
        System.out.println(message.toString());
    }

}