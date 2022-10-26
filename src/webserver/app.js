var messageConstants = require('./constants');



const express = require('express')
const path = require('path')
const app = express()
const port = 3000
const zmq = require("zeromq")
const http = require("http").createServer(app);
var fs = require('fs');
var bodyParser = require('body-parser')
const {
    constants
} = require('buffer')
const internal = require('stream')
const {
    parse
} = require('path')
const io = require("socket.io")(http, {
    cors: {
        origin: "*"
    }
});


/* set origin path */
app.use(express.static(path.join(__dirname, 'public')));


var googleWords = []
var pubMedWords = []
var indexCorpus = []
const connections = [];
var MAXSUGGESTS = 25


var holeArray = fs.readFileSync("indexData.txt", "utf-8").toString().split(",");


var startPath = ""

function confJsonData() {
    var obj = new Object();
    obj.path = startPath
    var jsonString = JSON.stringify(obj);
    return jsonString
}

/* socket connection to frontend and java backend */
io.sockets.on('connection', (socket) => {
    connections.push(socket);
    logToConsole('socket ' + socket.id + ' is connected. ' + connections.length + ' active');

    socket.emit("conf", confJsonData())

    socket.on('newIndex', (path) => {
        console.log("+++" + path)
        if (fs.existsSync(path)) {
            let tmpPathObj = new Object()
            tmpPathObj.path = path

            let setNewConf = generateJSONMessage(messageConstants.CHANGE_CONF, JSON.stringify(tmpPathObj))
            middlewear(setNewConf, socket)
            startPath = path
        
            var array = []
            try {
                fs.readdirSync(startPath).forEach(file => {
                    array.push(file)
                });
            } catch (error) {
                socket.emit("stdout", "folder error")
                return
            }

      
            socket.emit("stdout", JSON.stringify(array))
            return
        }
        socket.emit("stdout", "folder not found")

    })

    /* get triggerd by every key input by user in frontend */
    socket.on('search', (query) => {

        let arrayWords = query.split(/[ ,]+/)

        let word = search(arrayWords)
        
    
        socket.emit("autocomplete", word)
    });


    socket.on('finalSearch', (data) => {
        let sendSearchQuery = generateJSONMessage(messageConstants.GET_DOCUMENT_LIST, data)
        middlewear(sendSearchQuery, socket)
    })



    socket.on('disconnect', () => {
        connections.pop(socket)
        logToConsole('socket ' + socket.id + ' is disconnected. ' + connections.length + ' active');
    })
});



function generateJSONMessage(header, body, subBody=""){
    var senderObj = new Object()
    senderObj.header = header
    senderObj.body  = body;
    senderObj.body.subBody = subBody
    return JSON.stringify(senderObj)
}

/* get final search query, send it to java backend and wait for anwser */
async function middlewear(para, socket = null) {
 
    const sock = zmq.socket("req");
    sock.connect("tcp://127.0.0.1:5556")
    var words = para.split(/\W+/).filter(function(token) {
        return token.toLowerCase();
    });

 

    console.log("send: " + para)
    await sock.send(para)

    sock.on('message', function(data) {
        var messageObj = JSON.parse(data)
        var messageBody = messageObj["body"]
 

        switch (messageObj["header"]) {
            
            case messageConstants.GET_DOCUMENT_LIST:
                socket.emit("docResultList", messageBody)
                break

            case messageConstants.CHANGE_CONF:
                socket.emit("stdout", messageBody)
                break

            case messageConstants.READ_CONF:
                startPath = messageBody
                break

            default:
                socket.emit("error", null);
                break
        }
    });
}


var search = (query) => {
    let j = 0
    var oldResults = ""
    for (let i = 0; i < query.length - 1; i++) {
        oldResults += query[i] + " "
        
    }
    let succestions = []
    let checkDoubleWords = []
    for (let arr of holeArray) {
        let term = query[query.length - 1]
        if (arr.toLowerCase().startsWith(term.toLowerCase()) && term != "") {
            if (!checkDoubleWords.includes(arr.toLocaleLowerCase())) {
                checkDoubleWords.push(arr.toLocaleLowerCase())
                succestions.push(oldResults + arr.toLocaleLowerCase())
                return succestions
        
                
            }
        }
    }
}


/* send root html file to client */
app.get('/', function(req, res) {
    res.sendFile('views/index.html', {
        root: __dirname
    })
});

/* run server*/
http.listen(port, () => {
    logToConsole(`Example app listening on port ${port}`)
})

/* log helper */
function logToConsole(message) {
    console.log("[" + new Date().toLocaleTimeString() + "] " + message);
}


var test = "/Users/janbraitinger/Documents/Studium/Sommersemester2022/Masterarbeit/Implementierung/dumpData/DocumentD.txt"
app.get("/test", (req, res) => {

});


var jsonParser = bodyParser.json()
var urlencodedParser = bodyParser.urlencoded({
    extended: false
})


app.post("/getFile", urlencodedParser, (req, res) => {
    var path = req.body.path
    res.download(path);

});


function getFirstDirPath(){
    _flag = false
    if (!_flag) {
        let getConf = generateJSONMessage(messageConstants.READ_CONF, "")
        middlewear(getConf, null)
        _flag = true
    }
}



app.get("/getIndex", urlencodedParser, (req, res) => {
    res.send(startPath)
});


getFirstDirPath()