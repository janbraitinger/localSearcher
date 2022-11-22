const axios = require('axios');
const LUCENE_ENDPOINT_PORT = 4001
const LUCENE_ENDPOINT_URL = `http://localhost:${LUCENE_ENDPOINT_PORT}/`


module.exports.status = (para) => {
    console.log("status abfrage")
    axios.get(LUCENE_ENDPOINT_URL + "status", {})
        .then(function(response) {
            console.log("active")
        })
        .catch(function(error) {
            console.log("error")
        });
}

module.exports.getGateway = (destination) => {
    axios.get(LUCENE_ENDPOINT_URL + destination, {})
        .then(function(response) {
            console.log(response.data);
        })
        .catch(function(error) {
            console.log(error);
        });

}