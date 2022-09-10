import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.TermVectorsReader;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.json.JSONArray;


import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

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
        google.loadModel(embeddingDir+"googleCorpus.bin");
        pubmed = new WordEmbedding();
        pubmed.loadModel(embeddingDir+"pubmed.bin");


    }

    public void setNewIndex(String indexDirectoryPath) throws IOException {

        Directory indexDirectory = FSDirectory.open(Paths.get(indexDirectoryPath));
        System.out.println(indexDirectory);
        reader = DirectoryReader.open(indexDirectory);
        indexSearcher = new IndexSearcher(reader);
        queryParser = new QueryParser(LuceneConstants.CONTENTS, new StandardAnalyzer());
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
/*
    public void getQueryCountOfDoc(int docid) throws IOException {
        TopDocs results = indexSearcher.search(query, LuceneConstants.MAX_SEARCH); // or whatever you need instead of 100
        ScoreDoc[] hits = results.scoreDocs;

        for (ScoreDoc hit : hits) {
            getExplanation(indexSearcher, query, hit.doc);
        }
    }
*/
/*
    public void writeIndexTerms() {
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

    public void printHits(String searchQuery) throws IOException, ParseException {

        Query query = queryParser.parse(searchQuery);
        TopDocs results = indexSearcher.search(query, 100); // or whatever you need instead of 100
        ScoreDoc[] hits = results.scoreDocs;
        for (ScoreDoc hit : hits) {
            getExplanation(indexSearcher, query, hit.doc);
        }
    }*/

    public String getExplanation(String _query, int docID) throws IOException, ParseException {
        Query query = queryParser.parse(_query);
        Explanation explanation = indexSearcher.explain(query, docID);
        //System.out.println(explanation.getDescription()); // do what you need with this data
        return explanation.toString();     // do what you need with this data
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


    public ArrayList<Integer> getPositionOfTerms(int docId, String query) throws IOException {
        ArrayList<Integer> positonList = new ArrayList<>();
        Terms vector = reader.getTermVector(docId, LuceneConstants.TERM_DETAILS);
        TermsEnum terms = vector.iterator();
        PostingsEnum positions = null;
        BytesRef term;
        System.out.println(docId);
        while ((term = terms.next()) != null) {
            String termstr = term.utf8ToString(); // Get the text string of the term.
            if (termstr.equals(query)) {
                long freq = terms.totalTermFreq(); // Get the frequency of the term in the document.
                // Here you are getting a PostingsEnum that includes only one document entry, i.e., the current document.
                positions = terms.postings(positions, PostingsEnum.POSITIONS);
                positions.nextDoc(); // you still need to move the cursor
                // now accessing the occurrence position of the terms by iteratively calling nextPosition()
                for (int i = 0; i < freq; i++) {
                    //System.out.println(positions.nextPosition());
                    positonList.add(positions.nextPosition());
                }


            }

        }
        return positonList;
    }


}