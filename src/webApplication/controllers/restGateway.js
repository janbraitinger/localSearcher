const axios = require('axios');
const LUCENE_ENDPOINT_PORT = 4001
const LUCENE_ENDPOINT_URL = `http://localhost:${LUCENE_ENDPOINT_PORT}/`


module.exports.statusCheck = async () => {
    return axios.get(LUCENE_ENDPOINT_URL + "status")
        .then(response => response.data)
        .catch(() => {
            return JSON.parse(buildJSON("status", "offline"))
        })
}



module.exports.search = async (query) => {
    let tmpClientOb = JSON.parse(query)
    //tmpClientOb.query = query.replace(/ /g, "&");
    //let searchMsg = buildJSON("search", JSON.stringify(tmpClientOb))
    return axios.get(LUCENE_ENDPOINT_URL + "search/" + JSON.stringify(tmpClientOb))
        .then(response => response.data)
        .catch(() => {
            return buildJSON("result", "error")
        })
}

module.exports.wordCloud = async () => {
    return axios.get(LUCENE_ENDPOINT_URL + "wordcloud")
        .then(response => response.data)
        .catch(() => {
            return buildJSON("wordcloud", "error")
        })
}


module.exports.getConfData = async () => {
    return axios.get(LUCENE_ENDPOINT_URL + "conf")
        .then(response => response.data)
        .catch(() => {
            return buildJSON("conf", "error")
        })
}



module.exports.reIndex = async (url) => {
    url = url.replaceAll('/', '-');
    let searchMsg = buildJSON("reindex", url)
    return axios.get(LUCENE_ENDPOINT_URL + "setConf/" + searchMsg)
        .then(response => response.data)
        .catch(() => {
            console.log("error")
            return buildJSON("reindex", "error")
        })
}




function buildJSON(header, body, subbody = "") {
    let tmpObj = new Object();
    tmpObj.header = header;
    tmpObj.body = body;
    tmpObj.subbody = subbody;
    return JSON.stringify(tmpObj);
}