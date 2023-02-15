package lucene.searchEngine;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SearchObject {

    public boolean IS_MULTIPLE;
    private String QUERY;
    private Searcher searcher;
    private ArrayList<String> similarTerms;
    public boolean useEmbeddings = false;
    public ArrayList<Integer> hitDocs = new ArrayList<>();
    private HashMap<String, Long> timeStats = new HashMap<String, Long>();


    public SearchObject(String query, Searcher searcher) {
        this.IS_MULTIPLE = isMultipleQuery(query);
        this.QUERY = this.removeStopWordManually(query);
        this.searcher = searcher;

    }


    public void setNewQuery(String query) {
        this.QUERY = query;
    }

    public void checkEmbedding(String embedding) {
        if (embedding.length() > 2) {
            this.useEmbeddings = true;
        }
    }


    public List<List<List<String>>> getEmbeddings(String embeddingTypes) throws ExecutionException, InterruptedException {
        // todo: dont code the embedding selection hard

        ArrayList pubMedList = new ArrayList();
        ArrayList googleList = new ArrayList();
        int counter = 0;

        for (String query : this.getQueryArray()) {

            ExecutorService executor = Executors.newFixedThreadPool(2);
            System.out.println("--> " + embeddingTypes);

            if (embeddingTypes.contains("pubmed")) {
                long startTime = System.currentTimeMillis();
                Future<ArrayList> pubmedEmbeddingsFuture = executor.submit(() -> this.searcher.pubmed.getSimilarWords(query, 75));
                ArrayList pubmedEmbeddings = pubmedEmbeddingsFuture.get();
                long estimatedTime = System.currentTimeMillis() - startTime;
                timeStats.put("pubmed" + counter, estimatedTime);
                pubMedList.add(pubmedEmbeddings);
            }

            if (embeddingTypes.contains("google")) {
                long startTime = System.currentTimeMillis();
                Future<ArrayList> googleEmbeddingsFuture = executor.submit(() -> this.searcher.google.getSimilarWords(query, 25));
                ArrayList googleEmbeddings = googleEmbeddingsFuture.get();
                long estimatedTime = System.currentTimeMillis() - startTime;
                timeStats.put("google" + counter, estimatedTime);
                googleList.add(googleEmbeddings);
            }
            executor.shutdown();

            counter++;
        }

        List<List<List<String>>> listOfLists = new ArrayList<>();

        if (embeddingTypes.contains("pubmed")) {
            List<List<String>> pubmedCombinations = cartesian(pubMedList);
            // System.out.println(pubmedCombinations);
            listOfLists.add(pubmedCombinations);
        }

        if (embeddingTypes.contains("google")) {
            List<List<String>> googleCombinations = cartesian(googleList);
            listOfLists.add(googleCombinations);
            //System.out.println(googleCombinations);
        }



        return listOfLists;

    }
/*
    public List<List<List<String>>> getEmbeddings(String embeddingTypes) throws ExecutionException, InterruptedException {
        ArrayList pubMedList = new ArrayList();
        ArrayList googleList = new ArrayList();
        System.out.println(embeddingTypes);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future> futures = new ArrayList<>();
        for (String query : this.getQueryArray()) {
            if (embeddingTypes.contains("pubmed")) {
                futures.add(executor.submit(() -> {
                    long startTime = System.currentTimeMillis();
                    ArrayList pubmedEmbeddings = this.searcher.pubmed.getSimilarWords(query, 10);
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    timeStats.put("pubmed" + query, estimatedTime);
                    pubMedList.add(pubmedEmbeddings);
                }));
            }

            if (embeddingTypes.contains("google")) {
                futures.add(executor.submit(() -> {
                    long startTime = System.currentTimeMillis();
                    ArrayList googleEmbeddings = this.searcher.google.getSimilarWords(query, 10);
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    timeStats.put("google" + query, estimatedTime);
                    googleList.add(googleEmbeddings);
                }));
            }
        }
        for (Future future : futures) {
            future.get();
        }
        executor.shutdown();

        List<List<List<String>>> listOfLists = new ArrayList<>();
        if (embeddingTypes.contains("google")) {
            List<List<String>> googleCombinations = cartesian(googleList);
            listOfLists.add(googleCombinations);
            //System.out.println(googleCombinations);
        }
        if (embeddingTypes.contains("pubmed")) {
            List<List<String>> pubmedCombinations = cartesian(pubMedList);
            System.out.println(pubmedCombinations);
            //listOfLists.add(pubmedCombinations);
        }
        return listOfLists;
    }

*/


    public HashMap getTimeStats() {
        return this.timeStats;
    }


    public String getPreview(int docId) throws InvalidTokenOffsetsException, IOException, ParseException {
        return this.searcher.getPreviewOfSingleQuery(docId, this.QUERY);

    }

    public double getSimilarityTo(String newQuery, int embeddingType) {

        if (this.IS_MULTIPLE) {

            String[] tmp = newQuery.split("\\W+");
            int i = 0;
            float sumSimilarity = 0;
            for (String term : tmp) {
                if (embeddingType == 1) {
                    sumSimilarity += this.searcher.pubmed.getSimilarity(term, this.getQueryArray()[i]);
                }
                if (embeddingType == 2) {
                    sumSimilarity += this.searcher.google.getSimilarity(term, this.getQueryArray()[i]);
                }

                i++;
            }
            return sumSimilarity / tmp.length;
        }


        newQuery = this.removeLastCharacter(newQuery);
        //this.QUERY = this.removeLastCharacter(this.QUERY);
        System.out.println(newQuery + " - " + this.QUERY);

        switch (embeddingType) {
            case 1: double sim = this.searcher.pubmed.getSimilarity(this.QUERY, newQuery);
            System.out.println("1");
                return sim;
            case 2:
                System.out.println("2");
                return this.searcher.google.getSimilarity(this.QUERY, newQuery);
            default:
                throw new Error("Invalid embedding type: " + embeddingType);
        }


    }


    public float getBM25(int docId) throws IOException, ParseException {
        return searcher.getBM25Score(this.QUERY, docId);
    }

    public int getDistance(int docId) throws IOException {
        return this.getQueryDistance(docId);
    }
/*
    public float getWeight(int docId) throws IOException, ParseException {
        float bm25 = searcher.getBM25Score(this.QUERY, docId);
        int indexDistance = this.getQueryDistance(docId);
        return this.IS_MULTIPLE ? bm25 / indexDistance : bm25;
    }*/

    private int getQueryDistance(int docId) throws IOException {
        String[] mQuery = this.getQueryArray();
        int distance = this.searcher.calcIndexDistance(docId, mQuery);
        if (distance == 0) {
            return 1;
        }
        if (!this.IS_MULTIPLE) {
            return 1;
        }
        return distance;
    }


    public String getQueryString() {
        return this.QUERY;
    }

    public String[] getQueryArray() {
        return QUERY.split("\\W+");
    }


    private boolean isMultipleQuery(String query) {
        int queryCount = countTerms(query);
        return queryCount > 1;
    }


    private int countTerms(String str) {
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

    public List<List<String>> cartesian(List<List<String>> list) {
        long startTime = System.currentTimeMillis();
        List<List<String>> result = new ArrayList<List<String>>();
        int numSets = list.size();
        String[] tmpResult = new String[numSets];
        cartesian(list, 0, tmpResult, result);
        long endTime = System.currentTimeMillis();
        //Console.print("Building all combinations needed " + (endTime - startTime) + " ms", 0);
        return result;
    }

    private void cartesian(List<List<String>> list, int n, String[] tmpResult, List<List<String>> result) {
        if (n == list.size()) {
            result.add(new ArrayList<String>(Arrays.asList(tmpResult)));
            return;
        }

        for (String i : list.get(n)) {
            tmpResult[n] = i;
            cartesian(list, n + 1, tmpResult, result);
        }
    }


    private String removeLastCharacter(String str) {
        String result = null;
        if ((str != null) && (str.length() > 0)) {
            result = str.substring(0, str.length() - 1);
        }
        return result;
    }

    private String removeStopWordManually(String input) {
        String[] words = input.split("\\W+");
        HashSet<String> wordWithStopWord = new HashSet<String>(Arrays.asList(words));
        HashSet<String> StopWordsSet = new HashSet<>(Arrays.asList(StopWords.stopWordArray));
        wordWithStopWord.removeAll(StopWordsSet);
        String query = "";
        for (String word : wordWithStopWord) {
            query += word + " ";
        }
        query = this.removeLastCharacter(query);
        return query;
    }

}
