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
        idCell = $('#month');
        $(idCell).html(month);
    };
    idCell = $('#hsa');
    $(idCell).html(hsa_name);
};

scanQueries.showResultPage = function(pagePath) {
    $('#show-report-button').on('click', function() {
        var month = $("#month").val();
        var hsa_name = $("#hsa").val();
        if (null == hsa_name) {
            scanQueries.toast('Please select HSA from list.');
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
   
    var whereClause = 'name = ?'; 
    var selectionArgs = [name];
    
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