const express = require('express');
const app = express();
const path = require('path')
const PORT = process.env.PORT || 3000;
const route = require('./routes/basicRoute');
const http = require("http").createServer(app);
var io = require('socket.io')(http);

app.set('io', io);
require('./routes/socket')(io);

app.use(express.json())
app.use(express.urlencoded({ extended: false }));

app.use(express.static(path.join(__dirname, 'public')));
app.use('/', route);


http.listen(PORT, () => {
    console.log(`Example app listening on port ${PORT}`)
})