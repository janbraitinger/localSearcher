package lucene.searchEngine;

import org.json.JSONObject;

public class Message {

    private String header;
    private String body;

    public Message(String header, String body){
        this.header= header;
        this.body = body;
    }

    public String getMessage(){
        JSONObject messageObj = new JSONObject();
        messageObj.put("header", this.header);
        messageObj.put("body", this.body);

        return messageObj.toString();
    }



}