/* global control */
'use strict';

function display() {
  var searchClient = function(id, name) {
        if (!id=="") {
            var results = scanQueries.getExistingRecordsByClientID(id);
            if (results.getCount()==1) {
                console.log("I am here");
                var rowId = results.getRowId(0);
                //Open the list view for the found record
                control.openDetailView(
                        'scan_MNH_Register1',
                        rowId,
                        'assets/find_client_detail.html');
            } else if (results.getCount()>1) {
                //We found multiple records associated with this id so let's look for more identifiers
                control.openTableToListView(
                'scan_MNH_Register1',
                'clientID = ?',
                [id],
                'assets/find_client_list.html');
            } else {
                alert("Patient with id: " + id + " not found");
            }
        } else if (!name==""){
            var results = scanQueries.getExistingRecordByName(name);
            if (results.getCount()==1) {
                var rowId = results.getRowId(0);
                //Open the list view for the found record
                control.openDetailView(
                        'scan_MNH_Register1',
                        rowId,
                        'assets/find_client_detail.html');
            } else if (results.getCount()>1) {
                //We found multiple records associated with this id so let's look for more identifiers
                control.openTableToListView(
                'scan_MNH_Register1',
                'name = ?',
                [name],
                'assets/find_client_list.html');
            } else {
                alert("Patient with name: " + name + " not found");
            }
        } else {
            alert("Enter an ID or Name");
        }
        console.log('Done finding ' + id);
    };

    $('#search-client').on('click', function() {
       var patientID = $("#client_id").val();
       var patientName = $("#client_name").val();

       searchClient(patientID, patientName);
    });
}