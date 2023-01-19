package lucene.searchEngine;

import java.text.SimpleDateFormat;
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

class StopWords{
    public static final String[] stopWordArray = {"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"};
}

