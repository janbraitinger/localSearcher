var levenshtein = require('fast-levenshtein');

const {
    autocomplete,
    readAutocompleteFile,
    getTermArray
} = require('../controllers/autocomplete');
const {
    statusCheck,
    search,
    wordCloud,
    getConfData,
    reIndex,
    getInformation
} = require('../controllers/restGateway');

var wordCloudData = "";
var confData = "/";
var luceneStatusFlag = false;
var _coldStart = true;
var indexedDocuments = getInformation();;

module.exports = (io) => {
    io.on('connection', socket => {
        console.log(socket.id)
        readAutocompleteFile()



        socket.on('search', (input) => {
            let suggestions = autocomplete(input)
            socket.emit("autocomplete", suggestions)
        });

        socket.on('finalSearch', async (searchQuery) => {
            var searchResult = await search(searchQuery)

            if (searchResult.data.length == 1) {
                console.log("no results found")
                var searchQueryTerm = JSON.parse(searchQuery)["query"]
                var termList = getTermArray()
                var tokens = searchQueryTerm.split(" ");
                var mainList = []
                console.log("generating suggestions")
                for (var i = 0; i < tokens.length; i++) {
                    var subList = []
                    if (termList.includes(tokens[i])) {
                        subList.push(tokens[i])
                        mainList.push(subList)
                        continue
                    }
                    for (let term of termList) {
                        let distance = levenshtein.get(tokens[i], term);
                        if (distance < 3) {
                            if (!subList.includes(term)) {
                                subList.push(term)
                            }

                        }
                    }
                    mainList.push(subList)

                }
                try {
                    var alternatives = JSON.stringify(mainList)
                    console.log(alternatives)
                    if (mainList.length == 0) {
                        socket.emit("didYouMean", "empty")
                        return
                    }
                    socket.emit("didYouMean", alternatives)

                } catch {

                }

            }

            socket.emit("docResultList", searchResult.data)
        });

        socket.on('getCloud', async (searchQuery) => {

            try {
                if (_coldStart) {
                    wordCloudData = await wordCloud();
                    if (wordCloudData.body == "error") {
                        socket.broadcast.emit("wordcloud", "less");
                        return
                    }
                    wordCloudData = wordCloudData.data;

                    _coldStart = false;
                }
                let length = (wordCloudData.data) ? Object.keys(wordCloudData.data).length : 0;

                if (length === 0) {
                    wordCloudData = await wordCloud();
                    wordCloudData = wordCloudData.data;
                }
            } catch {
                wordCloudData = "empty"
            }




            socket.broadcast.emit("wordcloud", wordCloudData);
        });


        socket.on("reIndex", async (data) => {
            let stdout = await reIndex(data)
            if (stdout != "error") {
                wordCloudData = await wordCloud();
                wordCloudData = wordCloudData.data
                confData = await getConfData();
                confData = confData.data
                socket.broadcast.emit("wordcloud", wordCloudData)
                socket.broadcast.emit("getconf", confData)
                readAutocompleteFile()
                socket.emit("stdout", stdout.body)
                indexedDocuments = await getInformation();
                socket.emit("indexedDocuments", indexedDocuments)
            }

        });

        (async function() {
            if (luceneStatusFlag) {
                wordCloudData = await wordCloud();
                confData = await getConfData();
                indexedDocuments = await getInformation();
            }

            socket.emit("indexedDocuments", indexedDocuments)
            socket.emit("wordcloud", wordCloudData)
            socket.emit("getconf", confData)
        })();


    });




    // luceneStatus
    setInterval(async () => {
        let luceneStatus = await statusCheck()
        body = luceneStatus.data
        if (body == "online") {
            luceneStatusFlag = true
        } else {
            luceneStatusFlag = false
        }
        io.emit("luceneStatus", body);
    }, 2000);

}