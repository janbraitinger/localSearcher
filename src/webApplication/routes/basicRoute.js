const express = require('express');
const router = express.Router();
const {
    basicRoute, downloadFile
} = require('../controllers/basicRouteController');




router.get('/', basicRoute);
router.post('/getFile', downloadFile);



module.exports = router;