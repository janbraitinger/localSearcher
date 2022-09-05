import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

public class Indexer {

    private IndexWriter writer;

    public Indexer(String indexDirectoryPath) throws IOException {
        Directory indexDirectory =
                FSDirectory.open(Paths.get(indexDirectoryPath));


        String[] stopWordArray = {"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now"};
        CharArraySet stopWords = addStopWordArray(stopWordArray);

        StandardAnalyzer analyzer = new StandardAnalyzer(stopWords);
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(indexDirectory, iwc);
    }

    public void close() throws CorruptIndexException, IOException {
        writer.close();
    }

    public CharArraySet addStopWordArray(String[] array) {
        CharArraySet stopWords = new CharArraySet(0, false);
        for (String i : array) {
            stopWords.add(i);
        }


        return stopWords;
    }

    private Document getDocument(File file) throws IOException {
        Document document = new Document();

        //System.out.println(new File(file.getCanonicalPath()).lastModified());

        TextField contentField = new TextField(LuceneConstants.CONTENTS, new FileReader(file));
        TextField authorField = new TextField("author", "Jan Braitinger", TextField.Store.YES);
        TextField fileNameField = new TextField(LuceneConstants.FILE_NAME, file.getName(), TextField.Store.YES);
        TextField filePathField = new TextField(LuceneConstants.FILE_PATH, file.getCanonicalPath(), TextField.Store.YES);

        document.add(authorField);
        document.add(contentField);
        document.add(fileNameField);
        document.add(filePathField);
        //System.out.println(document.getFields());
        return document;
    }

    private void indexFile(File file) throws IOException {
        System.out.println("Indexing " + file.getCanonicalPath());
        Document document = getDocument(file);
        writer.addDocument(document);
    }

    public int createIndex(String dataDirPath, FileFilter filter)
            throws IOException {
        File[] files = new File(dataDirPath).listFiles();


        for (File file : files) {
            if (!file.isDirectory()
                    && !file.isHidden()
                    && file.exists()
                    && file.canRead()
                    && filter.accept(file)
            ) {

                indexFile(file);
            }
        }
        return writer.numRamDocs();
    }
}