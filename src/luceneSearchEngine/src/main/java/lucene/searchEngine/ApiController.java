package lucene.searchEngine;

import io.javalin.Javalin;

public class ApiController {

    private int port = 4001;
    private Javalin endPoint;
    private Application app;
    volatile boolean semaphore;

    public ApiController(int port, Application app) {
        Console.print("API runs on Port " + this.port, 0);
        this.port = port;
        this.app = app;
        this.endPoint = Javalin.create(/*config*/).start(this.port);
        this.handleRequests();
    }


    private void handleRequests() {

        this.endPoint.get("/status", handler -> {
            new Controller(handler).getStatus();
        });


        this.endPoint.get("/search/{data}", handler -> {
            if (!semaphore) {
                new Controller(handler).search(app.getSearcher());
                return;
            }
            handler.result("semaphore error");

        });
        this.endPoint.get("/wordcloud", handler -> {
            new Controller(handler).getWordCloud(app.getSearcher());
        });


        this.endPoint.get("/conf", handler -> {
            new Controller(handler).getConf(app.getConfManager());
        });


        this.endPoint.get("/setConf/{data}", handler -> {
            if (!semaphore) {
                semaphore = true;
                new Controller(handler).setConf(app.getConfManager(), app.getSearcher(), app);
                semaphore = false;
                return;
            }
            handler.result("semaphore error");

        });
    }


}
