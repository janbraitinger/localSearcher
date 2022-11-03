


module.exports = class BackEndSocket {



    constructor(target) {
        this.zmq = require("zeromq")
        this.socket = this.zmq.socket("req");
        this.socket.connect(target)
     
 
    }


    sendMessage(header, body = "") {
        if(header.length>0){
        let senderObj = new Object()
        senderObj.header = header
        senderObj.body = body;
        let message = JSON.stringify(senderObj)
        this.socket.send(message)
        }
    }

    isHeartBeat(data){
        let message = JSON.parse(data)
        if(message.header === 'ping'){
            return true
        }
        return false
    }





}

function test(){
    console.log(foo)
}