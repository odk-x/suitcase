/* global control */
'use strict';

var HSA_list = [
    "Rodney Kamaliza",
    "Mercy Macholowe",
    "Luka M'bawa",
    "Ben Banda",
    "Cosmas Bakili",
    "Mackson Khalawako",
    "Gladys Kaselo",
    "Tobias Kadzoma",
    "Asnty Chikapaza",
    "George Koloko",
    "Mabvuto Mandere",
    "Acklen Mdoka",
    "Maxwel Kadzilawa",
    "Chisomo Chisada ", 
    "Beatrice Chikaoneka",
    "Charles Isaac",
    "George Chidulo",
    "Emma Muwa",
    "Pemphero Katondo",
    "Catherine Kazembe"
];

function onlyUnique(value, index, self) { 
    return self.indexOf(value) === index;
}

function display() {
    var hsa = scanQueries.getQueryParameter(scanQueries.hsa_name);
    var arr_hsa = hsa.split(','); 

    var total = 0;
    //TODO : temporary we don't care about months
    if (arr_hsa.length == 1) {
      if (arr_hsa[0] == "all") {
        HSA_list.forEach(function(entry){
          var uniqueIDs = filterPatientForHSA(entry);

          var subTotal = uniqueIDs.length;
          if (subTotal !== 0) {
            createRow(entry, subTotal);
            total += subTotal;
          }
        });
      } else {
        // one HSA and one MONTH
        // so only one row to display
        var uniqueIDs = filterPatientForHSA(arr_hsa[0]);

        total = uniqueIDs.length;
        createRow(arr_hsa[0], total);
      }
    } else {
      arr_hsa.forEach(function(entry) {
        var uniqueIDs = filterPatientForHSA(entry);

        var subTotal = uniqueIDs.length;
        total += subTotal;
        createRow(entry, subTotal);
      });
    }
    $('#total').html(total);
}

function filterPatientForHSA(hsaName) {
  var record = scanQueries.getExistingRecordsByHSA(hsaName);

  var ids = record.getColumnData("clientID");
  var idsArr = JSON.parse(ids);
  var uniqueIDs = idsArr.filter(onlyUnique);

  uniqueIDs.forEach(function(uniqueID){
    var isDischar = scanQueries.isClientDischarComp(uniqueID);
    if (isDischar.getCount()>0) {
      //remove id if discharge completed
      var index = uniqueIDs.indexOf(uniqueID);
      if (index > -1) {
        uniqueIDs.splice(index, 1);
      }
    }
  });

  return uniqueIDs;
}

function createRow(name, value) {
  var newRow = document.createElement("tr");
  newRow.className = "active";
  var newcell1 = document.createElement("td")
  newcell1.innerText = name;
  newcell1.className = "col-md-2";
  newRow.appendChild(newcell1);

  var newcell2 = document.createElement("td")
  newcell2.innerText = value;
  newcell2.className = "col-md-2";
  newRow.appendChild(newcell2);

  var myTable = document.getElementById("report-table");
  myTable.appendChild(newRow);
}
 