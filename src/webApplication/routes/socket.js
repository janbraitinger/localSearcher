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
var _coldStart = true;

module.exports = (io) => {
    io.on('connection', socket => {
        console.log(socket.id)
   


        socket.on('search', (input) => {
            let suggestions = autocomplete(input)
            socket.emit("autocomplete", suggestions)
        });

        socket.on('finalSearch', async (searchQuery) => {
            let searchResult = await search(searchQuery)
            socket.emit("docResultList", searchResult.data)
        });

        socket.on('getCloud', async (searchQuery) => {
            if (_coldStart) {
                wordCloudData = await wordCloud();
                wordCloudData = wordCloudData.data;
                _coldStart = false;
            }
            let length = (wordCloudData.data) ? Object.keys(wordCloudData.data).length : 0;
        
            if (length === 0) {
                wordCloudData = await wordCloud();
                wordCloudData = wordCloudData.data;
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
            }

        });

        (async function() {
            if (luceneStatusFlag) {
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
        body = luceneStatus.data
        if (body == "online") {
            luceneStatusFlag = true
        } else {
            luceneStatusFlag = false
        }
        io.emit("luceneStatus", body);
    }, 2000);

}