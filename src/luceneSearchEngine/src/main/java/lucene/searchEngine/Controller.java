package lucene.searchEngine;


import com.beust.ah.A;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.http.Context;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Array;
import java.util.*;
import java.util.stream.Collectors;


public class Controller {

    private Context handler;


    public Controller(Context handler) {
        this.handler = handler;
        this.handler.contentType("application/json");
    }

    public void getStatus() {
        this.handler.json(new Response("status", "online"));

    }

    public void getWordCloud(Searcher searcher) {
        if(searcher.wordCloudList.get(0).equals("less")){
            this.handler.json(new Response("wordcloud","less"));
            return;
        }
        ArrayList<JSONObject> wordcloudList = searcher.wordCloudList;

        try {
            List<Map<String, Object>> data = wordcloudList.stream()
                    .filter(Objects::nonNull)
                    .map(json -> json.toMap())
                    .collect(Collectors.toList());

            this.handler.json(new Response("wordcloud", data));
        }catch (Exception e){
            this.handler.json(new Response("wordcloud","error: \n " + e));
        }

    }

    public void getConf(ConfManager conf) {
        String confResult = conf.readConf("searching", "dataPath");
        this.handler.json(new Response("configuration", confResult));
    }


    public void getInfo(int indexedDocuments) {
        this.handler.json(new Response("indexedDocuments", indexedDocuments));
    }


    public void search(Searcher searcher) throws InvalidTokenOffsetsException, IOException, ParseException {




        String data = this.handler.queryParam("data");

        JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
        String embedding;
        try {
            embedding = jsonObject.get("embedding").toString();
        }
        catch(Exception e)
        {
            embedding = new String[]{"google", "pubmed"}.toString();
        }
        String queryValue = jsonObject.get("query").toString();
        queryValue = queryValue.replaceAll("[^a-zA-Z0-9\\s]", "");
        System.out.println("Embedding: " + embedding + ", Query: " + queryValue);


       /* String data = this.handler.pathParam("data");
        JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
        String embeddingTypes = jsonObject.get("embedding").toString();


        String body = jsonObject.get("query").toString();
        body = body.replace("&", " ");
        body = body.substring(1, body.length() - 1);



        body = body.replaceAll("[^a-zA-Z0-9\\s]", "");
*/
        ArrayList<JSONObject> searchResult = this.searchInDocuments(queryValue, searcher, embedding);


        List<Map<String, Object>> resultList = searchResult.stream()
                .filter(Objects::nonNull)
                .map(json -> json.toMap())
                .collect(Collectors.toList());
        this.handler.json(new Response("resultlist", resultList));


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
            Console.print("Reindex", 0);
            app.setup();
            //handler.result(this.buildMessage("reindexedTime", "no server time is set, sry"));
            this.handler.json(new Response("configuration", "new path was set"));
        } else {
            this.handler.json(new Response("configuration", "folder does not exist"));

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
                float bm25 = searchObject.getBM25(docId);
                double similarity = 1;
                int distance = searchObject.getDistance(docId);
                float weight = (float) (0.6 * bm25 + 0.2 * (1/distance) + 0.2 * similarity);
                System.out.println(weight);

                String preview = searchObject.getPreview(docId);
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
                        String newQuery = new String(); // based on embedding

                        for (String term : singleCombination) {
                            newQuery += term + " ";
                        }


                        embeddingHit = searcher.search(newQuery);
                        hitCollection = embeddingHit.scoreDocs;


                        for (ScoreDoc hit : hitCollection) {
                            int docId = hit.doc;
                            if (!checkDockList.contains(docId)) {
                                checkDockList.add(docId);
                                Document document = searcher.getDocument(hit);


                                double bm25 = searchObject.getBM25(docId);
                                double similarity = searchObject.getSimilarityTo(newQuery, embedding);

                                int distance = searchObject.getDistance(docId);

                                double weight = 0.6 * bm25 + 0.2 * (1/distance) + 0.2 * similarity;




                                String preview = searchObject.getEmbeddingPreview(docId, newQuery);
                                JSONObject jsonMessage = null;


                                jsonMessage = buildMessage(embedding, newQuery, weight, document, preview);

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

    private JSONObject buildMessage(int matchingOperation, String term, double weight, Document doc, String preview) {
        JSONObject messageSubItem = new JSONObject();
        messageSubItem.put("Matching", matchingOperation);
        messageSubItem.put("Term", term);
        messageSubItem.put("Title", doc.get(LuceneConstants.FILE_NAME));
        messageSubItem.put("Path", doc.get(LuceneConstants.FILE_PATH));
        try {
            messageSubItem.put("Weight", weight); // if problem -> remove
        }
        catch (Exception e){
        }
        messageSubItem.put("Preview", preview);
        messageSubItem.put("Date", doc.get(LuceneConstants.CREATION_DATE));
        //messageSubItem.put("Date", "doc.get(LuceneConstants.CREATION_DATE)");
        return messageSubItem;
    }


}
