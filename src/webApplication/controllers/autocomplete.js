const fs = require('fs');
const MAXSUGGESTS = 25
var holeArray = fs.readFileSync("../indexData.txt", "utf-8").toString().split(",");


module.exports.getTermArray = () => {
    if(holeArray !== undefined) {
        return holeArray;
    }
    this.readAutocompleteFile()
    return holeArray;
}


module.exports.readAutocompleteFile = () => {
    holeArray = fs.readFileSync("../indexData.txt", "utf-8").toString().split(",");
}



module.exports.autocomplete = (input) => {
    var query = input.split(/[ ,]+/)
    let j = 0
    var oldResults = ""
    for (let i = 0; i < query.length - 1; i++) {
        oldResults += query[i] + " "

    }
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