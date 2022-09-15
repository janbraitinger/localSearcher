import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.BytesRef;
import org.bytedeco.javacv.FrameFilter;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Main {

    String indexDir = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/src/index";
    String dataDir = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/dumpData";
    Indexer indexer;
    Searcher searcher;
    static String configFile = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/conf.ini";
    ConfManager cMan;

    public Main() throws IOException, ParseException {
        System.out.println("check " + dataDir);
        cMan = new ConfManager(configFile);

        //searcher.writeIndexTerms();


    }


    private void test() throws IOException, ParseException {
        searcher = new Searcher(indexDir);


    }

    public static void main(String[] args) throws IOException {




        Main tester = null;

        try {

            tester = new Main();

            tester.deleteIndex();
            tester.createIndex();
            tester.test();
            //tester.searcher.getPreviewOfSingleQuery(9,"/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/dumpData/DocumentC.txt", "Coronavirus".toLowerCase(), 2);

            //tester.searcher.getPositionOfTerms(9, "covid");





        } catch (Exception e) {
            e.printStackTrace();
        }

        try (ZContext context = new ZContext()) {
            // Socket to talk to clients
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind("tcp://127.0.0.1:5555");
            System.out.println("go");

            while (!Thread.currentThread().isInterrupted()) {
                byte[] reply = socket.recv(0);
                System.out.println(
                        "Received " + ": [" + new String(reply, ZMQ.CHARSET) + "]"
                );


                String operator = new String(reply, 0, 1, Charset.defaultCharset());
                String query = new String(reply, ZMQ.CHARSET).substring(1).toLowerCase();

                System.out.println("general " + operator);
                switch (operator.charAt(0)) {
                    case '0': //searchquery
                        JSONArray resultList = tester.search(query);
                        socket.send(("0"+resultList).toString().getBytes(ZMQ.CHARSET), 0);

                        break;
                    case '1': //changesettings
                        //TODO: Check input
                        //TODO FilePath is wrong
                        //TODO: Check Reading ConfFile



                        //String[] changeData = tester.handleConfChange(reply);
                        try{
                            JSONObject obj = new JSONObject(query);

                        String path = obj.getString("path");
                        System.out.println(1 + " " + path);
                        if(path != null){
                            tester.doIt(path);

                            tester.deleteIndex();
                            String result = tester.createIndex();
                            tester.searcher.setNewIndex("/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/src/index");

                            result = 1 + result;
                            socket.send(result.toString().getBytes(ZMQ.CHARSET), 0);
                            break;
                        }      }catch(Exception e){
                            System.err.println(e);
                            socket.send(e.toString().getBytes(ZMQ.CHARSET), 0);
                        }



                        break;
                    case '2'://get information
                        //String[] readData = tester.handleConfRead(reply);


                        String confResult = tester.getConf();
                        confResult = 2 + confResult;
                        System.out.println(confResult);
                        System.out.println("back");
                        socket.send(confResult.toString().getBytes(ZMQ.CHARSET), 0);
                        break;
                    default:
                        return;
                }


                // Collection<String> similarWords = google.getSimWords(searchQuery, 2500);


            }
        } catch (Exception e) {
            System.err.println(e);
            //throw new RuntimeException(e);
        }

    }

    private String getConf() {
        return cMan.readConf("searching", "dataPath");
    }

    private void doIt(String query) throws IOException, ParseException {

        if(Files.exists(Path.of(query))){
            cMan.writeConf("searching", "dataPath", query);
            return;
        }
        System.err.println("no folder found");
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
        System.out.println("test");
        System.out.println(cMan.readConf("searching", "dataPath"));
        long endTime = System.currentTimeMillis();
        indexer.close();
        String result = numIndexed + " File indexed, time taken: "
                + (endTime - startTime) + " ms";
        System.out.println(result);
        return result;
    }


    private JSONArray search(String searchQuery) throws IOException, ParseException {



        JSONArray finalJSON = new JSONArray();
        System.out.println(searcher.getTotalWordFreq(searchQuery));
        long startTime = System.currentTimeMillis();
        TopDocs hits;
        try {
            hits = searcher.search(searchQuery);
        } catch (Exception e) {
            e.printStackTrace();
            return finalJSON.put("error");
        }
        long endTime = System.currentTimeMillis();
        System.out.println(hits.totalHits + " documents found. Time: " + (endTime - startTime) + " ms");
        JSONArray matching = new JSONArray();
        JSONArray embedding = new JSONArray();
        JSONArray embedding2 = new JSONArray();

        ArrayList<Integer> docList = new ArrayList<Integer>();
        ScoreDoc[] _hits = hits.scoreDocs;

        for (ScoreDoc hit : _hits) {
            String stats = searcher.getExplanation(searchQuery, hit.doc);
            Document doc = searcher.getDocument(hit);
            int docId = hit.doc;
            //System.out.println("matching: " + searcher.getPositionOfTerms(docId, searchQuery));
            docList.add(docId);
            String preview = "";
            JSONObject entry = new JSONObject();
            try {
                 preview = searcher.getPreviewOfSingleQuery(hit.doc, doc.get(LuceneConstants.FILE_PATH), searchQuery, 8);
            }catch (Exception e){
                preview= e.toString();
            }

            entry.put("Title", doc.get(LuceneConstants.FILE_NAME));
            entry.put("Path", doc.get(LuceneConstants.FILE_PATH));
            entry.put("Stats", stats);
            entry.put("Preview", preview);
            entry.put("Date", doc.get(LuceneConstants.CREATION_DATE));

            matching.put(entry);
        }


        startTime = System.currentTimeMillis();
        try{
        Collection<String> simWords = searcher.google.getSimWords(searchQuery, 25);
        for (String simW : simWords) {

            hits = searcher.search(simW);
            _hits = hits.scoreDocs;

            for (ScoreDoc hit : _hits) {

                int docId = hit.doc;
                if (!docList.contains(docId)) {
                   // System.out.println(simW + " match with " + docId);
                    try {
                        docList.add(docId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                    String stats = searcher.getExplanation(searchQuery, hit.doc);
                    Document doc = searcher.getDocument(hit);
                    //System.out.println("google: " + searcher.getPositionOfTerms(docId, searchQuery));
                    String preview = "";

                    try {
                        preview = searcher.getPreviewOfSingleQuery(hit.doc, doc.get(LuceneConstants.FILE_PATH), simW, 8);
                    }catch (Exception e){

                        preview = e.toString();
                    }

                    JSONObject entry = new JSONObject();
                    stats = searcher.getExplanation(simW, hit.doc);
                    entry.put("Term", simW);
                    entry.put("Title", doc.get(LuceneConstants.FILE_NAME));
                    entry.put("Path", doc.get(LuceneConstants.FILE_PATH));
                    entry.put("Stats", stats);
                    entry.put("Preview", preview);
                    entry.put("Date", doc.get(LuceneConstants.CREATION_DATE));
                    embedding.put(entry);

                }


            }
        }


        simWords = searcher.pubmed.getSimWords(searchQuery, 25);
        for (String simW : simWords) {

            hits = searcher.search(simW);
            _hits = hits.scoreDocs;

            for (ScoreDoc hit : _hits) {

                int docId = hit.doc;
                if (!docList.contains(docId)) {
                    // System.out.println(simW + " match with " + docId);
                    try {
                        docList.add(docId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                    String stats = searcher.getExplanation(searchQuery, hit.doc);
                    Document doc = searcher.getDocument(hit);
                   // System.out.println("pubmed: " + searcher.getPositionOfTerms(docId, searchQuery));

                    JSONObject entry = new JSONObject();
                    stats = searcher.getExplanation(simW, hit.doc);
                    String preview = "";
                    try{
                        preview = searcher.getPreviewOfSingleQuery(hit.doc,doc.get(LuceneConstants.FILE_PATH), simW, 8);

                    }catch (Exception e){
                        preview = e.toString();
                    }

                    entry.put("Term", simW);
                    entry.put("Titl e", doc.get(LuceneConstants.FILE_NAME));
                    entry.put("Path", doc.get(LuceneConstants.FILE_PATH));
                    entry.put("Stats", stats);
                    entry.put("Preview", preview);
                    entry.put("Date", doc.get(LuceneConstants.CREATION_DATE));

                    embedding2.put(entry);

                }


            }
        }

        }catch (Exception e){
            System.out.println("embedding: " + e);
        }



        finalJSON.put(0, matching);
        finalJSON.put(1, embedding);
        finalJSON.put(2, embedding2);
        endTime = System.currentTimeMillis();
        System.out.println((endTime - startTime) + " ms was needed for finding files");
       /* for(Object i : finalJSON){
            System.out.println(i);
        }*/
        return finalJSON;
    }


}