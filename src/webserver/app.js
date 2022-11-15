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
} = require('path');
const io = require("socket.io")(http, {
    cors: {
        origin: "*"
    }
});






app.use(express.static(path.join(__dirname, 'public')));



var connections = [];
var MAXSUGGESTS = 25


var holeArray = fs.readFileSync("indexData.txt", "utf-8").toString().split(",");


var startPath = "/"
var cloudList;

function confJsonData() {
    var obj = new Object();
    obj.path = startPath
    var jsonString = JSON.stringify(obj);
    return jsonString
}





//let getConf = generateJSONMessage(messageConstants.READ_CONF, "")





//socket connection to frontend and java backend 
io.sockets.on('connection', (socket) => {

    var sock = zmq.socket('req');
    var connectAddress = 'tcp://127.0.0.1:5556';
    sock.connect(connectAddress);
   
    //socket.broadcast.emit('', "")
    sock.send(generateJSONMessage("getConf"));

    sock.on('message', function(data) {
        var messageObj = JSON.parse(data)
  
        var messageBody = null
        try {
            messageBody = messageObj["body"]
        } catch {
            messageBody = ""
        }




    switch (messageObj["header"]) {

        case messageConstants.GET_DOCUMENT_LIST:
            socket.emit("docResultList", messageBody)
            break

        case messageConstants.CHANGE_CONF:
            socket.emit("stdout", messageBody)
            cloudList = messageObj["subbody"]
            break

        case messageConstants.READ_CONF:
            startPath = messageBody
            cloudList = messageObj["subbody"]
            
            break


        default:
            socket.emit("error", null);
            break
    }
})

    connections.push(socket.id);



    logToConsole('socket ' + socket.id + ' is connected -> ' + connections.length + ' client(s) active');

    socket.emit("conf", confJsonData())

    socket.on('newIndex', (path) => {
        if (fs.existsSync(path)) {
            let tmpPathObj = new Object()
            tmpPathObj.path = path

            let setNewConf = generateJSONMessage(messageConstants.CHANGE_CONF, JSON.stringify(tmpPathObj))
            sock.send(setNewConf)
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

    // get triggerd by every key input by user in frontend 
    socket.on('search', (query) => {

        let arrayWords = query.split(/[ ,]+/)

        let word = search(arrayWords)


        socket.emit("autocomplete", word)
    });


    socket.on('finalSearch', (data) => {
        let sendSearchQuery = generateJSONMessage(messageConstants.GET_DOCUMENT_LIST, data, 123)
        sock.send(sendSearchQuery)
    })



    socket.on('disconnect', () => {
        connections.pop(socket.id)
        logToConsole('socket ' + socket.id + ' is disconnected -> ' + connections.length + ' client(s) active');

        socket.disconnect(true)
        sock.disconnect(connectAddress)
        sock.close()
    })
});



function generateJSONMessage(header, body = "", subBody = "") {
    var senderObj = new Object()
    senderObj.header = header
    senderObj.body = body;
    senderObj.subBody = subBody
    return JSON.stringify(senderObj)
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
                let a = getTermsByString(term, oldResults)
                return a



            }
        }
    }
}


function getTermsByString(searchInput, oldResults) {
    var tmp = []
    var i = 0
    for (let arr of holeArray) {
        if (arr.toLowerCase().startsWith(searchInput.toLowerCase()) && i < MAXSUGGESTS) {
            tmp.push(oldResults + arr.toLocaleLowerCase())
            i++
        }
    }
    return tmp
}

// send root html file to client 
app.get('/', function(req, res) {
    res.sendFile('views/index.html', {
        root: __dirname
    })
});

http.listen(port, () => {
    logToConsole(`Example app listening on port ${port}`)
})

function logToConsole(message) {
    console.log("[" + new Date().toLocaleTimeString() + "] " + message);
}



var jsonParser = bodyParser.json()
var urlencodedParser = bodyParser.urlencoded({
    extended: false
})


app.post("/getFile", urlencodedParser, (req, res) => {
    var path = req.body.path
    res.download(path);

});



app.get("/getIndex", urlencodedParser, (req, res) => {
  
    generateJSONMessage(messageConstants.READ_CONF, "")
    res.send(startPath)
});


app.get("/getCloud", urlencodedParser, (req, res) => {
    res.send(cloudList)
});

