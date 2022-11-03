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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class Searcher {

    IndexReader reader;
    IndexSearcher indexSearcher;
    QueryParser queryParser;
    Query query;
    WordEmbedding google, pubmed;


    public Searcher(String indexDirectoryPath) throws IOException, ParseException {
        setNewIndex(indexDirectoryPath);
       google = new WordEmbedding();
       google.loadModel(Path.EMBEDDINGS + "googleCorpus.bin");
       /* pubmed = new WordEmbedding();
        pubmed.loadModel(embeddingDir + "pubmed.bin");*/


    }

    public void setNewIndex(String indexDirectoryPath) throws IOException {
        Directory indexDirectory = FSDirectory.open(Paths.get(indexDirectoryPath));
        reader = DirectoryReader.open(indexDirectory);
        indexSearcher = new IndexSearcher(reader);
        indexSearcher.setSimilarity(new BM25Similarity());

        queryParser = new QueryParser(LuceneConstants.CONTENTS, new StandardAnalyzer());

        queryParser.setDefaultOperator(QueryParser.Operator.AND);


    }

    public TopDocs search(String searchQuery)
            throws IOException, ParseException {

        query = queryParser.parse(searchQuery);
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


    public String getPreviewOfSingleQuery(int docId, String filePath, String query, int ngram) throws IOException, ParseException, InvalidTokenOffsetsException {

        Formatter formatter = new SimpleHTMLFormatter();
        Analyzer analyzer = new StopAnalyzer();
        QueryScorer queryScorer = new QueryScorer(queryParser.parse(query));
        Highlighter highlighter = new Highlighter(formatter, queryScorer);
        Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer, 1);
        highlighter.setTextFragmenter(fragmenter);

        TokenStream stream = TokenSources.getTokenStream(reader, docId, LuceneConstants.HIGHLIGHT_INDEX, analyzer);
        String text = getDocumentById(docId).get(LuceneConstants.HIGHLIGHT_INDEX);
        String highlight;
        try {
            highlight = highlighter.getBestFragments(stream, text, 25)[0];
        }catch(Exception e){
            highlight = "No preview available";
        }
        return highlight;
    }
    public Integer calcIndexDistance(int docId, String[] query) throws IOException {
        ArrayList indexe = new ArrayList();
        List<List<Integer>> lst = new ArrayList<List<Integer>>();


        for (int i = 0; i < query.length; i++) {

            ArrayList tmpIndexes = getIndexPositionOfTerm(docId, query[i]);
            System.out.println(query[i]);
            System.out.println(tmpIndexes.size());
            if(tmpIndexes.size() == 0){
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

    public void cartesian(List<List<Integer>> list, int n,Integer[] tmpResult, List<List<Integer>> result) {
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