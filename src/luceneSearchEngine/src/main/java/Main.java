import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Array;
import java.util.*;


public class Main {

    String indexDir = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/src/index";
    String dataDir = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/dumpData";
    Indexer indexer;
    Searcher searcher;
    static String configFile = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/conf.ini";
    ConfManager cMan;

    public Main() throws IOException, ParseException {
        Console.print("checking folder: " + dataDir, 0);
        //Runnable r = () -> System.out.print("Run method in other Thread");
        cMan = new ConfManager(configFile);

        //searcher.writeIndexTerms();


    }


    private void generateSearcherObj() throws IOException, ParseException {
        searcher = new Searcher(indexDir);


    }

    public static void main(String[] args) throws IOException {


        Main tester = null;

        try {

            tester = new Main();

            tester.deleteIndex();
            tester.createIndex();
            tester.generateSearcherObj();


            //tester.searcher.getPreviewOfSingleQuery(9,"/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/dumpData/DocumentC.txt", "Coronavirus".toLowerCase(), 2);
            //tester.searcher.getPositionOfTerms(9, "covid");


        } catch (Exception e) {
            e.printStackTrace();
        }

        try (ZContext context = new ZContext()) {
            // Socket to talk to clients
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind("tcp://127.0.0.1:5555");
            Console.print("Socket connection is deployed. Ready to rumble\n+" +
                    "----------------------------------------------------------------------", 0);

            while (!Thread.currentThread().isInterrupted()) {
                byte[] reply = socket.recv(0);
                Console.print(
                        "Received message from Node.js: " + new String(reply, ZMQ.CHARSET)
                        , 0);


                String operator = new String(reply, 0, 1, Charset.defaultCharset());
                String query = new String(reply, ZMQ.CHARSET).substring(1).toLowerCase();


                switch (operator.charAt(0)) {
                    case '0': //searchquery
                        Console.print("Searching for query '" + query + "'", 0);
                        JSONArray resultList = tester.search(query);
                        socket.send(("0" + resultList).toString().getBytes(ZMQ.CHARSET), 0);

                        break;
                    case '1': //changesettings
                        Console.print("Change settings", 0);

                        //String[] changeData = tester.handleConfChange(reply);
                        try {
                            JSONObject obj = new JSONObject(query);

                            String path = obj.getString("path");
                            //System.out.println(1 + " " + path);
                            if (path != null) {
                                tester.doIt(path);

                                tester.deleteIndex();
                                String result = tester.createIndex();
                                tester.searcher.setNewIndex("/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/src/index");

                                result = 1 + result;
                                socket.send(result.toString().getBytes(ZMQ.CHARSET), 0);
                                break;
                            }
                        } catch (Exception e) {
                            System.err.println(e);
                            socket.send(e.toString().getBytes(ZMQ.CHARSET), 0);
                        }


                        break;
                    case '2'://get information
                        //String[] readData = tester.handleConfRead(reply);

                        Console.print("Reading data from conf file", 0);
                        String confResult = tester.getConf();
                        confResult = 2 + confResult;
                        //System.out.println(confResult);
                        //System.out.println("back");
                        socket.send(confResult.toString().getBytes(ZMQ.CHARSET), 0);
                        break;
                    default:
                        Console.print("Message can not be assigned to an operation", 1);
                        return;
                }


                // Collection<String> similarWords = google.getSimWords(searchQuery, 2500);


            }
        } catch (Exception e) {
            Console.print("Socketerror:\n" + e, 2);

            //throw new RuntimeException(e);
        }

    }

    private String getConf() {
        return cMan.readConf("searching", "dataPath");
    }

    private void doIt(String query) throws IOException, ParseException {

        if (Files.exists(Path.of(query))) {
            cMan.writeConf("searching", "dataPath", query);
            return;
        }
        Console.print("Can not write to conf file", 2);

    }

    private String[] handleConfRead(byte[] input) {
        String[] abc = new String[1];
        return abc;
    }


    private String[] handleConfChange(byte[] input) {
        String[] abc = new String[1];
        return abc;
    }

    private int[] detectWhiteSpaces(String s) {
        int index = -1;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                index = i;
                break;
            }
        }

        System.out.println("Required Index : " + index);
        return null;
    }

    private void deleteIndex() {
        Arrays.stream(new File(indexDir).listFiles()).forEach(File::delete);
    }


    private String createIndex() throws IOException {
        indexer = new Indexer(indexDir);
        int numIndexed;
        long startTime = System.currentTimeMillis();
        //numIndexed = indexer.createIndex(cMan.readConf("searching", "dataPath"), new TextFileFilter());
        String dirPath = cMan.readConf("searching", "dataPath");
        numIndexed = indexer.createIndex(dirPath, new TextFileFilter());
        //System.out.println(cMan.readConf("searching", "dataPath"));
        long endTime = System.currentTimeMillis();
        indexer.close();
        String result = numIndexed + " file(s) indexed, time taken: "
                + (endTime - startTime) + " ms";
        Console.print(result, 0);
        return result;
    }


    private JSONArray search(String searchQuery) throws IOException, ParseException {


        JSONArray messageObject = new JSONArray();
        JSONArray directMatches = new JSONArray();
        JSONArray googleCorpusMatches = new JSONArray();
        JSONArray pubMedCorpusMatches = new JSONArray();
        TopDocs hits;
        ArrayList<SimilarObject> embeddingTerms;


        long startTime = System.currentTimeMillis();

        try {
            hits = searcher.search(searchQuery);
        } catch (Exception e) {
            e.printStackTrace();
            return messageObject.put("error");
        }


        ScoreDoc[] _hits = hits.scoreDocs;
        ArrayList<Integer> docList = new ArrayList<Integer>();


        if (countWords(searchQuery) > 1) {
            //The \\W+ will match all non-alphabetic characters occurring one or more times. So there is no need to replace. You can check other patterns also.
            String[] searchQueryArraySplit = searchQuery.split("\\W+");
            String[] searchQueryArray = removeStopWord(searchQueryArraySplit);
            //System.out.println("---" + searchQueryArray);
            Console.print("Detect multiple query: " + Arrays.toString(searchQueryArray), 0);
        }


        for (ScoreDoc hit : _hits) {
            String stats = searcher.getExplanation(searchQuery, hit.doc);
            Document doc = searcher.getDocument(hit);
            int docId = hit.doc;


            //searcher.calcIndexDistance(docId, searchQuery);


            docList.add(docId);
            String preview = "";

            try {
                preview = searcher.getPreviewOfSingleQuery(hit.doc, doc.get(LuceneConstants.FILE_PATH), searchQuery, 8);
            } catch (Exception e) {
                preview = e.toString();
            }
            SimilarObject matchinQuery = new SimilarObject();
            matchinQuery.term = searchQuery;
            matchinQuery.similarity = 1;
            directMatches.put(addToMessage(matchinQuery, stats, doc, preview));
        }



       /* embeddingTerms = searcher.google.getSimWords(searchQuery, 25);


        for (SimilarObject simW : embeddingTerms) {

            hits = searcher.search(simW.term);
            _hits = hits.scoreDocs;

            for (ScoreDoc hit : _hits) {

                int docId = hit.doc;
                if (!docList.contains(docId)) {

                    try {
                        docList.add(docId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                    String stats = searcher.getExplanation(searchQuery, hit.doc);
                    Document doc = searcher.getDocument(hit);

                    String preview = "";

                    try {
                        preview = searcher.getPreviewOfSingleQuery(hit.doc, doc.get(LuceneConstants.FILE_PATH), simW.term, 8);
                    } catch (Exception e) {

                        preview = e.toString();
                    }


                    stats = searcher.getExplanation(simW.term, hit.doc);
                    googleCorpusMatches.put(addToMessage(simW, stats, doc, preview));

                }


            }
        }

        //todo: own threads for similarwords function
        embeddingTerms = searcher.pubmed.getSimWords(searchQuery, 25);
        for (SimilarObject simW : embeddingTerms) {

            hits = searcher.search(simW.term);
            _hits = hits.scoreDocs;

            for (ScoreDoc hit : _hits) {

                int docId = hit.doc;
                if (!docList.contains(docId)) {

                    try {
                        docList.add(docId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                    String stats = searcher.getExplanation(searchQuery, hit.doc);
                    Document doc = searcher.getDocument(hit);


                    stats = searcher.getExplanation(simW.term, hit.doc);
                    String preview = "";
                    try {
                        preview = searcher.getPreviewOfSingleQuery(hit.doc, doc.get(LuceneConstants.FILE_PATH), simW.term, 8);

                    } catch (Exception e) {
                        preview = e.toString();
                    }

                    pubMedCorpusMatches.put(addToMessage(simW, stats, doc, preview));

                }


            }
        }

*/
        messageObject.put(0, directMatches);
        messageObject.put(1, googleCorpusMatches);
        messageObject.put(2, pubMedCorpusMatches);

        long endTime = System.currentTimeMillis();

        long timeNeeded = endTime - startTime;
        String consoleMesage = timeNeeded + " ms was needed for finding files";

        return messageObject;
    }

    private void iterateSearch(ScoreDoc hit) {

    }


    public String[] removeStopWord(String[] words) {
        String[] stopWords = {"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"};
        HashSet<String> wordWithStopWord = new HashSet<String>(
                Arrays.asList(words));
        HashSet<String> StopWordsSet = new HashSet<>(Arrays.asList(stopWords));
        wordWithStopWord.removeAll(StopWordsSet);
        return wordWithStopWord.toArray(new String[wordWithStopWord.size()]);
    }


    private List<String> getTokens(String str) {
        List<String> tokens = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(str, ",");
        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
        }
        return tokens;
    }

    private JSONObject addToMessage(SimilarObject simW, String stats, Document doc, String preview) {
        JSONObject messageSubItem = new JSONObject();
        messageSubItem.put("Term", simW.term);
        messageSubItem.put("Similarity", simW.similarity);
        messageSubItem.put("Title", doc.get(LuceneConstants.FILE_NAME));
        messageSubItem.put("Path", doc.get(LuceneConstants.FILE_PATH));
        messageSubItem.put("Stats", stats);
        messageSubItem.put("Preview", preview);
        messageSubItem.put("Date", doc.get(LuceneConstants.CREATION_DATE));

        return messageSubItem;
    }

    private int countWords(String str) {
        if (str == null || str.isEmpty())
            return 0;

        int count = 0;
        for (int e = 0; e < str.length(); e++) {
            if (str.charAt(e) != ' ') {
                count++;
                while (str.charAt(e) != ' ' && e < str.length() - 1) {
                    e++;
                }
            }
        }
        return count;
    }


}