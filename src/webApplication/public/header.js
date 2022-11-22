export const Section = {
    searchButton: "#searchBtn",
    errorMessage: "#errormsg",
    searchInputField: "#searchQueryInputField",
    serverMessage: "#servermsg",
    dirPath: "#indexpath",
    dirContentList: "#folderContent",
    searchResults: "#results",
    filterSection: "#filterArea",
    countDirEntrys: "#countEntrys",
    loadingSymbol: ".loader",
    statistics : "#statistics",
    autocompleteList : "#list",
    filterButton : "#filterButton",
    docCounter : "#countEntrys",
    changeIndexBtn : "#changeIndexBtn",
    termButton : ".termBtn"
 }
 
 
 export const RpcGetCall = {
    ERROR: "error",
    AUTOCOMPLETE: "autocomplete",
    SEARCH: "search",
    CHANGEDIR: "changedir",
    SERVERMESSAGE : "stdout",
    CONF: "conf",
    RESULTLIST : "docResultList",
    FILE : "fileBack"
 }

 export const RpcSendCall = {
   AUTOCOMPLETE_SEARCH : "search",
   SEARCH : "finalSearch",
   CHECK_INDEX : "checkIndex",
   NEW_INDEX : "newIndex",
   GET_FILE : "file"
}


 export const Constant = {
   DIRECT_MATCHING : 0,
   GOOGLE_EMBEDDING: 1,
   PUPMED_EMBEDDING: 2

 }