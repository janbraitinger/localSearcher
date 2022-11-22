package lucene.searchEngine;

class LuceneConstants {
    public static final String CONTENTS = "contents";
    public static final String FILE_NAME = "filename";
    public static final String FILE_PATH = "filepath";
    public static final String TERM_DETAILS = "vector";
    public static final String CREATION_DATE = "creation_date";
    public static final String HIGHLIGHT_INDEX = "highlight";
    public static final int NORMAL_MATCHING = 0;
    public static final int EMBEDDING_MATCHING = 1;
    public static final int MAX_SEARCH = 10;}

class SocketMessages{
    public static final String SEND_DOCUMENT_LIST = "documentList";
    public static final String READ_CONF = "getConf";
    public static final String GET_STATUS = "ping";
    public static final String CHANGE_CONF = "changeConf";
}

class Path{
    public static final String EMBEDDINGS = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/wordEmbeddings/";

}