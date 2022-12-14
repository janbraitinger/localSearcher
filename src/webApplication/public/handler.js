
import {
  Section,
  RpcGetCall,
  RpcSendCall,
  Constant
} from './header.js';

var termArray = [] // for getting length if longest term for css width
var isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
var stdoutBuffer = ""
const dataTable = $('#aexample').DataTable({
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
      "sSearch": "Filter Data"
  },
  "iDisplayLength": -1,
  "sPaginationType": "full_numbers",
  "ordering": true,
  columnDefs: [{
          className: 'text-center',
          targets: [1, 2, 3]
      },
      {
          "width": "50%",
          "targets": 0
      },
      {
          "width": "25%",
          "targets": 1
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

socket.on('error'), (data) => {
  console.log(error)
}

socket.on("disconnect", (reason) => {
  console.log(reason)
  if (reason === "io server disconnect") {
      logToConsole("trying to reconnect")
      socket.connect();
  }
  $("#waitingStatus").text("error")
  logToConsole("connection lost")
});




//  something wrong and only 3 suggestions
socket.on(RpcGetCall.AUTOCOMPLETE, (result) => {
  var dataArray = result

  removeElements()
  if (dataArray != null) {
   
    $("#searchQueryInputField").hover(function () {
      $(this).css("box-shadow", "none", "important");
  }, function () {
    $(this).css("box-shadow", "none", "important");
  });
       



      $("#list").show()
      for (let i of dataArray) {

          let word = "<b>" + i.substring(0, getSection.searchInputField.val().length) + "</b>"
          word += i.substring(getSection.searchInputField.val().length)
          $("#list").append("<li class='list-items'> <img class='searchBulletImg' src='/img/searchicon.png'>" + word + "</li>");


      }

  }
});




$('#settingsModal').on('hidden.bs.modal', function () {
  location.reload();
})


window.onbeforeunload = function() {
  socket.emit('disconnect', "");
};


socket.on("bradcast", (status) => {

})


socket.on("test", (data) => {
  console.log(data)
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


$(document).on('keypress', function(e) {
  if (e.which == 13) {

      e.preventDefault();
      var searchQuery = getSection.searchInputField.val();

      removeElements()
      dataTable.clear().draw()



      if (searchQuery.length > 0) {

          getSection.loader.show()

          socket.emit(RpcSendCall.SEARCH, searchQuery)

      }
  }
});

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
  let docs = JSON.parse(data)
  canvasId = 0

  showResultList(docs)

  getSection.loader.hide()
  $("#aexample_paginate").show()
});

// conf data could contain multiple elements in future
socket.on(RpcGetCall.CONF, (confData) => {
  let newPath = JSON.parse(confData).path
  getSection.dirPath.val(newPath);

})



socket.on(RpcGetCall.SERVERMESSAGE, (data) => {

  stdoutBuffer += data + "<br/>"

  //getSection.serverMessage.html("indexing done")
  $("#indexingWrapper").hide()
  $("#indexingDone").text("Indexing done. Please close this window.")

})


function makeAlert() {
  stdoutBuffer = ""

};

setInterval(makeAlert, 500);




getSection.searchButton.click(function() {
  var searchQuery = getSection.searchInputField.val();
  var embeddingSearchChecked = []
  if($('#checkboxGoogle').is(':checked')){
    embeddingSearchChecked.push("google");
  }
  if($('#checkboxPubMed').is(':checked')){
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

  termArray.push(getTerm)


  let termButton = "<button class='termBtn' style='background-color:" + selectMatching(obj.Matching) + "; color:#fff;'>" + getTerm + "</button>"
  let weight = obj.Weight
  let weightResult = parseFloat(weight).toFixed(2);



  let canvas = "<canvas class='weightCanvas' id='myCanvas" + canvasId + "'>Your browser does not support the HTML5 canvas tag.</canvas>"

  dataTable.row.add([obj.Title, obj.Date, termButton, canvas, weightResult, obj.Path, obj.Preview]).draw(true);


  $('#aexample').show()
  fillWeightCanvers(weightResult)

}

function logslider(position) {
  // position will be between 0 and 100
  var minp = 0;
  var maxp = 5;


  var minv = Math.log(10);
  var maxv = Math.log(10000);

  // calculate adjustment factor
  var scale = (maxv - minv) / (maxp - minp);

  return Math.exp(minv + scale * (position - minp));
}



function fillWeightCanvers(weight) {


  //var canvases = document.getElementsByTagName('canvas');
  //for( var i=0; i<canvases.length; i++){
  let canvas = document.getElementById("myCanvas" + canvasId)
  //let ctx = canvases[i].getContext('2d');
  try {
      let ctx = canvas.getContext('2d');
      ctx.fillStyle = 'lightblue'
      ctx.fillRect(20, 20, logslider(weight), 100);

      ctx.stroke();
      canvasId++
  } catch {
      return
  }
  //}
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
  $(".preview span").html(dataTable.row(this).data()[6]);

  $('.btn-primary').off('click');

  $('.btn-primary').click(function() { //downloadButton on modal
      ajaxDownload(path)
  });

  $("#amyModal").modal("show");
});



function showResultList(jsonPara) {
  if(jsonPara.header == "error"){
    alert("server is indexing atm")
    return
  }



  getSection.searchResults.hide()
  getSection.searchResults.html("")
  const matching = [Constant.DIRECT_MATCHING, Constant.GOOGLE_EMBEDDING, Constant.PUPMED_EMBEDDING]
  var matchingId = 0
  var objCount = 0
  console.log(jsonPara)
  for (let itemCollection of jsonPara) {
   
      if (itemCollection["time"] || itemCollection["stats"]) {

          getSection.docCounter.show()
          getSection.docCounter.html("found " + jsonPara.length-- + " documents in " + itemCollection["time"] + " ms <br/> " + JSON.stringify(itemCollection["stats"]))
          break
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
      console.log("drwaew")
  });
});
