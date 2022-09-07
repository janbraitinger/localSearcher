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

/* read embeddings content for suggestions */
/*var pubMedArray = fs.readFileSync('pubmedWords.txt').toString().split(",");
var googleArray = fs.readFileSync('googleWords.txt').toString().split(",");


for (i in pubMedArray) {
   pubMedWords.push(pubMedArray[i]);
}
for (i in googleArray) {
   googleWords.push(googleArray[i]);
}*/
//var indexFile  = fs.readFileSync('indexData.txt').toString().split(",");
var holeArray = fs.readFileSync("indexData.txt", "utf-8").toString().split(",");

//var holeArray = pubMedArray.concat(googleArray);
//var holeArray = indexCorpus


/* ----------- */

var tmpStartPath = ""

function confJsonData() {
    var obj = new Object();
    obj.path = tmpStartPath
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


            middlewear("1" + JSON.stringify(tmpPathObj), socket)
            tmpStartPath = path
            //confJsonData().path = path
            var array = []
            try {
                fs.readdirSync(tmpStartPath).forEach(file => {
                    array.push(file)
                });
            } catch (error) {
                socket.emit("stdout", "folder error")
                return
            }
    
            //console.log(JSON.stringify(array.Path));
            socket.emit("stdout", JSON.stringify(array))
            return
        }
        socket.emit("stdout", "folder not found")

    })

    /* get triggerd by every key input by user in frontend */
    socket.on('search', (query) => {
        let arrayWords = query.split(/[ ,]+/)
        let word = search(arrayWords)
        //console.log(word)
        socket.emit("autocomplete", word)
    });

/* socket.on('checkIndex', () => {
        var array = []
        try {
            fs.readdirSync(tmpStartPath).forEach(file => {
                array.push(file)
            });
        } catch (error) {
            socket.emit("stdout", "folder error")
            return
        }

        //console.log(JSON.stringify(array.Path));
        socket.emit("docResultList", JSON.stringify(array))
    })*/


    /* receive final search query */
    socket.on('finalSearch', (data) => {
        middlewear("0" + data, socket)
    })



    socket.on('disconnect', () => {
        connections.pop(socket)
        logToConsole('socket ' + socket.id + ' is disconnected. ' + connections.length + ' active');
    })
});




/* get final search query, send it to java backend and wait for anwser */
async function middlewear(para, socket=null) {
   console.log()
    const sock = zmq.socket("req");
    sock.connect("tcp://127.0.0.1:5555")
    var words = para.split(/\W+/).filter(function(token) {
        return token.toLowerCase();

    });
    console.log("send: " + para)
    await sock.send(para)


    sock.on('message', function(data) {
        var substring = data.toString('utf8').substring(1)
        logToConsole('got messsage: ' + data.toString('utf8'));
        //logToConsole(substring)
        //logToConsole(data.toString('utf8')[0])
        var operator = data.toString('utf8')[0]
        switch (parseInt(operator)) {
            case 0:
                socket.emit("docResultList", substring)
                // console.log(0)
                break
            case 1:
                socket.emit("stdout", substring) // time 
                console.log("stdout" + substring)
                break

                case 2:
                  console.log("path")
                  tmpStartPath = substring
                  break
            default:
                //console.log("other")
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
                j++
                if (j > MAXSUGGESTS) {
                    return succestions

                }
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


_flag = false
if(!_flag){
   middlewear("2", null)
   _flag = true
}


app.get("/getIndex", urlencodedParser, (req, res) => {
   console.log("test")
   console.log(tmpStartPath)
   res.send(tmpStartPath)

});