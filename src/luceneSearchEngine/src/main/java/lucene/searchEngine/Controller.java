package lucene.searchEngine;

import io.javalin.Javalin;

public class Controller{

    private int port;
    private Javalin app;
    public Controller(int port){
        this.port = port;
    }

    public Javalin createEndpoint(){
        app = Javalin.create(/*config*/).start(this.port);
        return app;
    }


    public String buildMessage(String header, String body){
        Message msgObj = new Message(header, body);
        return msgObj.getMessage();
    }




}
