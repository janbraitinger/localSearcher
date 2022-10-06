import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
        String content = null;
        if(checkFileName(file).equals("pdf")) {
            content = getText(file);
        } if(checkFileName(file).equals("txt")){
            content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        }





        TextField contentField = new TextField(LuceneConstants.CONTENTS, content, Field.Store.YES);
        TextField authorField = new TextField("author", "Jan Braitinger", TextField.Store.YES);
        TextField fileNameField = new TextField(LuceneConstants.FILE_NAME, file.getName(), TextField.Store.YES);
        TextField filePathField = new TextField(LuceneConstants.FILE_PATH, file.getCanonicalPath(), TextField.Store.YES);
        BasicFileAttributes attr = Files.readAttributes(Path.of(file.getPath()), BasicFileAttributes.class);

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String creationTime = sdf.format(attr.creationTime().toMillis());
        TextField creationDate = new TextField(LuceneConstants.CREATION_DATE, creationTime, Field.Store.YES);



        FieldType ft = new FieldType();
        ft.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS );
        ft.setStoreTermVectors( true );
        ft.setStoreTermVectorOffsets( true );
        ft.setStoreTermVectorPayloads( true );
        ft.setStoreTermVectorPositions( true );
        ft.setTokenized( true );

        //document.add(new Field(LuceneConstants.TERM_DETAILS, content, FieldType.LegacyNumericType.DOUBLE), ft));


        document.add(new Field(LuceneConstants.TERM_DETAILS, new String(Files.readAllBytes(Path.of(file.getPath())), "UTF-8"), ft));
        document.add(new TextField(LuceneConstants.HIGHLIGHT_INDEX, new String(Files.readAllBytes(Path.of(file.getPath()))), Field.Store.YES));

        document.add(authorField);
        document.add(contentField);
        document.add(fileNameField);
        document.add(filePathField);
        document.add(creationDate);


        return document;
    }

    private String checkFileName(File file){
        String fileName = file.getName();
        int index = fileName.lastIndexOf('.');
        if(index > 0) {
            String extension = fileName.substring(index + 1);
            return extension;
        }
        return "";
    }

    private String getText(File pdfFile) throws IOException {
        PDDocument doc = PDDocument.load(pdfFile);
        String result = new PDFTextStripper().getText(doc);
        doc.close();
        return result;
    }

    private void indexFile(File file) throws IOException {
        Console.print("Indexing " + file.getCanonicalPath(),0);
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