const {
    autocomplete,
    readAutocompleteFile
} = require('../controllers/autocomplete');
const {
    statusCheck,
    search,
    wordCloud,
    getConfData,
    reIndex
} = require('../controllers/restGateway');

var wordCloudData = "";
var confData = "/";
var luceneStatusFlag = false;

module.exports = (io) => {
    io.on('connection', socket => {
        console.log(socket.id)
        socket.on('search', (input) => {
            let suggestions = autocomplete(input)
            socket.emit("autocomplete", suggestions)
        });
        socket.on('finalSearch', async (searchQuery) => {
            let searchResult = await search(searchQuery)
            socket.emit("docResultList", searchResult)
        });

        socket.on('getCloud', async (searchQuery) => {
            let wordCloudData = await wordCloud();
            socket.broadcast.emit("wordcloud", wordCloudData)
        });

        socket.on("reIndex", async (data) => {
            let stdout = await reIndex(data)
            if(stdout != "error"){
                let wordCloudData = await wordCloud();
                let confData = await getConfData();
                socket.broadcast.emit("wordcloud", wordCloudData)
                console.log("reindex")
                socket.broadcast.emit("getconf", confData)
                readAutocompleteFile()
                socket.emit("stdout", "indexing done in " + stdout.body)
            }

        });

        (async function() {
            if(luceneStatusFlag){
                wordCloudData = await wordCloud();
                confData = await getConfData();
            }

            socket.emit("wordcloud", wordCloudData)
            socket.emit("getconf", confData)
        })();


    });




    // luceneStatus
    setInterval(async () => {
        let luceneStatus = await statusCheck()
        body = luceneStatus.body
        if(body == "online"){
            luceneStatusFlag = true
        }else{
            luceneStatusFlag = false
        }
        io.emit("luceneStatus", body);
    }, 2000);

}