package lucene.searchEngine;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Application {

    private Searcher searcher;
    private ConfManager confManager;
    private String indexerDir;
    private String autoCompletePath;
    private String configFile = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/conf.ini";
    private String dataDir;

    public Application() throws IOException, ParseException {
        Console.print("Starting Application", 0);
        this.confManager = new ConfManager(configFile);
        this.readConf();
        this.searcher = new Searcher(indexerDir);

    }


    public void setup() throws IOException {
        this.readConf();
        this.deleteIndex();
        this.createIndex();
        this.searcher.setNewIndex(indexerDir);
        this.searcher.writeIndexTerms(autoCompletePath);
    }

    private void readConf(){
        Console.print("Read conf file "+ configFile, 0);
        this.dataDir = confManager.readConf("searching", "dataPath");
        this.autoCompletePath = confManager.readConf("index", "autocomplete");
        this.indexerDir = confManager.readConf("index", "indexerDir");
    }

    private String createIndex() throws IOException {
        Console.print("Create index under "+ indexerDir, 0);
        Indexer indexer = new Indexer(indexerDir);

        long startTime = System.currentTimeMillis();
        int numIndexed = indexer.createIndex(this.dataDir, new TextFileFilter());
        //Console.print("dirPath is " + this.confManager.readConf("searching", "dataPath"), 0);
        long endTime = System.currentTimeMillis();

        indexer.close();

        String consoleMessage = numIndexed + " file(s) indexed, time taken: " + (endTime - startTime) + " ms";
        Console.print(consoleMessage, 0);
        return consoleMessage;
    }

    private void deleteIndex() {
        Arrays.stream(new File(this.indexerDir).listFiles()).forEach(File::delete);
    }

    public Searcher getSearcher(){
        return this.searcher;
    }

    public ConfManager getConfManager(){
        return this.confManager;
    }


}
