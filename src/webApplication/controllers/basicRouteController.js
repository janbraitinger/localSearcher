
module.exports.basicRoute = (req, res) => {
    res.sendFile('index.html', {
        root: "./views"
    });
}



module.exports.downloadFile = (req, res) => {
    var path = req.body.path
    res.download(path);
}