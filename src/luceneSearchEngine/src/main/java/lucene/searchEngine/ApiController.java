package lucene.searchEngine;

import io.javalin.Javalin;
import org.bytedeco.javacv.FrameFilter;

public class ApiController {

    private int port = 4001;
    private Javalin endPoint;
    private Application app;
    volatile boolean lock = false;

    public ApiController(int port, Application app) {
        Console.print("API runs on Port " + this.port, 0);
        this.port = port;
        this.app = app;
        this.endPoint = Javalin.create(/*config*/).start(this.port);
        try {
            this.handleRequests();
        }catch (Exception e){
            System.err.println(e);
        }
    }


    private void handleRequests() {

        this.endPoint.get("/api/v1/status", handler -> {
            new Controller(handler).getStatus();
            return;

        });

        this.endPoint.get("/api/v1/search", handler -> {
            if (!this.lock) {
                try {
                    new Controller(handler).search(app.getSearcher());
                }catch(Exception e){
                    handler.json(new Response("error", "please try again later"));
                }
                return;
            }
            handler.json(new Response("error", "please try again later"));

        });

        this.endPoint.get("/api/v1/wordcloud", handler -> {
            if (!this.lock) {
                System.out.println("Debug - Word Cloud Request");
                new Controller(handler).getWordCloud(app.getSearcher());
                return;
            }
            handler.json(new Response("error", "please try again later"));
        });


        this.endPoint.get("/api/v1/conf", handler -> {
            if (!this.lock) {
                new Controller(handler).getConf(app.getConfManager());
                return;
            }
            handler.json(new Response("error", "please try again later"));
        });


        this.endPoint.get("/api/v1/information", handler -> {
                new Controller(handler).getInfo(this.app.getIndexedDocuments());
        });


        this.endPoint.get("/api/v1/setConf/{data}", handler ->
        {
            if (!this.lock) {
                synchronized (this) {
                    this.lock = true;
                    new Controller(handler).setConf(app.getConfManager(), app.getSearcher(), app);
                    System.out.println("done with setting up new path");
                    this.lock = false;
                    return;
                }
            }
            handler.json(new Response("error", "please try again later"));
        });
    }






}
