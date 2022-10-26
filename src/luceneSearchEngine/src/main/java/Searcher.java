import org.apache.arrow.flatbuf.Int;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
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
import org.opencv.core.Mat;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Searcher {

    IndexReader reader;
    IndexSearcher indexSearcher;
    QueryParser queryParser;
    Query query;
    WordEmbedding google, pubmed;
    int docsLength;

    public Searcher(String indexDirectoryPath) throws IOException, ParseException {
        setNewIndex(indexDirectoryPath);
        String embeddingDir = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/wordEmbeddings/";

        google = new WordEmbedding();
        google.loadModel(embeddingDir + "googleCorpus.bin");
        pubmed = new WordEmbedding();
        pubmed.loadModel(embeddingDir + "pubmed.bin");



    }

    public void setNewIndex(String indexDirectoryPath) throws IOException {

        Directory indexDirectory = FSDirectory.open(Paths.get(indexDirectoryPath));
        reader = DirectoryReader.open(indexDirectory);
        indexSearcher = new IndexSearcher(reader);
        indexSearcher.setSimilarity(new BM25Similarity());

        queryParser = new QueryParser(LuceneConstants.CONTENTS, new StandardAnalyzer());

        queryParser.setDefaultOperator(QueryParser.Operator.AND);

        docsLength = getDocsLength();
    }


    public TopDocs search(Query query, Sort sort)
            throws IOException, ParseException {

        return indexSearcher.search(query,
                LuceneConstants.MAX_SEARCH, sort);
    }

    public TopDocs search(String searchQuery)
            throws IOException, ParseException {

        query = queryParser.parse(searchQuery);
        return indexSearcher.search(query, LuceneConstants.MAX_SEARCH);
    }

    public long getDocCount() throws IOException {
        return indexSearcher.collectionStatistics(LuceneConstants.FILE_NAME).maxDoc();
    }

    public long getCountOfAllWords() throws IOException {
        return reader.getSumDocFreq(LuceneConstants.CONTENTS);
    }

    public long getWordCountOfDoc(int docid) {
        return 0;
    }



    public void writeIndexTerms() throws IOException {

        String path = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/src/indexData.txt";
        FileWriter fileWriter = new FileWriter(path);
        List<LeafReaderContext> list = reader.leaves();
        String[] stopWordArray = {"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"};

        for(String word: stopWordArray){
            fileWriter.write(word + ",");
            fileWriter.flush();
        }


        for (LeafReaderContext lrc : list) {
            Terms terms = lrc.reader().terms(LuceneConstants.CONTENTS);
            if (terms != null) {
                TermsEnum termsEnum = terms.iterator();

                BytesRef term;
                while ((term = termsEnum.next()) != null) {
   
                    fileWriter.write(term.utf8ToString() + ",");
                    fileWriter.flush();

                }
            }
        }
    }

    /*public void writeIndexTerms() {
      String path = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/dumpData/lucene_index/indexData.txt";
        try {

            FileWriter fileWriter = new FileWriter(path);

            Terms terms;
            terms = MultiTerms.getTerms(reader, LuceneConstants.CONTENTS);



            if (terms != null) {
                TermsEnum iter = terms.iterator();
                BytesRef byteRef = null;
                while ((byteRef = iter.next()) != null) {
                    fileWriter.write(byteRef.utf8ToString() + ",");
                    fileWriter.flush();
                }
            }
            fileWriter.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

*/

    public float getBM25Score(String queryString, int docID) throws IOException, ParseException {
        Query query = queryParser.parse(queryString);
        Explanation explanation = indexSearcher.explain(query, docID);
        return explanation.getValue();
    }

    private String getDocName(int docId) throws IOException {
        return reader.document(docId).getField(LuceneConstants.FILE_NAME).stringValue();
    }

    private int getDocsLength() {
        return reader.maxDoc();
    }

    public long getTotalWordFreq(String query) throws IOException {
        Term term = new Term(LuceneConstants.CONTENTS, query);
        return reader.totalTermFreq(term);
    }


    public Document getDocumentById(int docid) throws IOException, ParseException {
        return indexSearcher.doc(docid);
    }

    public Document getDocument(ScoreDoc scoreDoc)
            throws CorruptIndexException, IOException {
        return indexSearcher.doc(scoreDoc.doc);
    }


    public String getPreviewOfSingleQuery(int docId, String filePath, String query, int ngram) throws IOException, ParseException, InvalidTokenOffsetsException {

        Formatter formatter = new SimpleHTMLFormatter();
        Analyzer analyzer = new StopAnalyzer();
        QueryScorer queryScorer = new QueryScorer(queryParser.parse(query));
        Highlighter highlighter = new Highlighter(formatter, queryScorer);
        Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer, 1);
        highlighter.setTextFragmenter(fragmenter);

        TokenStream stream = TokenSources.getTokenStream(reader, docId, LuceneConstants.HIGHLIGHT_INDEX, analyzer);
        String text = getDocumentById(docId).get(LuceneConstants.HIGHLIGHT_INDEX);
        String highlight = Arrays.toString(highlighter.getBestFragments(stream, text, 25));


        return highlight;

    }

    public Integer calcIndexDistance(int docId, String[] query) throws IOException {
        ArrayList indexe = new ArrayList();
        List<List<Integer>> lst = new ArrayList<List<Integer>>();


        for (int i = 0; i < query.length; i++) {

            ArrayList tmpIndexes = getPositionOfTerms(docId, query[i]);
            System.out.println(query[i]);
            System.out.println(tmpIndexes.size());
            if(tmpIndexes.size() == 0){
                return -1;
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
            //System.out.print(" -> " + Math.abs(indexDistance));
            //System.out.println();
        }
        Collections.sort(counterList);



        return counterList.get(0);

    }


    public List<List<Integer>> cartesian(List<List<Integer>> list) {
        List<List<Integer>> result = new ArrayList<List<Integer>>();
        int numSets = list.size();
        Integer[] tmpResult = new Integer[numSets];

        cartesian(list, 0, tmpResult, result);

        return result;
    }

    public void cartesian(List<List<Integer>> list, int n,
                          Integer[] tmpResult, List<List<Integer>> result) {
        if (n == list.size()) {
            result.add(new ArrayList<Integer>(Arrays.asList(tmpResult)));
            return;
        }

        for (Integer i : list.get(n)) {
            tmpResult[n] = i;
            cartesian(list, n + 1, tmpResult, result);
        }
    }




    private ArrayList<Integer> getPositionOfTerms(int docId, String query) throws IOException {
        System.out.println("searching Position for term: <<" + query + ">>");
        ArrayList<Integer> positonList = new ArrayList<>();
        Terms vector = reader.getTermVector(docId, LuceneConstants.TERM_DETAILS);
        TermsEnum terms = vector.iterator();
        PostingsEnum positions = null;
        BytesRef term;


        while ((term = terms.next()) != null) {
            String termstr = term.utf8ToString(); // Get the text string of the term.
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


}