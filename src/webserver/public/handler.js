
import {
  Section,
  RpcGetCall,
  RpcSendCall,
  Constant
} from './header.js';

var termArray = [] // for getting length if longest term for css width
var isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
var stdoutBuffer = ""
const index = ""
const getSection = new Object();
getSection.searchButton = $(Section.searchButton)
getSection.errorMessage = $(Section.errorMessage)
getSection.searchInputField = $(Section.searchInputField)
getSection.statistics = $(Section.statistics)
getSection.loader = $(Section.loadingSymbol)
getSection.serverMessage = $(Section.serverMessage)
getSection.dirPath = $(Section.dirPath)
getSection.dirContentList = $(Section.dirContent)
getSection.autocompleteList = $(Section.list)
getSection.filterButton = $(Section.filterButton)
getSection.filterSection = $(Section.filterSection)
getSection.docCounter = $(Section.docCounter)
getSection.searchResults = $(Section.searchResults)
getSection.changeIndexBtn = $(Section.changeIndexBtn)
getSection.termBtn = $(Section.term)

if (isIOS) {
  getSection.searchButton.prop('disabled', true)
  getSection.errorMessage.html("[error: no ios support]")
  getSection.searchInputField.prop('disabled', true)
}


/* socket.io section */
/* receiving messages from node.js backend */
socket.on('connect', () => {
  logToConsole("connect");
});

socket.on("disconnect", (reason) => {
  if (reason === "io server disconnect") {
      logToConsole("trying to reconnect")
      socket.connect();
  }
  logToConsole("connection lost")
});


//  something wrong and only 3 suggestions
socket.on(RpcGetCall.AUTOCOMPLETE, (result) => {
  var dataArray = result
  removeElements()
  if (dataArray != null) {
      for (let i of dataArray) {
          let word = "<b>" + i.substring(0, getSection.searchInputField.val().length) + "</b>"
          word += i.substring(getSection.searchInputField.val().length)
          $("#list").append("<li class='list-items'>" + word + "</li>");
      }
  }
});


$("#list").on("click", function(e) {
  getSection.searchInputField.val(e.target.innerText)
  removeElements()
});


socket.on(RpcGetCall.RESULTLIST, (data) => {
  let docs = JSON.parse(data)
  showResultList(docs)
  getSection.loader.hide()
});

// conf data could contain multiple elements in future
socket.on(RpcGetCall.CONF, (confData) => {
  let newPath = JSON.parse(confData).path
  getSection.dirPath.val(newPath);
})


socket.on(RpcGetCall.SERVERMESSAGE, (data) => {

  stdoutBuffer += data + "<br/>"
  
  getSection.serverMessage.html(stdoutBuffer)
})


function makeAlert(){ 
  stdoutBuffer = ""

};

setInterval(makeAlert, 500);


socket.on(RpcGetCall.RESULTLIST, (data) => {
  let jsonData = JSON.parse(data)
  var stringData = ""
  for (let i of jsonData) {
      stringData += "- " + i + "<br/>"
  }
  getSection.dirContentList.html(stringData)
})


getSection.searchButton.click(function() {
  var searchQuery = getSection.searchInputField.val();
  getSection.loader.show()
  removeElements()
  socket.emit(RpcSendCall.SEARCH, searchQuery)
});


getSection.searchInputField.keyup((e) => {
  if (getSection.searchInputField.val().length !== 0) {
      socket.emit(RpcSendCall.AUTOCOMPLETE_SEARCH, getSection.searchInputField.val())
  }
  removeElements()
})




function selectMatching(operator) {
  const btn = $(document.createElement('button')).prop({
      type: 'button',
      class: 'wordembeddingButton'
  })

  switch (operator) {
      case 0:
          btn.html("direct matching")
          break
      case 2:
          btn.html("pubmed corpus")
          btn.css("background-color", "#daedd3");
          break
      case 1:
          btn.html("google corpus")
          btn.css("background-color", "#e6d3ed");
          break
      default:
          btn.html("unknown")
  }

  return btn
}


getSection.filterButton.click(() => {
  var filterArea = getSection.filterSection
  if (filterArea.is(":hidden")) {
      filterArea.show()
      return
  }
  filterArea.hide()
})




function handleListElement(MATCHING, img, obj, i) {
  var getTerm = ""
  try {
      getTerm = obj.Stats.split("contents:")[1].split(" in ")[0]
  } catch {
      getTerm = "unknown"
  }
  let btn = $(document.createElement('button')).prop({
      type: 'button',
      class: 'termBtn'
  })
  termArray.push(getTerm)
  btn.html(getTerm)
  const li = document.createElement("li");
  li.appendChild(img)
  li.appendChild(document.createTextNode(obj.Title));
  selectMatching(MATCHING).appendTo(li)
  btn.appendTo(li)
  li.setAttribute('data-toggle', 'modal');
  li.setAttribute('data-target', '#myModal');
  li.setAttribute('data-jsonNr', i);
  li.setAttribute('data-title', "<p style='word-wrap: break-word;word-break: break-all;'>" + obj.Title + "</p>");
  li.setAttribute('data-path', obj.Path);
  li.setAttribute('data-stats', obj.Stats);
  li.setAttribute('data-term', getTerm);
  li.classList.add('list-group-item');




  return li
}




function showResultList(jsonPara) {


  getSection.searchResults.html("")
  const matching = [Constant.DIRECT_MATCHING, Constant.GOOGLE_EMBEDDING, Constant.PUPMED_EMBEDDING]
  var matchingId = 0
  var objCount = 0
  for (let itemCollection of jsonPara) {
      var selectMatching = matching[matchingId]

      for (let i = 0; i < itemCollection.length; i++) {


          var img = document.createElement('img');

          if (itemCollection[i].Type == "pdf") {
              img.src = "img/pdf_icon.png"
          } else {
              img.src = "img/txt_icon.png"
          }

          var liElement = handleListElement(selectMatching, img, itemCollection[i], i)
          getSection.searchResults.append(liElement)
          objCount++
      }
      matchingId++

  }
  getSection.docCounter.html(objCount + " documents found")

  if(termArray.length > 0){

  var longest = termArray.reduce(
      function(a, b) {
          return a.length > b.length ? a : b;
      });

  $(".termBtn").width(longest.length * 8 + 'px');
  termArray = []
    
}
}




$(document).on("click", ".list-group-item", function() {
  let title = $(this).attr("data-title")
  let path = $(this).attr("data-path")
  let term = $(this).attr("data-term")
  let stats = $(this).attr("data-stats")

  console.log(stats)

  $('.modal-title').html("<span style='word-break: break-all;'>" + title + "</span>")

  //$('.modal-body-path').html("hier kommt der Text")

  $('.modal-body').html("<p><b>Term: </b>"+term+"</p>" + 
  "<p><b>Date: </b> unknown </p>"+
  "<p style='word-break: break-all;'><b>Path:</b> "+path+" <button type='button' class='btn btn-dark' id='getDoc' value='" + path + "'>View/Download</button><br/>" + "" + "</p>")

//<hr/>similar documents:<ul style='padding-left:15px;'><li>document A</li><li>document B</li></ul>

  $('#getDoc').click(function() {
      path = $(this).attr("value")
      ajaxDownload(path)

  });
});


$(".checkbox-menu").on("change", "input[type='checkbox']", function() {
  $(this).closest("li").toggleClass("active", this.checked);
});


$(document).on('click', '.allow-focus', function(e) {
  e.stopPropagation();
});



/*
function loadEntry() {
  socket.emit(RpcSendCall.CHECK_INDEX)
}
*/

getSection.changeIndexBtn.on("click", function() {
  //loadEntry()
  let newIndex = getSection.dirPath.val();

  getSection.searchResults.html("")
  getSection.searchInputField.val("")
  socket.emit(RpcSendCall.NEW_INDEX, newIndex)


})




function logToConsole(message) {
  console.log("[" + new Date().toLocaleTimeString() + "] " + message);
}




function ajaxDownload(file) {

  $.ajax({
      url: "/getFile",
      type: "POST",
      data: {
          "path": file
      },

      xhrFields: {
          responseType: 'blob'
      },
      success: function(response, status, xhr) {

          var fileName = xhr.getResponseHeader('Content-Disposition').split('"')[1].split('"')[0]
          console.log(fileName)

          var a = document.createElement('a');
          var url = window.URL.createObjectURL(response);
          a.href = url;
          //a.download = fileName;
          a.click();
          window.URL.revokeObjectURL(url);

      },
      error: function(xhr, ajaxOptions, thrownError) {
          alert(xhr.status);
          alert(thrownError);
      }

  });
  console.log("done")
}

function removeElements() {
  let items = document.querySelectorAll(".list-items")
  items.forEach((item) => {
      item.remove()
  })
}

getFirstIndex()


function getFirstIndex(){


$.ajax({
  url: "/getIndex",
  type: "GET",
  success: function(response, status, xhr) {
    getSection.dirPath.val(response);
    //console.log(response)
  },
  error: function(xhr, ajaxOptions, thrownError) {
      //alert(xhr.status);
      //alert(thrownError);
  }

});

}