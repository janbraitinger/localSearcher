import {
  Section,
  RpcGetCall,
  RpcSendCall,
  Constant
} from './header.js';

var termArray = [] // for getting length if longest term for css width
var isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
var stdoutBuffer = ""
var _secureFlag = false
var dataTable = $('#aexample').DataTable({
  // "pageLength": 10,
  pagingType: 'full_numbers',
  searching: true,
  paging: false,
  info: false,
  "destroy": true,
  responsive: true,
  order: [
      [4, 'desc']
  ],
  "autoWidth": false,
  "oLanguage": {
      "Search": "Filter Data"
  },
  "iDisplayLength": -1,
  "sPaginationType": "full_numbers",
  "ordering": true,
  "columns": [ // Spaltenbreiten manuell definieren
      {
          "width": "50%"
      },
      {
          "width": "10%"
      },
      {
          "width": "15%"
      },
      {
          "width": "10%"
      },
      {
          "width": "5%"
      }
  ],
  columnDefs: [{
          className: 'text-center',
          targets: [1, 2, 3],
      },

      {
          "width": "250px",
          "targets": 3,
          "targets": "static",
      },

      {
          target: 5,
          visible: false,
          searchable: false,
      },
      {
          target: 6,
          visible: false,
          searchable: false,
      },
  ]
});






var _first = false
const index = ""
const getSection = new Object();
$.fn.dataTable.ext.errMode = 'none';

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




socket.on('connect', () => {
  logToConsole("connect");


});

socket.on('error'), (data) => {
  logToConsole("Error: " + data)
}

socket.on("disconnect", (reason) => {
  logToConsole(reason)
  if (reason === "io server disconnect") {
      logToConsole("trying to reconnect")
      socket.connect();
  }
  $("#waitingStatus").text("error")
  logToConsole("connection lost")
});




socket.on(RpcGetCall.AUTOCOMPLETE, (result) => {
  var dataArray = result
  removeElements()
  if (dataArray != null) {
      /* $("#searchQueryInputField").hover(function() {
           $(this).css("box-shadow", "none", "important");
       }, function() {
           $(this).css("box-shadow", "none", "important");
       });*/
      $("#list").show()
      for (let i of dataArray) {
          let word = "<b>" + i.substring(0, getSection.searchInputField.val().length) + "</b>"
          word += i.substring(getSection.searchInputField.val().length)
          $("#list").append("<li class='list-items'> <img class='searchBulletImg' src='/img/searchicon.png'>" + word + "</li>");
      }
  }
});




$('#settingsModal').on('hidden.bs.modal', function() {
  location.reload();
})


window.onbeforeunload = function() {
  socket.emit('Disconnected to the server', "");
};

socket.on("didYouMean", (alternative) => {
  var suggestionJSON = JSON.parse(alternative)

  if (suggestionJSON.length > 0) {
      var suggestionDiv = document.getElementById("doYouMean");
      var suggestionString = "";
      var breakUp = 10;
      for (var i = 0; i < suggestionJSON[0].length && i < breakUp; i++) {
          suggestionString += "<span class='suggestion'>" + suggestionJSON[0][i] + "</span>, ";
      }
      suggestionDiv.innerHTML = "<span style='color:black;font-style: normal;  '>Do you mean one of these?</span> " + suggestionString.slice(0, -2); // Entfernt das letzte Komma und Leerzeichen

      var suggestions = document.querySelectorAll('.suggestion');
      for (var i = 0; i < suggestions.length; i++) {
          suggestions[i].addEventListener('click', function() {
              $('#searchQueryInputField').val(this.innerText)
          });
      }

  } else {
      $("#doYouMean").html("No documents found")
  }
})



$("#connectedStatus").hide()
$("#disconnectedStatus").hide()


setInterval(function() {
  socket.on("luceneStatus", (status) => {
    switch (status) {
          case 'online':
              $("#serverStatus").css({
                  'background-color': 'greenyellow'
              });
              break;
          case 'offline':
              $("#serverStatus").css({
                  'background-color': 'red'
              });
              break;
          default:
              $("#serverStatus").css({
                  'background-color': 'orange'
              });
              break;
      }
  })
}, 1000);

$(window).click(function() {
  removeElements()
});



$("#list").click(function(e) {
  e.stopPropagation();
  getSection.searchInputField.val(e.target.innerText)
  removeElements()
});



socket.on(RpcGetCall.RESULTLIST, (data) => {
  data = JSON.stringify(data)
  var docs = JSON.parse(data)

  if (docs.length == 1) {
      $("#doYouMean").show()
  } else {
      $("#doYouMean").hide()
  }

  canvasId = 0
  displayResults(docs)
  getSection.loader.hide()
});





socket.on(RpcGetCall.CONF, (confData) => {
  let newPath = JSON.parse(confData).path
  getSection.dirPath.val(newPath);
})



socket.on(RpcGetCall.SERVERMESSAGE, (data) => {

  stdoutBuffer += data + "<br/>"

  $("#indexingWrapper").hide()
  $("#indexingDone").text("Indexing done. Please close this window.")

})


function makeAlert() {
  stdoutBuffer = ""

};

setInterval(makeAlert, 500);




getSection.searchButton.click(function() {
  var searchQuery = getSection.searchInputField.val().toLowerCase();
  var embeddingSearchChecked = []
  if ($('#checkboxGoogle').is(':checked')) {
      embeddingSearchChecked.push("google");
  }
  if ($('#checkboxPubMed').is(':checked')) {
      embeddingSearchChecked.push("pubmed");
  }


  let searchObj = new Object();
  searchObj.embedding = embeddingSearchChecked;
  searchObj.query = searchQuery;


  let searchMsg = JSON.stringify(searchObj)




  removeElements()
  dataTable.clear().draw()



  if (searchQuery.length > 0) {

      getSection.loader.show()

      socket.emit(RpcSendCall.SEARCH, searchMsg)
  }
});


getSection.searchInputField.keyup((e) => {
  if (getSection.searchInputField.val().length !== 0) {
      socket.emit(RpcSendCall.AUTOCOMPLETE_SEARCH, getSection.searchInputField.val())
  }
  removeElements()
})




function selectMatching(operator) {
  switch (operator) {
      case 0:
          return "#b09eb7"
      case 2:
          return "#e8d5ef"
      case 1:
          return "#d0bdd6"
      default:
          return "#fff"
  }
}




getSection.filterButton.click(() => {
  var filterArea = getSection.filterSection
  if (filterArea.is(":hidden")) {
      filterArea.show()
      return
  }
  filterArea.hide()
})


var canvasId = 0

function handleListElement(MATCHING, obj) {


  var getTerm = ""
  var getWeight = ""


  try {
      getWeight = obj.Weight
      getTerm = obj.Term
  } catch {
      getTerm = "unknown"
      getWeight = "unknown"
  }

  try {
      obj.MATCHING
  } catch {
      return

  }


  termArray.push(getTerm)


  let termButton = "<button class='termBtn' style='background-color:" + selectMatching(obj.Matching) + "; color:#fff;'>" + getTerm + "</button>"
  let weight = obj.Weight
  let weightResult = parseFloat(weight).toFixed(2);



  let preview = ""
  try {
      preview = obj.Preview.replace(/[\[\]]/g, "");
      preview = "[...] " + preview + " [...]"
  } catch {
      preview = "error"
  }


  var bar = '<div id="progressbar" style="width:'+scaleValues(weight)+'px;"></div>'

 
  
  dataTable.row.add([obj.Title, obj.Date, termButton, bar, weightResult, obj.Path, preview]).draw(true);
  //fillWeightCanvers(weightResult)





 
 
}

function scaleValues(value) {
  var n = 0
  var k = weightList[0];
  var result = (value - n) * 100 / (k - n);
  return result;
}





$("#statsButton").click(function(e) {
  $('#statsModal').modal('show');




});




$('#aexample tbody').on('click', 'tr', function() {
  $(".modal-body div span").text("");
  var path = dataTable.row(this).data()[5];
  $(".modal-title").html(dataTable.row(this).data()[0]);
  $(".path span").html(dataTable.row(this).data()[5]);
  $(".term span").html(dataTable.row(this).data()[2]);
  $(".preview span").html("<div class='word-wrap'>" + dataTable.row(this).data()[6] + "</div>");

  $('.btn-primary').off('click');

  $('.btn-primary').click(function() { //downloadButton on modal
      ajaxDownload(path)
  });

  $("#amyModal").modal("show");
});


var weightList = []


function displayResults(jsonPara) {

  if (jsonPara == "please try again later") {
      getSection.loader.hide()
      alert("An error occured. Please try again.")
      return
  }




  //getSection.searchResults.hide()
  //getSection.searchResults.html("")

  const matching = [Constant.DIRECT_MATCHING, Constant.GOOGLE_EMBEDDING, Constant.PUPMED_EMBEDDING]
  var matchingId = 0
  var objCount = 0
 



  for (let itemCollection of jsonPara) {

      let tmpWeight = itemCollection.Weight
      if (typeof tmpWeight !== 'undefined') {
      weightList.push(tmpWeight)
      }
  }

 

  for (let itemCollection of jsonPara) {
      try {
          if (itemCollection["time"] || itemCollection["stats"]) {

              getSection.docCounter.show()


              let length = parseInt(jsonPara.length) - 1
              getSection.docCounter.html("found " + length + " documents in " + itemCollection["time"] + " ms <br/> <span class='light'>" + JSON.stringify(itemCollection["stats"]) + "</span>")
              break
          }
      } catch {
        logToConsole("time error")
      }
      let selectMatching = matching[matchingId]
      let liElement = handleListElement(selectMatching, itemCollection)
      getSection.searchResults.append(liElement)
      matchingId++
  }




  if (termArray.length > 0) {
      var longest = termArray.reduce(
          function(a, b) {
              return a.length > b.length ? a : b;
          });

      $(".termBtn").width(longest.length * 8 + 'px');
      termArray = []
  }




  if (dataTable.data().count() > 0) {
    dataTable.column(4).visible(false); 
    $('#aexample').show();


  } else {
    $('#aexample').hide();
  }




}



$(".checkbox-menu").on("change", "input[type='checkbox']", function() {
  $(this).closest("li").toggleClass("active", this.checked);
});


$(document).on('click', '.allow-focus', function(e) {
  e.stopPropagation();
});




getSection.changeIndexBtn.on("click", function() {
  //loadEntry()
  let newIndex = getSection.dirPath.val();

  getSection.searchResults.html("")
  getSection.searchInputField.val("")
  socket.emit("reIndex", newIndex)
  $("#indexingWrapper").show();
  $("#changeIndexBtn").hide();




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

          //var fileName = xhr.getResponseHeader('Content-Disposition').split('"')[1].split('"')[0]


          var a = document.createElement('a');
          var url = window.URL.createObjectURL(response);
          a.href = url;
          // a.download = fileName;
          a.setAttribute('target', '_blank');
          a.click();

          //window.URL.revokeObjectURL(url);
          //window.open(url, '_blank');

      },
      error: function(xhr, ajaxOptions, thrownError) {
          console.log(xhr.status);
          console.log(thrownError);
      }

  });

}

function removeElements() {
  let items = document.querySelectorAll(".list-items")
  items.forEach((item) => {
      item.remove()
  })
  // $("#searchQueryInputField").css("cssText", "border: gainsboro solid 1px !important");
}




socket.on("indexedDocuments", (data) => {

  var number = 0
  const updateNumber = () => {
      document.getElementById("invertedDocumentsNumber").innerHTML = parseInt(number);
      if (number < data) {
          let tmp = data / 25
          number += tmp
          setTimeout(updateNumber, 1000 / 25); // Call this function again after 1 second
      }
  };

  // Call the updateNumber function for the first time
  updateNumber();



})

/* receiving messages from node.js backend */
socket.on('getconf', (data) => {
  let newPath = data.body
  getSection.dirPath.val(newPath);


  /*data = JSON.stringify(data)
  data = JSON.parse(data)
  console.log(data)
  let path = data.body
  getSection.dirPath.val(path);*/


});




var minDate, maxDate;

// Custom filtering function which will search data in column four between two values
$.fn.dataTable.ext.search.push(
  function(settings, data, dataIndex) {
      var min = minDate.val();
      var max = maxDate.val();
      var date = new Date(data[1]);
      if (
          (min === null && max === null) ||
          (min === null && date <= max) ||
          (min <= date && max === null) ||
          (min <= date && date <= max)
      ) {
          return true;
      }
      return false;
  }
);

$(document).ready(function() {
  // Create date inputs
  minDate = new DateTime($('#min'), {
      format: 'MMMM Do YYYY'
  });
  maxDate = new DateTime($('#max'), {
      format: 'MMMM Do YYYY'
  });

  // DataTables initialisation
  //var table = $('#example').DataTable();

  // Refilter the table
  $('#min, #max').on('change', function() {
      dataTable.draw();
   
  });
});




var container = document.getElementById("searchQueryInputField");
var list = document.getElementById("list");
list.style.width = container.offsetWidth - 2 + "px";

window.addEventListener("resize", function() {
  list.style.width = container.offsetWidth - 2 + "px";
});



var thresholdSlider = document.getElementById("slider");


thresholdSlider.addEventListener("input", function() {
  var threshold = parseFloat(this.value);
  $("#sliderData").html(threshold);
  addFilter(); // Call addFilter to update the DataTable with the new filter function
  dataTable.draw();
});

// Add a new filtering function to the DataTable
function addFilter() {
  logToConsole("filter is active now")
  $.fn.dataTable.ext.search.pop();
  $.fn.dataTable.ext.search.push(
      function(settings, data, dataIndex) {
          var threshold = parseFloat(thresholdSlider.value);
          var value = parseFloat(data[4]); // assuming the threshold column is 5th (index 4)
          return (isNaN(threshold) || isNaN(value) || value >= threshold);
      }
  );
  thresholdSlider.setAttribute("max", weightList[0])
}


$('#slider').on('input', function() {
  dataTable.draw();
});