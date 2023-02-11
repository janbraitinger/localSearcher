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

            var searchQueryTerm = JSON.parse(searchQuery)["query"]

            var termList = getTermArray()

            var tokens = searchQueryTerm.split(" ");

            var mainList = []
            for (var i = 0; i < tokens.length; i++) {
                var subList = []
                if(termList.includes(tokens[i])){
                    subList.push(tokens[i])
                    mainList.push(subList)
                    continue
                }
                for (let term of termList) {
                    let distance = levenshtein.get(tokens[i], term); 
                    if(distance<2){
                        subList.push(term)
                    }
                }
                mainList.push(subList)
                
            }

            


          

            let lengthOfResultList = searchResult.data.length
            console.log(lengthOfResultList)


            if(lengthOfResultList == 1){


                console.log(mainList)
                let alternatives = JSON.stringify(mainList)
                //if(doubleCheckList.length > 0){
                    socket.emit("didYouMean", alternatives)
                //}
              
            }
           

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