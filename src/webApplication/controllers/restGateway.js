const axios = require('axios');
const LUCENE_ENDPOINT_PORT = 4001
const LUCENE_ENDPOINT_URL = `http://localhost:${LUCENE_ENDPOINT_PORT}/api/v1/`


module.exports.statusCheck = async () => {
    return axios.get(LUCENE_ENDPOINT_URL + "status")
        .then(response => response.data)
        .catch(() => {
            return JSON.parse(buildJSON("status", "offline"))
        })
}



module.exports.search = async (query) => {
    return axios.get(LUCENE_ENDPOINT_URL + "search/", {
        params: {
          data: query
        }
      })
      .then(response => {
        return response.data
      })
      .catch(error => {
        return buildJSON("error", error)
      });
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