import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.TermVectorsReader;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.bytedeco.javacv.FrameFilter;
import org.json.JSONArray;


import java.awt.print.Book;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

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
        String after = "";
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


    public String getPreviewOfSingleQuery(int docId, String filePath, String query, int ngram) throws IOException, ParseException, InvalidTokenOffsetsException {
        //todo: load file in array
        //todo: remove stopwords from file
        //todo: check array with index of getPositionOfTerms

       /* Formatter formatter = new SimpleHTMLFormatter();


        QueryScorer scorer = new QueryScorer(queryParser.parse(query));

        //used to markup highlighted terms found in the best sections of a text
        Highlighter highlighter = new Highlighter(formatter, scorer);

        //It breaks text up into same-size texts but does not split up spans
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 10);

        //breaks text up into same-size fragments with no concerns over spotting sentence boundaries.
        //Fragmenter fragmenter = new SimpleFragmenter(10);

        //set fragmenter to highlighter
        highlighter.setTextFragmenter(fragmenter);
        Analyzer analyzer = new StandardAnalyzer();

        TokenStream stream = TokenSources.getTokenStream(reader, docId, LuceneConstants.CONTENTS, analyzer);
        String text = getDocumentById(docId).get(LuceneConstants.CONTENTS);
        String[] frags = highlighter.getBestFragments(stream, text, 10);




        for (String frag : frags)f
        {
            System.out.println("=======================");
            System.out.println(frag);
        }
        */
        Formatter formatter = new SimpleHTMLFormatter();
        Analyzer analyzer = new StandardAnalyzer();
        QueryScorer queryScorer = new QueryScorer(queryParser.parse(query));
        Highlighter highlighter = new Highlighter(formatter, queryScorer);

       // highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
       ;

        Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer, 1);
        highlighter.setTextFragmenter(fragmenter);






        TokenStream stream = TokenSources.getTokenStream(reader, docId, "abc", analyzer);
        String text = getDocumentById(docId).get("abc");



         return highlighter.getBestFragments(stream, text, 25)[0];


        /*
        ArrayList<Integer> positions = getPositionOfTerms(docId, query);

        String[] stopWordArray = {"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"};

        FileReader fr = null;
        String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        StringTokenizer st = new StringTokenizer(content, " ");
        String[] fileArray = new String[st.countTokens()];


        // create ArrayList object
        List<String> elements = new ArrayList<String>();
        List<String> original = new ArrayList<String>();
        // iterate through StringTokenizer tokens
        while (st.hasMoreTokens()) {
            String yourString = st.nextToken();
            String result = yourString.replaceAll("[-+.^:,]", "");
            // add tokens to AL
            if (!elements.contains(result)) {
                elements.add(result.toLowerCase());
            }
            if (!original.contains(result)) {
                original.add(yourString);
            }


        }




        System.out.println("0: " + original.indexOf(query));
        System.out.println("1: " + elements.indexOf(query));
        System.out.println("2: " + positions.get(0));


        String a = "";
        String b = "";
        boolean _firstFlag = true;
                if (original.indexOf(query) + ngram < original.size()) {
                    for (int i = positions.get(0); i <= positions.get(0) + ngram; i++) {
                        if (_firstFlag) {
                            _firstFlag = false;
                            b += "<b>" + original.get(i) + "</b> ";

                        } else {
                            b += original.get(i) + " ";
                        }
                    }
                }

                if (elements.indexOf(query) - ngram > 0) {
                    for (int i = positions.get(0) - ngram; i < positions.get(0); i++) {

                        a += original.get(i) + " ";
                    }
                }


                return (a + b);


                */
              //  return null;

    }

    public ArrayList<Integer> getPositionOfTerms(int docId, String query) throws IOException {



        ArrayList<Integer> positonList = new ArrayList<>();
        Terms vector = null;

            vector = reader.getTermVector(docId, LuceneConstants.TERM_DETAILS);

        TermsEnum terms = vector.iterator();
        PostingsEnum positions = null;
        BytesRef term;


        while ((term = terms.next()) != null) {
            String termstr = term.utf8ToString(); // Get the text string of the term.
            String result = termstr.replaceAll("[-+.^:,]","");
            if (result.equals(query)) {
                long freq = terms.totalTermFreq(); // Get the frequency of the term in the document.
                // Here you are getting a PostingsEnum that includes only one document entry, i.e., the current document.
                positions = terms.postings(positions, PostingsEnum.POSITIONS);
                positions.nextDoc(); // you still need to move the cursor
                // now accessing the occurrence position of the terms by iteratively calling nextPosition()
               // System.out.print(termstr + " - ");
                for (int i = 0; i < freq; i++) {
                   positonList.add(positions.nextPosition());
                  //  System.out.print(positions.nextPosition() + " ");
                }
                //System.out.println("");
           }
        }
        //System.out.println("docID " + docId + ": "+ positonList);
        return positonList;


    }
}