package lucene.searchEngine;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.http.Context;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Controller {

    private Context handler;


    public Controller(Context handler) {
        this.handler = handler;
    }

    public void getStatus() {
        this.handler.result(this.buildMessage("status", "online"));
    }

    public void getWordCloud(Searcher searcher) {
        ArrayList<JSONObject> wordcloudList = searcher.wordCloudList;
        this.handler.result(this.buildMessage("wordcloud", wordcloudList));
    }

    public void getConf(ConfManager conf) {
        String confResult = conf.readConf("searching", "dataPath");
        this.handler.result(this.buildMessage("get conf", confResult));
    }

    public void search(Searcher searcher) throws InvalidTokenOffsetsException, IOException, ParseException {

        String data = this.handler.pathParam("data");
        JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
        String embeddingTypes =  jsonObject.get("embedding").toString();


        String body = jsonObject.get("query").toString();
        body = body.replace("&", " ");
        body = body.substring(1, body.length() - 1);
        body = body.replaceAll("[-+.^:,]","");
        String searchResult = this.searchInDocuments(body, searcher, embeddingTypes).toString();
        this.handler.result(searchResult); // change to json


    }

    public String buildMessage(String header, Object body) {
        Message msgObj = new Message(header, body);
        return msgObj.getMessage();
    }

    public void setConf(ConfManager confManager, Searcher searcher, Application app) throws IOException, ParseException {
        String data = handler.pathParam("data");
        JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
        String body = jsonObject.get("body").toString();
        body = body.substring(1, body.length() - 1);
        body = body.replaceAll("-", "/");
        String path = body;

            if (path != null && Files.exists(Path.of(path))) {
                this.changeDataDir(path, confManager);
                Console.print("Reindex",0);
                app.setup();
                handler.result(this.buildMessage("reindexedTime", "no server time is set, sry"));
            } else {
                handler.result(this.buildMessage("error", "folder does not exist"));
            }


    }


    private void changeDataDir(String query, ConfManager confManager) throws IOException, ParseException {
        if (Files.exists(java.nio.file.Path.of(query))) {
            confManager.writeConf("searching", "dataPath", query);
            return;
        }
        Console.print("Can not write to conf file", 2);
    }


    private ArrayList<JSONObject> searchInDocuments(String searchQuery, Searcher searcher, String embeddingTypes) throws IOException, ParseException, InvalidTokenOffsetsException {
        searchQuery = searchQuery.toLowerCase();
        long startTime = System.currentTimeMillis();
        ArrayList<JSONObject> addHitsToMessage = new ArrayList<>();

        SearchObject searchObject = new SearchObject(searchQuery, searcher);


        searchObject.checkEmbedding(embeddingTypes);


        TopDocs directHits = searcher.search(searchObject.getQueryString());
        ScoreDoc[] directHitCollection = directHits.scoreDocs;

        ArrayList<Integer> checkDockList = new ArrayList<>();

        // without Embeddings
        for (ScoreDoc hit : directHitCollection) {

            int docId = hit.doc;
            if (!checkDockList.contains(docId)) {
                checkDockList.add(docId);

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
                for (List<List<String>> embeddingList : searchObject.getEmbeddings(embeddingTypes)) {

                    for (List<String> singleCombination : embeddingList) {

                        String newSearchQuery = new String();

                        for (String term : singleCombination) {
                            newSearchQuery += term + " ";
                        }

                        embeddingHit = searcher.search(newSearchQuery);
                        hitCollection = embeddingHit.scoreDocs;
                        for (ScoreDoc hit : hitCollection) {

                            int docId = hit.doc;
                            if (!checkDockList.contains(docId)) {
                                checkDockList.add(docId);
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
        messageSubItem.put("Weight", weight); // if problem -> remove
        messageSubItem.put("Preview", preview);
        messageSubItem.put("Date", doc.get(LuceneConstants.CREATION_DATE));
        //messageSubItem.put("Date", "doc.get(LuceneConstants.CREATION_DATE)");
        return messageSubItem;
    }


}
