package lucene.searchEngine;


import org.apache.lucene.queryparser.classic.ParseException;
import java.io.IOException;



public class Main {

    private final int PORT = 4001;

    public Main() throws IOException, ParseException {
        Application app = new Application();
        app.setup();
        new ApiController(PORT, app);

    }

    public static void main(String[] args) throws IOException, ParseException {
        new Main();
    }


}