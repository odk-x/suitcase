/* global control */
'use strict';

var scanQueries = {};

scanQueries.month = 'month';
scanQueries.hsa_name = 'hsa'; 

var TABLE_NAME = 'scan_MNH_Register1';
scanQueries.getKeysToAppendToURL = function(newMonth, newHSAName) {
    var result =
        '?' +
        scanQueries.month +
        '=' +
        encodeURIComponent(newMonth) +
        '&' +
        scanQueries.hsa_name +
        '=' +
        encodeURIComponent(newHSAName);
    return result;
};

scanQueries.getQueryParameter = function(key) {
    var href = document.location.search;
    var startIndex = href.search(key);
    if (startIndex < 0) {
        console.log('requested query parameter not found: ' + key);
        return null;
    }
    // Then we want the substring beginning after "key=".
    var indexOfValue = startIndex + key.length + 1;  // 1 for '='
    // And now it's possible that we have more than a single url parameter, so
    // only take as many characters as we need. We'll stop at the first &,
    // which is what specifies more keys.
    var fromValueOnwards = href.substring(indexOfValue);
    var stopAt = fromValueOnwards.search('&');
    if (stopAt < 0) {
        return decodeURIComponent(fromValueOnwards);
    } else {
        return decodeURIComponent(fromValueOnwards.substring(0, stopAt));
    }
};

scanQueries.showQueryParams = function() {
    var idCell;
    var month = scanQueries.getQueryParameter(scanQueries.month);
    var hsa_name = scanQueries.getQueryParameter(scanQueries.hsa_name);
    if (month !== null) {
        var arr_month = month.split(',');
        var monthParam = "";
        if (arr_month[0] == "all") {
            monthParam = "all  ";
        } else {
            var monthName = new Array();
            monthName[0] = "January";
            monthName[1] = "February";
            monthName[2] = "March";
            monthName[3] = "April";
            monthName[4] = "May";
            monthName[5] = "June";
            monthName[6] = "July";
            monthName[7] = "August";
            monthName[8] = "September";
            monthName[9] = "October";
            monthName[10] = "November";
            monthName[11] = "December";
            
            var monthParam = "";
            arr_month.forEach(function(entry) { 
                monthParam = monthParam + monthName[entry-1] + ", ";
            });
        }
        idCell = $('#month');
        $(idCell).html(monthParam.substring(0, monthParam.length-2));
    };
    idCell = $('#hsa');
    $(idCell).html(hsa_name);
};

scanQueries.showResultPage = function(pagePath, isMonth) {
    $('#show-report-button').on('click', function() {
        var month = $("#month").val();
        var hsa_name = $("#hsa").val();
        if (null == hsa_name) {
            scanQueries.toast('Please select HSA from list.');
        } else if (isMonth && null == month) {
            scanQueries.toast('Please select month from list.');
        } else {
            var queryString = scanQueries.getKeysToAppendToURL(month, hsa_name);
            var url = control.getFileAsUrl(pagePath + queryString);
            window.location.href = url;
        };
    });

};

scanQueries.getExistingRecordsByHSA = function(hsa_name) {
    var whereClause = 'HSA_name = ? AND dischar_comp is NULL'; 
    var selectionArgs = [hsa_name];
    
    var records = control.query(
            TABLE_NAME,
            whereClause,
            selectionArgs);

    return records;
};

scanQueries.getExistingRecordsByClientID = function(id) {
    var whereClause = 'clientID = ?';
    var selectionArgs = [id];
    
    var records = control.query(
            TABLE_NAME,
            whereClause,
            selectionArgs);

    return records;
};


scanQueries.getExistingRecordByName = function(name) {
   
    var whereClause = 'name LIKE ?'; 
    var selectionArgs = ["%" + name + "%"];
    
    var records = control.query(
            TABLE_NAME,
            whereClause,
            selectionArgs);

    return records;
};
scanQueries.toast = function(msg){
    $("<div class='ui-loader ui-overlay-shadow ui-body-e ui-corner-all'><h3>"+msg+"</h3></div>")
    .css({ display: "block", 
        opacity: 0.90, 
        position: "fixed",
        padding: "7px",
        background: "#969696",
        color: "#ffffff",
        "text-align": "center",
        width: "270px",
        left: ($(window).width() - 284)/2,
        top: $(window).height()/2 })
    .appendTo( $('#content-container')).delay( 1500 )
    .fadeOut( 400, function(){
        $(this).remove();
    });
}

$(document).ready(function () {
    $(function() {
        $('#hsa').on('change', function(){
          var selected = $(this).find("option:selected").val();
          if (selected === "all") {
            $('#hsa').selectpicker('deselectAll');
            $('#hsa').selectpicker('val', 'all');          
          };
        });
    });
})

$(document).ready(function () {
    $(function() {
        $('#month').on('change', function(){
          var selected = $(this).find("option:selected").val();
          if (selected === "all") {
            $('#month').selectpicker('deselectAll');
            $('#month').selectpicker('val', 'all');          
          };
        });
    });
})