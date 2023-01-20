package lucene.searchEngine;

import io.javalin.Javalin;

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
        this.handleRequests();
    }


    private void handleRequests() {

        this.endPoint.get("/api/v1/status", handler -> {
            new Controller(handler).getStatus();
            return;

        });

        this.endPoint.get("/api/v1/search", handler -> {
            if (!this.lock) {
                new Controller(handler).search(app.getSearcher());
                return;
            }
            handler.json(new Response("error", "please try again later"));

        });

        this.endPoint.get("/api/v1/wordcloud", handler -> {
            if (!this.lock) {
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


        this.endPoint.get("/api/v1/setConf/{data}", handler ->
        {
            if (!this.lock) {
                synchronized (this) {
                    this.lock = true;
                    System.out.println("was nun");
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
