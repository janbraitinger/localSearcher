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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/*
public class Main2 {

    // toDo: Move paths to .conf file
    private String indexDir = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/src/index";
    private String autoCompletePath = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/src/indexData.txt";
    private String configFile = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/conf.ini";

    private Searcher searcher;
    private ConfManager cMan;




    public Main2() {

        Console.print("start indexing", 0);

        try {
            this.createConfManagerObj();

            this.deleteIndex();
            this.createIndex();
            this.generateSearcherObj();
            this.searcher.writeIndexTerms(autoCompletePath);
            this.startSocketThread();
        } catch (Exception e) {
            Console.print(e.toString(), 2);
        }
    }

    public static void main(String[] args) {
        new Main2();
    }

    private void generateSearcherObj() throws IOException, ParseException {
        searcher = new Searcher(indexDir);
    }

    private void createConfManagerObj() throws IOException {
        cMan = new ConfManager(configFile);
    }


    private void startSocketThread() {
        try (ZContext context = new ZContext(2)) {

            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind("tcp://127.0.0.1:5556");



            Console.print("Socket connection is deployed. Ready to rumble\n" +
          "----------------------------------------------------------------------", 0);

            String messageString = "";


            while (!Thread.currentThread().isInterrupted()) {
             //   Console.print(String.valueOf(socket.monitor("tcp://127.0.0.1:5556", ZMQ.EVENT_ALL)),0);



                byte[] reply = socket.recv(0);
                Console.print(
                        "Received message from Node.js: " + new String(reply, ZMQ.CHARSET)
                        , 0);

                JSONObject inputMessageObj = new JSONObject(new String(reply, ZMQ.CHARSET));
                String header = (String) inputMessageObj.get("header");


                switch (header) {
                    case SocketMessages.SEND_DOCUMENT_LIST:
                        String query = (String) inputMessageObj.get("body");
                        Console.print("Searching for query '" + query + "'", 0);
                        JSONArray resultList = search(query);
                        messageString = buildMessageString(SocketMessages.SEND_DOCUMENT_LIST, resultList.toString());
                        socket.send(messageString.getBytes(ZMQ.CHARSET), 0);
                        break;


                    case SocketMessages.CHANGE_CONF:
                        Console.print("Change settings", 0);
                        String newPath = (String) inputMessageObj.get("body");
                        try {
                            JSONObject obj = new JSONObject(newPath);
                            String path = obj.getString("path");

                            if (path != null) {
                                this.changeDataDir(path);
                                this.deleteIndex();
                                String result = this.createIndex();
                                this.searcher.setNewIndex(indexDir);
                                this.searcher.writeIndexTerms(autoCompletePath);
                                messageString = buildMessageString(SocketMessages.CHANGE_CONF, result);
                                socket.send(messageString.getBytes(ZMQ.CHARSET), 0);
                                break;
                            }
                        } catch (Exception e) {
                            System.err.println(e);
                            socket.send(e.toString().getBytes(ZMQ.CHARSET), 0);
                        }
                        break;

                    case SocketMessages.READ_CONF:
                        Console.print("Reading data from conf file", 0);
                        String confResult = getConf();

                           messageString = buildMessageString(SocketMessages.READ_CONF, confResult);
                        socket.send(messageString.getBytes(ZMQ.CHARSET), 0);
                        break;

                    case SocketMessages.GET_STATUS:
                        messageString = buildMessageString(SocketMessages.GET_STATUS, "pong");
                        socket.send(messageString.getBytes(ZMQ.CHARSET), 0);
                        break;


                    default:
                        Console.print("Message can not be assigned to an operation", 1);
                        socket.send("error".toString().getBytes(ZMQ.CHARSET), 0);
                        break;
                }
            }
        } catch (Exception e) {
            Console.print("Socketerror:\n" + e, 2);

        }
    }

    private String buildMessageString(String header, String body) {
        JSONObject messageObj = new JSONObject();
        messageObj.put("header", header);
        messageObj.put("body", body);
        return messageObj.toString();
    }


    // ToDo: Key as Parameter via Constants
    private String getConf() {
        return cMan.readConf("searching", "dataPath");
    }

    private void changeDataDir(String query) throws IOException, ParseException {
        if (Files.exists(Path.of(query))) {
            cMan.writeConf("searching", "dataPath", query);
            return;
        }
        Console.print("Can not write to conf file", 2);
    }


    private void deleteIndex() {
        Arrays.stream(new File(indexDir).listFiles()).forEach(File::delete);
    }


    private String createIndex() throws IOException {
        Indexer indexer = new Indexer(indexDir);

        long startTime = System.currentTimeMillis();
        String dirPath = cMan.readConf("searching", "dataPath");

        int numIndexed = indexer.createIndex(dirPath, new TextFileFilter());

        Console.print("dirPath is " + cMan.readConf("searching", "dataPath"),0);
        long endTime = System.currentTimeMillis();

        indexer.close();

        String consoleMessage = numIndexed + " file(s) indexed, time taken: " + (endTime - startTime) + " ms";
        Console.print(consoleMessage, 0);
        return consoleMessage;
    }


    private JSONArray searchSingleQuery(String query){
        return null;
    }

    private JSONArray searchMultipleQuery(ArrayList<String> query){
        return null;
    }

    private JSONArray search(String searchQuery) throws IOException, ParseException, InterruptedException {



        JSONArray messageObject = new JSONArray();
        JSONArray directMatches = new JSONArray();
        JSONArray googleCorpusMatches = new JSONArray();
        JSONArray pubMedCorpusMatches = new JSONArray();
        TopDocs hits;
        ArrayList<SimilarObject> embeddingTerms;

        ArrayList<Integer> docList = new ArrayList<Integer>();

        int lengthOfQuery = countWords(searchQuery);
        String[] searchQueryArray = new String[lengthOfQuery];
        boolean multipleQueryFlag = false;
        if (lengthOfQuery > 1) {
            String[] searchQueryArraySplit = searchQuery.split("\\W+");
            searchQueryArray = removeStopWordManually(searchQueryArraySplit);
            Console.print("Detect multiple query: " + Arrays.toString(searchQueryArray), 0);
            multipleQueryFlag = true;
        }


        String newQuery = "";
        for (String subQuery : searchQueryArray) {
            newQuery += subQuery + " ";
        }

        if (!multipleQueryFlag) {
            newQuery = searchQuery;
        }



        hits = searcher.search(newQuery);
        ScoreDoc[] _hits = hits.scoreDocs;


        for (ScoreDoc hit : _hits) {
            float bm25Score = searcher.getBM25Score(newQuery, hit.doc);

            Document doc = searcher.getDocument(hit);
            int docId = hit.doc;
            int queryDistance = 1;
            if (multipleQueryFlag) {
                System.out.println("--" + Arrays.toString(searchQueryArray));
                queryDistance = searcher.calcIndexDistance(docId, searchQueryArray);
            }



            docList.add(docId);
            String preview = "";

            try {
                preview = searcher.getPreviewOfSingleQuery(hit.doc, doc.get(LuceneConstants.FILE_PATH), newQuery, 8);
            } catch (Exception e) {

                preview = e.toString();
            }
            SimilarObject matchinQuery = new SimilarObject();
            matchinQuery.term = newQuery;
            matchinQuery.similarity = 2;
            float documentWeight = (float) ((bm25Score / queryDistance) + matchinQuery.similarity*2);
            directMatches.put(addToMessage(matchinQuery, documentWeight, doc, preview));
        }


        if (multipleQueryFlag) {

            ArrayList embeddingWordList = new ArrayList();
            ArrayList<Collection> embeddings = new ArrayList<>();
            for (String query : searchQueryArray) {

                Collection _embeddingTerms = searcher.pubmed.getSimilarWords(query, 25);


                embeddingWordList.add(_embeddingTerms);
            }



            List<List<String>> _result = cartesian(embeddingWordList);




            for(List<String> list : _result) {
                String newEmbeddingMultipleQuery = "";
                String[] originlaEewEmbeddingMultipleQuery = new String[list.size()];
                int i = 0;
                for(String letter : list) {
                    newEmbeddingMultipleQuery += letter + " ";
                    originlaEewEmbeddingMultipleQuery[i] = letter;
                    i++;

                }


                try{



                    TopDocs neueHits = searcher.search(newEmbeddingMultipleQuery);
                    ScoreDoc[] getroffen = neueHits.scoreDocs;





                    for (ScoreDoc hit : getroffen) {

                        if(!docList.contains(hit.doc)) {

                            double sumSimilarity = 0;
                            for(int j=0; j<searchQueryArray.length;j++){
                                double similarity = searcher.pubmed.getSimilarity(originlaEewEmbeddingMultipleQuery[j], searchQueryArray[j]);
                                //System.out.println(originlaEewEmbeddingMultipleQuery[j] + " - " + searchQueryArray[j] +" -> " + similarity);
                                sumSimilarity += similarity;
                            }

                            sumSimilarity /= searchQueryArray.length;





                            docList.add(hit.doc);
                            Document doc = searcher.getDocument(hit);
                            String preview = "";

                            try {
                                preview = searcher.getPreviewOfSingleQuery(hit.doc, doc.get(LuceneConstants.FILE_PATH), newQuery, 25);
                            } catch (Exception e) {
                                preview = e.toString();
                            }
                            float bm25Score = searcher.getBM25Score(newEmbeddingMultipleQuery, hit.doc);
                            float queryDistance = searcher.calcIndexDistance(hit.doc, originlaEewEmbeddingMultipleQuery);




                            SimilarObject matchinQuery = new SimilarObject();
                            matchinQuery.term = Arrays.toString(originlaEewEmbeddingMultipleQuery);
                            matchinQuery.similarity = sumSimilarity;
                            float documentWeight = (float) (bm25Score / queryDistance + sumSimilarity*2);

                            googleCorpusMatches.put(addToMessage(matchinQuery, documentWeight, doc, preview));
                        }
                    }

                }catch (Exception e){
                    Console.print(e.toString(), 2);
                }

            }



        }
        else {




            //if just one matches - get other word


            // capital of france
            // yes ........ no -> Detect which words occur in the document
            // searching an alternative term for france


            //first step: check if only one word was known
            //second step: alternatives of the other words


            Thread.sleep(2000);
            System.out.println(docList);
            embeddingTerms = searcher.pubmed.getSimilarObjects(searchQuery, 25);


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

                        }
                        //float bm25Score = searcher.getBM25Score(newQuery, hit.doc);
                        Document doc = searcher.getDocument(hit);

                        String preview = "";

                        try {
                            preview = searcher.getPreviewOfSingleQuery(hit.doc, doc.get(LuceneConstants.FILE_PATH), simW.term, 8);
                        } catch (Exception e) {


                            preview = e.toString();
                        }


                        float bm25Score = searcher.getBM25Score(simW.term, hit.doc);
                        float documentWeight = (float) ((bm25Score / 1) + searcher.pubmed.getSimilarity(simW.term, searchQuery)*2);
                        pubMedCorpusMatches.put(addToMessage(simW, bm25Score, doc, preview));

                    }


                }
            }


        }

/*


        //todo: own threads for similarwords function
        embeddingTerms = searcher.pubmed.getSimilarObjects(searchQuery, 25);
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
                    String stats = searcher.getBM25Score(searchQuery, hit.doc);
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


        messageObject.put(0, directMatches);
        messageObject.put(1, googleCorpusMatches);
        messageObject.put(2, pubMedCorpusMatches);
        Console.print("Found " + String.valueOf(docList.size()) + " documents as result", 0);
        return messageObject;
    }


    public List<List<String>> cartesian(List<List<String>> list) {
        long startTime = System.currentTimeMillis();
        List<List<String>> result = new ArrayList<List<String>>();
        int numSets = list.size();
        String[] tmpResult = new String[numSets];

        cartesian(list, 0, tmpResult, result);
        long endTime = System.currentTimeMillis();
        Console.print("Building all combinations needed " + (endTime-startTime) + " ms", 0);
        return result;
    }


    public static String removeLastCharacter(String str) {
        String result = null;
        if ((str != null) && (str.length() > 0)) {
            result = str.substring(0, str.length() - 1);
        }
        return result;
    }

    public void cartesian(List<List<String>> list, int n,
                          String[] tmpResult, List<List<String>> result) {
        if (n == list.size()) {
            result.add(new ArrayList<String>(Arrays.asList(tmpResult)));
            return;
        }

        for (String i : list.get(n)) {
            tmpResult[n] = i;
            cartesian(list, n + 1, tmpResult, result);
        }
    }


    public String[] removeStopWordManually(String[] words) {
        HashSet<String> wordWithStopWord = new HashSet<String>(Arrays.asList(words));
        HashSet<String> StopWordsSet = new HashSet<>(Arrays.asList(StopWords.stopWordArray));
        wordWithStopWord.removeAll(StopWordsSet);
        return wordWithStopWord.toArray(new String[wordWithStopWord.size()]);
    }


    private JSONObject addToMessage(SimilarObject simW, float weight, Document doc, String preview) {
        JSONObject messageSubItem = new JSONObject();
        messageSubItem.put("Term", simW.term);
        messageSubItem.put("Similarity", simW.similarity);
        messageSubItem.put("Title", doc.get(LuceneConstants.FILE_NAME));
        messageSubItem.put("Path", doc.get(LuceneConstants.FILE_PATH));
        messageSubItem.put("Weight", weight);
        messageSubItem.put("Preview", preview);
        messageSubItem.put("Date", doc.get(LuceneConstants.CREATION_DATE));

        return messageSubItem;
    }


    private int countWords(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
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
*/