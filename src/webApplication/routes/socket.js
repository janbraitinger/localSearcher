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

        socket.on("reIndex", async (data) => {
            let stdout = await reIndex(data)
            console.log(stdout)
            if(stdout != "error"){
                let wordCloudData = await wordCloud();
                let confData = await getConfData();
                socket.emit("stdout", stdout)
                socket.broadcast.emit("wordcloud", wordCloudData)
                socket.broadcast.emit("getconf", confData)
                readAutocompleteFile()
            }

        });

        (async function() {
            let wordCloudData = await wordCloud();
            let confData = await getConfData();
            socket.emit("wordcloud", wordCloudData)
            socket.emit("getconf", confData)
        })();


    });




    // luceneStatus
    setInterval(async () => {
        let luceneStatus = await statusCheck()
        body = luceneStatus.body
        io.emit("luceneStatus", body);
    }, 2000);

}