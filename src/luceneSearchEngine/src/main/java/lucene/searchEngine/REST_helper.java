package lucene.searchEngine;


class Response {
    private String key;
    private Object data;

    public Response(String key, Object data) {
        this.key = key;
        this.data = data;
    }

    public String getKey() {
        return key;
    }

    public Object getData() {
        return data;
    }
}

