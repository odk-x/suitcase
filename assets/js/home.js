/**
 * Responsible for rendering the home screen.
 */
'use strict';
/* global control */

function display() {

    $('#trimester-anc-button').on('click', function() {
        control.launchHTML('assets/reportTrimesterANC.html');
    });

}
