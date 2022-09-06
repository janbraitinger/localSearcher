import {
  Section,
  RpcGetCall,
  RpcSendCall,
  Constant
} from './header.js';

var isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
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
  getSection.serverMessage.html(data)
})


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


function removeElements() {
  let items = document.querySelectorAll(".list-items")
  items.forEach((item) => {
      item.remove()
  })
}

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
  const li = document.createElement("li");
  li.appendChild(img)
  li.appendChild(document.createTextNode(obj.Title));
  selectMatching(MATCHING).appendTo(li)
  li.setAttribute('data-toggle', 'modal');
  li.setAttribute('data-target', '#myModal');
  li.setAttribute('data-jsonNr', i);
  li.setAttribute('data-title', "<p style='word-wrap: break-word;word-break: break-all;'>" + obj.Title + "</p>");
  li.setAttribute('data-path', obj.Path);
  li.setAttribute('data-stats', obj.Stats);
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


}


$(document).on("click", ".list-group-item", function() {
  let title = $(this).attr("data-title")
  let path = $(this).attr("data-path")
  let stats = $(this).attr("data-stats")
  $('.modal-title').html("<span style='word-break: break-all;'>" + title + "</span>")
  $('.modal-body').html("<p style='word-break: break-all;'><a href='" + path + "'>" + path + "</a><br/>" + stats + "</p><hr/>similar documents:<ul style='padding-left:15px;'><li>document A</li><li>document B</li></ul>")
});


$(".checkbox-menu").on("change", "input[type='checkbox']", function() {
  $(this).closest("li").toggleClass("active", this.checked);
});


$(document).on('click', '.allow-focus', function(e) {
  e.stopPropagation();
});



function loadEntry() {
  socket.emit(RpcSendCall.CHECK_INDEX)

}

getSection.changeIndexBtn.on("click", function() {
  loadEntry()
  let newIndex = getSection.dirPath.val();
  socket.emit(RpcSendCall.NEW_INDEX, newIndex)
  loadEntry()
})



function logToConsole(message) {
  console.log("[" + new Date().toLocaleTimeString() + "] " + message);
}