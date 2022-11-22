package lucene.searchEngine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.Javalin;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.bytedeco.javacv.FrameFilter;
import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.Semaphore;


public class Main {

    // toDo: Move paths to .conf file
    private String indexDir = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/src/index";
    private String autoCompletePath = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/src/indexData.txt";
    private String configFile = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/conf.ini";

    private Searcher searcher;
    private ConfManager cMan;
    private final int PORT = 4001;
    Controller controller = new Controller(PORT);
    volatile boolean semaphore;

    public Main() {

        Console.print("Start indexing", 0);

        try {
            this.createConfManagerObj();
            this.deleteIndex();
            this.createIndex();
            this.generateSearcherObj();
            this.searcher.writeIndexTerms(autoCompletePath);
            Javalin endpoint = this.controller.createEndpoint();


            endpoint.get("/status", handler -> {

                String result = this.controller.buildMessage("status", "online");
                handler.result(result);
            });
            endpoint.get("/search/{data}", handler -> {
                if (!semaphore) {
                    String data = handler.pathParam("data");
                    JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
                    String body = jsonObject.get("body").toString();
                    body = body.replace("&", " ");
                    body = body.substring(1, body.length() - 1);
                    System.out.println(body);
                    handler.result(this.search(body).toString());
                    return;
                }
                handler.result(this.controller.buildMessage("error", "semaphore blocked"));

                //this.search();
            });


            endpoint.get("/setConf/{data}", handler -> {
                System.out.println("hallo");
                if (!semaphore) {
                    System.out.println("ey");
                    semaphore = true;
                    String timetaken = null;
                    String messageString = null;
                    try {
                        String data = handler.pathParam("data");
                        JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();

                        String body = jsonObject.get("body").toString();


                        body = body.substring(1, body.length() - 1);
                        body = body.replaceAll("-", "/");
                        System.out.println(body);
                        String path = body;
                        timetaken = "";
                        //JSONObject obj = new JSONObject(newPath);
                        //String path = obj.getString("body");

                        if (path != null && Files.exists(Path.of(path))) {
                            this.changeDataDir(path);
                            this.deleteIndex();
                            timetaken = this.createIndex();
                            this.searcher.setNewIndex(indexDir);
                            this.searcher.writeIndexTerms(autoCompletePath);
                            messageString = buildMessageString(SocketMessages.CHANGE_CONF, timetaken, null);
                            handler.result(messageString);
                            handler.result(this.controller.buildMessage("reindexedTime", messageString));
                        }else{
                            handler.result(this.controller.buildMessage("error", "folder does not exist"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    semaphore = false;
                    return;
                }
                handler.result(this.controller.buildMessage("error", "semaphore blocked"));

                //this.search();
            });

            endpoint.get("/wordcloud", handler -> {
                ArrayList<JSONObject> wordcloud = this.searcher.wordCloudList;
                String message = buildMessageString("wordcloud", wordcloud, null);
                handler.result(message);
            });

            endpoint.get("/conf", handler -> {
                String confResult = getConf();
                String message = buildMessageString(SocketMessages.READ_CONF, confResult, null);
                handler.result(message);
            });


            // this.startSocketThread();


        } catch (Exception e) {
            Console.print(e.toString(), 2);
        }
    }

    public static void main(String[] args) {
        new Main();
    }


    private void startSocketThread() {
        try (ZContext context = new ZContext(2)) {

            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind("tcp://127.0.0.1:5556");


            Console.print("Socket connection is deployed. Ready to rumble\n" +
                    "----------------------------------------------------------------------", 0);

            String messageString = "";


            while (!Thread.currentThread().isInterrupted()) {

                byte[] reply = socket.recv(0);
                Console.print(
                        "Received message from Node.js: " + new String(reply, ZMQ.CHARSET)
                        , 0);

                JSONObject inputMessageObj = new JSONObject(new String(reply, ZMQ.CHARSET));
                String header = (String) inputMessageObj.get("header");


                switch (header) {
                    // search + query
                    case SocketMessages.SEND_DOCUMENT_LIST:
                        String query = (String) inputMessageObj.get("body");
                        //String subBody = (String) inputMessageObj.get("subBody");
                        // System.out.println(subBody);
                        Console.print("Searching for query '" + query + "'", 0);
                        ArrayList<JSONObject> resultList = search(query);
                        messageString = buildMessageString(SocketMessages.SEND_DOCUMENT_LIST, resultList.toString(), null);
                        socket.send(messageString.getBytes(ZMQ.CHARSET), 0);
                        break;

                    // set conf
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
                                messageString = buildMessageString(SocketMessages.CHANGE_CONF, result, this.searcher.wordCloudList);
                                socket.send(messageString.getBytes(ZMQ.CHARSET), 0);
                                break;
                            }
                        } catch (Exception e) {
                            System.err.println(e);
                            socket.send(e.toString().getBytes(ZMQ.CHARSET), 0);
                        }
                        break;
                    // get conf
                    case SocketMessages.READ_CONF:
                        Console.print("Reading data from conf file", 0);
                        String confResult = getConf();


                        messageString = buildMessageString(SocketMessages.READ_CONF, confResult, this.searcher.wordCloudList);
                        socket.send(messageString.getBytes(ZMQ.CHARSET), 0);
                        break;
                    //get status
                    case SocketMessages.GET_STATUS:
                        messageString = buildMessageString(SocketMessages.GET_STATUS, "pong", null);
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


    private String buildMessageString(String header, Object body, Object subbody) {
        JSONObject messageObj = new JSONObject();
        messageObj.put("header", header);
        messageObj.put("body", body);
        messageObj.put("subbody", subbody);
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

    private void generateSearcherObj() throws IOException, ParseException {
        searcher = new Searcher(indexDir);
    }

    private void createConfManagerObj() throws IOException {
        cMan = new ConfManager(configFile);
    }


    private void deleteIndex() {
        Arrays.stream(new File(indexDir).listFiles()).forEach(File::delete);
    }


    private String createIndex() throws IOException {
        Indexer indexer = new Indexer(indexDir);

        long startTime = System.currentTimeMillis();
        String dirPath = cMan.readConf("searching", "dataPath");

        int numIndexed = indexer.createIndex(dirPath, new TextFileFilter());

        Console.print("dirPath is " + cMan.readConf("searching", "dataPath"), 0);
        long endTime = System.currentTimeMillis();

        indexer.close();

        String consoleMessage = numIndexed + " file(s) indexed, time taken: " + (endTime - startTime) + " ms";
        Console.print(consoleMessage, 0);
        return consoleMessage;
    }


    private ArrayList<JSONObject> search(String searchQuery) throws IOException, ParseException, InvalidTokenOffsetsException {
        long startTime = System.currentTimeMillis();
        ArrayList<JSONObject> addHitsToMessage = new ArrayList<>();

        SearchObject searchObject = new SearchObject(searchQuery, searcher);
        searchObject.activateEmbeddings();


        TopDocs directHits = searcher.search(searchObject.getQueryString());
        ScoreDoc[] directHitCollection = directHits.scoreDocs;


        // without Embeddings
        for (ScoreDoc hit : directHitCollection) {

            int docId = hit.doc;
            if (!searchObject.hitDocs.contains(docId)) {
                searchObject.hitDocs.add(docId);

                Document document = searcher.getDocument(hit);
                float weight = searchObject.getWeight(docId) + 2;

                String preview = searchObject.getPreview(docId, document.get(LuceneConstants.FILE_PATH));
                JSONObject jsonMessage = buildMessage(LuceneConstants.NORMAL_MATCHING, searchObject.getQueryString(), weight, document, preview);
                addHitsToMessage.add(jsonMessage);

            }
        }

        //with embeddings
        if (searchObject.useEmbeddings) {
            TopDocs embeddingHit;
            ScoreDoc[] hitCollection;
            try {
                int embedding = 1;
                for (List<List<String>> embeddingList : searchObject.getEmbeddings()) {

                    for (List<String> singleCombination : embeddingList) {

                        String newSearchQuery = new String();

                        for (String term : singleCombination) {
                            newSearchQuery += term + " ";
                        }

                        embeddingHit = searcher.search(newSearchQuery);
                        hitCollection = embeddingHit.scoreDocs;
                        for (ScoreDoc hit : hitCollection) {

                            int docId = hit.doc;
                            if (!searchObject.hitDocs.contains(docId)) {
                                searchObject.hitDocs.add(docId);
                                Document document = searcher.getDocument(hit);
                                double similarity = searchObject.getSimilarityTo(newSearchQuery, embedding);
                                float weight = (float) (searchObject.getWeight(docId) + similarity);

                                String preview = searchObject.getPreview(docId, document.get(LuceneConstants.FILE_PATH));
                                JSONObject jsonMessage = buildMessage(embedding, newSearchQuery, weight, document, preview);
                                addHitsToMessage.add(jsonMessage);
                            }
                        }


                    }
                    embedding++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        JSONObject resultCounter = new JSONObject();
        long estimatedTime = System.currentTimeMillis() - startTime;

        Console.print("Found " + addHitsToMessage.size() + " documents as result", 0);
        HashMap timeStats = searchObject.getTimeStats();

        resultCounter.put("time", estimatedTime);
        resultCounter.put("stats", new JSONObject(timeStats));

        addHitsToMessage.add(resultCounter);
        return addHitsToMessage;

    }

    private JSONObject buildMessage(int matchingOperation, String term, float weight, Document doc, String preview) {
        JSONObject messageSubItem = new JSONObject();
        messageSubItem.put("Matching", matchingOperation);
        messageSubItem.put("Term", term);
        messageSubItem.put("Title", doc.get(LuceneConstants.FILE_NAME));
        messageSubItem.put("Path", doc.get(LuceneConstants.FILE_PATH));
        messageSubItem.put("Weight", weight);
        messageSubItem.put("Preview", preview);
        messageSubItem.put("Date", doc.get(LuceneConstants.CREATION_DATE));
        return messageSubItem;
    }
}