const fs = require('fs');
const MAXSUGGESTS = 25
var indexedTermsArray = fs.readFileSync("../indexData.txt", "utf-8").toString().split(",");


module.exports.getTermArray = () => {
    if(indexedTermsArray !== undefined) {
        return indexedTermsArray;
    }
    this.readAutocompleteFile()
    return indexedTermsArray;
}


module.exports.readAutocompleteFile = () => {
    indexedTermsArray = fs.readFileSync("../indexData.txt", "utf-8").toString().split(",");
}



module.exports.autocomplete = (input) => {
    var query = input.split(/[ ,]+/)
    let j = 0
    var oldResults = ""
    for (let i = 0; i < query.length - 1; i++) {
        oldResults += query[i] + " "
    }
    let checkDoubleWords = []
    for (let indexedTerm of indexedTermsArray) {
        let term = query[query.length - 1]
        if (indexedTerm.toLowerCase().startsWith(term.toLowerCase()) && term != "") {
            if (!checkDoubleWords.includes(indexedTerm.toLocaleLowerCase())) {
                checkDoubleWords.push(indexedTerm.toLocaleLowerCase())
                return improveSuggestList(term, oldResults)
            }
        }
    }
}

function improveSuggestList(searchInput, oldResults) {
    var suggestList = []
    var i = 0
    for (let indexedTerm of indexedTermsArray) {
        if (indexedTerm.toLowerCase().startsWith(searchInput.toLowerCase()) && i < MAXSUGGESTS) {
            suggestList.push(oldResults + indexedTerm.toLocaleLowerCase())
            i++
        }
    }
    return suggestList
}