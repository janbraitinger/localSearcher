const bodyParser = require('body-parser')
module.exports.basicRoute = (req, res) => {
    res.sendFile('index.html', {
        root: "./views"
    });
}



module.exports.downloadFile = (req, res) => {
    console.log(req.body)
    console.log("download controller")
    var path = req.body.path
    res.download(path);
}