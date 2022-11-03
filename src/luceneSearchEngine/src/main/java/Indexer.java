import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.bytedeco.javacv.FrameFilter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.logging.Level;

public class Indexer {

    private IndexWriter writer;

    public Indexer(String indexDirectoryPath) throws IOException {

        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(Level.SEVERE);
        Directory indexDirectory = FSDirectory.open(Paths.get(indexDirectoryPath));
        CharArraySet stopWords = addStopWordArray(StopWords.stopWordArray);
        StandardAnalyzer analyzer = new StandardAnalyzer(stopWords);
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

        this.writer = new IndexWriter(indexDirectory, iwc);
    }

    public void close() throws CorruptIndexException, IOException {
        this.writer.close();
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
        }
        if(checkFileName(file).equals("txt")){
            content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        }


        BasicFileAttributes attr = Files.readAttributes(Path.of(file.getPath()), BasicFileAttributes.class);

        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String creationTime = sdf.format(attr.creationTime().toMillis());

        FieldType ft = new FieldType();
        ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS );
        ft.setStoreTermVectors( true );
        ft.setStoreTermVectorOffsets( true );
        ft.setStoreTermVectorPayloads( true );
        ft.setStoreTermVectorPositions( true );
        ft.setTokenized( true );


        TextField contentField = new TextField(LuceneConstants.CONTENTS, content, Field.Store.YES);
        TextField fileNameField = new TextField(LuceneConstants.FILE_NAME, file.getName(), TextField.Store.YES);
        TextField filePathField = new TextField(LuceneConstants.FILE_PATH, file.getCanonicalPath(), TextField.Store.YES);
        TextField creationDate = new TextField(LuceneConstants.CREATION_DATE, creationTime, Field.Store.YES);
        TextField highlight = new TextField(LuceneConstants.HIGHLIGHT_INDEX, content, Field.Store.YES);
        Field termDetails = new Field(LuceneConstants.TERM_DETAILS, content, ft);


        document.add(contentField);
        document.add(fileNameField);
        document.add(filePathField);
        document.add(creationDate);
        document.add(termDetails);
        document.add(highlight);


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

    private byte[] getPDFByteString(File pdfFile) throws IOException {
        PDDocument document = PDDocument.load(pdfFile);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        document.save(byteArrayOutputStream);
        document.close();
        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        return inputStream.readAllBytes();
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

    public int createIndex(String dataDirPath, FileFilter filter) throws IOException {
        int i=0;
        File[] files = new File(dataDirPath).listFiles();
        //if(files != null){
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
    //}
        this.writer.commit();
        return this.writer.numDocs();
    }
}