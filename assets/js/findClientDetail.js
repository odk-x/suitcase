/**
 * The file for displaying a detail view.
 */
/* global $, control, d3, data */
'use strict';

// Handle the case where we are debugging in chrome.
if (JSON.parse(control.getPlatformInfo()).container === 'Chrome') {
    console.log('Welcome to Tables debugging in Chrome!');
    $.ajax({
        url: control.getFileAsUrl('../app/output/debug/' + tableId + '_data.json'),
        async: false,  // do it first
        success: function(dataObj) {
            if (dataObj === undefined || dataObj === null) {
                console.log('Could not load data json for table: ' + tableId);
            }
            window.data.setBackingObject(dataObj);
        }
    });
}

var rowId;

function display() {
    // Perform your modification of the HTML page here and call display() in
    // the body of your .html file.
    $('#name').text(data.get('name'));
    $('#clientID').text(data.get('clientID'));
    $('#village').text(data.get('village'));

    //image for child name
    var nameUriRelative = data.get('name_image0_uriFragment');
    var nameSrc = '';
    if (nameUriRelative !== null && nameUriRelative !== "") {
        var nameUriAbsolute = control.getRowFileAsUrl(data.getTableId(), data.getRowId(0), nameUriRelative);
        nameSrc = nameUriAbsolute;
    }

    var nameThumbnail = $('<img>');
    nameThumbnail.attr('src', nameSrc);
    nameThumbnail.attr('class', 'thumbnail');
    nameThumbnail.attr('id', 'name_image0');
    $('#name_pic').append(nameThumbnail);

    //image for child id
    var seUriRelative = data.get('clientID_image0_uriFragment');
    var seSrc = '';
    if (seUriRelative !== null && seUriRelative !== "") {
        var seUriAbsolute = control.getRowFileAsUrl(data.getTableId(), data.getRowId(0), seUriRelative);
        seSrc = seUriAbsolute;
    }

    var seThumbnail = $('<img>');
    seThumbnail.attr('src', seSrc);
    seThumbnail.attr('class', 'thumbnail');
    seThumbnail.attr('id', 'clientID_image0');
    $('#clientID_pic').append(seThumbnail);

    //image for child village
    var regUriRelative = data.get('village_image0_uriFragment');
    var regSrc = '';
    if (regUriRelative !== null && regUriRelative !== "") {
        var regUriAbsolute = control.getRowFileAsUrl(data.getTableId(), data.getRowId(0), regUriRelative);
        regSrc = regUriAbsolute;
    }

    var regThumbnail = $('<img>');
    regThumbnail.attr('src', regSrc);
    regThumbnail.attr('class', 'thumbnail');
    regThumbnail.attr('id', 'village_image0');
    $('#village_pic').append(regThumbnail);

}