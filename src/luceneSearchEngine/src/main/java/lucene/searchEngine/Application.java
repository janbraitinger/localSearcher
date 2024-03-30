package lucene.searchEngine;

import org.apache.lucene.queryparser.classic.ParseException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Application {

    private int indexedDocuments;
    private Searcher searcher;
    private ConfManager confManager;
    private String indexerDir;
    private String autoCompletePath;
    private String configFile = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/conf.ini";
    private String dataDir;
    private boolean coldstart = true;

    public Application() throws IOException, ParseException {
        Console.print("Starting Application", 0);
        this.confManager = new ConfManager(configFile);

    }


    public int getIndexedDocuments(){
        System.out.println("Debug - Get indexed docuemnts. Result: " + this.indexedDocuments);
        return this.indexedDocuments;
    }


    public void setup() throws IOException, ParseException {
        this.readConf();
        Console.print("SetUp starts", 0);
        this.deleteIndex();
        this.indexedDocuments = this.createIndex();
        if(coldstart){
            this.searcher = new Searcher(indexerDir);
            coldstart = false;
        }

        this.searcher.setNewIndex(indexerDir);
        this.searcher.writeIndexTerms(autoCompletePath);
        Console.print("SetUp is done", 0);
    }

    private void readConf(){
        Console.print("Read conf file "+ configFile, 0);
        this.dataDir = confManager.readConf("searching", "dataPath");
        this.autoCompletePath = confManager.readConf("index", "autocomplete");
        this.indexerDir = confManager.readConf("index", "indexerDir");
        Console.print("Path which contains the documents is "+ dataDir, 0);
        Console.print("The index file is saved under "+ indexerDir, 0);
    }

    private int createIndex() throws IOException {
        Console.print("Create index under "+ indexerDir, 0);
        Indexer indexer = new Indexer(indexerDir);
        long startTime = System.currentTimeMillis();
        int numIndexed = indexer.createIndex(this.dataDir, new TextFileFilter());
        //Console.print("dirPath is " + this.confManager.readConf("searching", "dataPath"), 0);
        long endTime = System.currentTimeMillis();
        indexer.close();
        String consoleMessage = numIndexed + " file(s) indexed, time taken: " + (endTime - startTime) + " ms";
        Console.print(consoleMessage, 0);
        return numIndexed;
    }

    private void deleteIndex() {
        Console.print("Deleting old indexes", 0);
        try {
            Arrays.stream(new File(this.indexerDir).listFiles()).forEach(File::delete);
        }catch (Exception e){
            Console.print("Deleting index file", 2);
        }
        }

    public Searcher getSearcher(){
        return this.searcher;
    }

    public JSONObject getInfo(){
        return null;
    }

    public ConfManager getConfManager(){
        return this.confManager;
    }


}
