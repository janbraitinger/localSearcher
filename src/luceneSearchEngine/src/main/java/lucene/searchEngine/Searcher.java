package lucene.searchEngine;


import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;


public class Searcher {
    private Map<String, ArrayList<Integer>> termPositionsMap = new HashMap<>();
    IndexReader reader;
    IndexSearcher indexSearcher;
    QueryParser queryParser;
    Query query;
    WordEmbedding google, pubmed;
    ArrayList<JSONObject> wordCloudList = new ArrayList<>();


    public Searcher(String indexDirectoryPath) throws IOException, ParseException {
        setNewIndex(indexDirectoryPath);
        google = new WordEmbedding();
        google.loadModel(Path.EMBEDDINGS + "googleCorpus.bin");
        pubmed = new WordEmbedding();
        pubmed.loadModel(Path.EMBEDDINGS + "pubmed.bin");


    }

    public void setNewIndex(String indexDirectoryPath) throws IOException {
        Directory indexDirectory = FSDirectory.open(Paths.get(indexDirectoryPath));

        reader = DirectoryReader.open(indexDirectory);
        indexSearcher = new IndexSearcher(reader);
        indexSearcher.setSimilarity(new BM25Similarity());

        queryParser = new QueryParser(LuceneConstants.CONTENTS, new StandardAnalyzer());
        queryParser.setDefaultOperator(QueryParser.Operator.AND);


    }


    private double overlayFunction(double L, double xPos) {
        return (Math.pow(10 / (L / 2), 2) * xPos * (L - xPos));
    }

    public TopDocs search(String searchQuery)
            throws IOException, ParseException {
        query = queryParser.parse(searchQuery);
        System.out.println(query);
        return indexSearcher.search(query, LuceneConstants.MAX_SEARCH);
    }

    private void clearFile(String path) throws IOException {
        FileWriter fwOb = new FileWriter(path, false);
        PrintWriter pwOb = new PrintWriter(fwOb, false);
        pwOb.flush();
        pwOb.close();
        fwOb.close();
    }

    public void writeIndexTerms(String path) throws IOException {
        clearFile(path);
        List<LeafReaderContext> list = reader.leaves();
        int counter = 0;
        FileWriter fileWriter = new FileWriter(path, true);

        for (String word : StopWords.stopWordArray) {
            fileWriter.write(word + ",");
            counter++;
            fileWriter.flush();
        }

        for (LeafReaderContext lrc : list) {
            Terms terms = lrc.reader().terms(LuceneConstants.CONTENTS);

            if (terms != null) {
                TermsEnum termsEnum = terms.iterator();
                BytesRef term;
                while ((term = termsEnum.next()) != null) {
                    counter++;

                    fileWriter.write(term.utf8ToString() + ",");
                    fileWriter.flush();
                }
            }
        }
        Console.print("Wrote " + counter + " terms into autocomplete file", 0);
        fileWriter.close();
        this.generateWordCloudList();
    }


    private void generateWordCloudList() throws IOException {
        List<Tuple> indexedWords = new ArrayList<>();

        final Fields fields = MultiFields.getFields(reader);
        final Terms terms = fields.terms(LuceneConstants.CONTENTS);
        if(terms == null){
            return;
        }
        final TermsEnum termsEnum = terms.iterator();
        BytesRef term;
        while ((term = termsEnum.next()) != null) {
            final String word = term.utf8ToString();
            if (word.matches("[a-zA-Z]+") && word.length() > 3) { // optional filter for word length and alphabetic characters only
                final int freq = (int) termsEnum.totalTermFreq();
                indexedWords.add(new Tuple(word, freq));
            }
        }


        Collections.sort(indexedWords, new Comparator<Tuple>() {
            public int compare(Tuple o1, Tuple o2) {
                return o1.getFreq() - o2.getFreq();
            }
        });


        for (int i = 0; i < indexedWords.size(); i++) {
            System.out.println(indexedWords.get(i).term + " - " + indexedWords.get(i).freq);
        }


        ArrayList result = new ArrayList();
        if (indexedWords.size() > 25) {
            for (int i = indexedWords.size() - 25; i < indexedWords.size() - 1; i++) {

                JSONObject messageObj = new JSONObject();
                messageObj.put("word", indexedWords.get(i).term);
                messageObj.put("size", indexedWords.get(i).freq);
                result.add(messageObj);
            }

        } else {
            result = null;
        }


        this.wordCloudList = result;


    }


    public float getBM25Score(String queryString, int docID) throws IOException, ParseException {
        Query query = queryParser.parse(queryString);
        Explanation explanation = indexSearcher.explain(query, docID);
        return explanation.getValue();
    }


    public Document getDocumentById(int docid) throws IOException, ParseException {
        return indexSearcher.doc(docid);
    }

    public Document getDocument(ScoreDoc scoreDoc) throws IOException {
        return indexSearcher.doc(scoreDoc.doc);
    }


    public String getPreviewOfSingleQuery(int docId, String inputQuery) throws IOException, ParseException, InvalidTokenOffsetsException {
        query = queryParser.parse(inputQuery);
        Console.print(inputQuery, 0);
        Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(query));
        String text = getDocumentById(docId).get(LuceneConstants.CONTENTS);
        String preview = highlighter.getBestFragment(new StandardAnalyzer(), LuceneConstants.HIGHLIGHT_INDEX, text);
        Console.print(preview, 0);
        return preview;
    }

    public Integer calcIndexDistance(int docId, String[] query) throws IOException {
        List<List<Integer>> lst = new ArrayList<List<Integer>>();

        for (int i = 0; i < query.length; i++) {
            ArrayList tmpIndexes = getIndexPositionOfTerm(docId, query[i]);
            if (tmpIndexes.size() == 0) {
                return 1;
            }
            lst.add(tmpIndexes);
        }


        List<List<Integer>> result = cartesian(lst);


        ArrayList<Integer> counterList = new ArrayList<>();
        for (List<Integer> r : result) {
            ArrayList<Integer> tmpAdder = new ArrayList<>();
            for (Integer i : r) {
                tmpAdder.add(i);
            }


            Collections.sort(tmpAdder);
            int first = tmpAdder.get(0);
            int last = tmpAdder.get(tmpAdder.size() - 1);
            int indexDistance = first - last;
            counterList.add(Math.abs(indexDistance));
        }
        Collections.sort(counterList);


        return counterList.get(0);

/*
        for (String term : query) {
            ArrayList<Integer> tmpIndexes = getIndexPositionOfTerm(docId, term);
            if (tmpIndexes.isEmpty()) {
                return 1;
            }
            lst.add(tmpIndexes);
        }

        List<List<Integer>> result = cartesian(lst);
        int minDistance = Integer.MAX_VALUE;

        for (List<Integer> indexes : result) {
            Collections.sort(indexes);
            int first = indexes.get(0);
            int last = indexes.get(indexes.size() - 1);
            int distance = Math.abs(first - last);
            minDistance = Math.min(minDistance, distance);
        }
        return minDistance;*/


    }


    public List<List<Integer>> cartesian(List<List<Integer>> list) {
        List<List<Integer>> result = new ArrayList<List<Integer>>();
        int numSets = list.size();
        Integer[] tmpResult = new Integer[numSets];
        cartesian(list, 0, tmpResult, result);
        return result;
    }

    public void cartesian(List<List<Integer>> list, int n, Integer[] tmpResult, List<List<Integer>> result) {
        if (n == list.size()) {
            result.add(new ArrayList<Integer>(Arrays.asList(tmpResult)));
            return;
        }

        for (Integer i : list.get(n)) {
            tmpResult[n] = i;
            cartesian(list, n + 1, tmpResult, result);
        }
    }


    private ArrayList<Integer> getIndexPositionOfTerm(int docId, String query) throws IOException {
        ArrayList<Integer> positonList = new ArrayList<>();
        Terms vector = reader.getTermVector(docId, LuceneConstants.TERM_DETAILS);
        TermsEnum terms = vector.iterator();
        PostingsEnum positions = null;
        BytesRef term;


        while ((term = terms.next()) != null) {
            String termstr = term.utf8ToString();
            String result = termstr.replaceAll("[-+.^:,]", "");
            if (result.equals(query)) {

                long freq = terms.totalTermFreq();
                positions = terms.postings(positions, PostingsEnum.POSITIONS);
                positions.nextDoc();

                for (int i = 0; i < freq; i++) {
                    positonList.add(positions.nextPosition());
                }
            }
        }
        return positonList;
    }


    // faster
       /*  private ArrayList<Integer> getIndexPositionOfTerm(int docId, String query) throws IOException {
            ArrayList<Integer> positonList = termPositionsMap.get(query);
            if (positonList != null) {
                return positonList;
            }
            positonList = new ArrayList<>();
            Terms vector = reader.getTermVector(docId, LuceneConstants.TERM_DETAILS);
            TermsEnum terms = vector.iterator();
            PostingsEnum positions = null;
            BytesRef term;

            while ((term = terms.next()) != null) {
                String termstr = term.utf8ToString();
                if (termstr.equals(query)) {
                    long freq = terms.totalTermFreq();
                    positions = terms.postings(positions, PostingsEnum.POSITIONS);
                    positions.nextDoc();

                    for (int i = 0; i < freq; i++) {
                        positonList.add(positions.nextPosition());
                    }
                    termPositionsMap.put(query, positonList);
                    return positonList;
                }
            }
            return positonList;
        }*/


}