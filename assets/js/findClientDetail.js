/**
 * The file for displaying a detail view.
 */
/* global $, control, d3, data */
'use strict';

//field name, field description
var fieldNames = [
    "HSA_name,HSA Name",
    "HSA_addtl,HSA Addtl",
    "name, Name",
    "clientID,ID",
    "client_code,Client Code",
    "age,Age",
    "village,Village",

    "MATERNAL HISTORY",
    "EDD,Expected Due Date",
    "num_preg,Total # of pregnaces",
    "live_births,Total # of live births",

    "CCPF",
    "regCCPF,Registered for CCPF",
    "CCPFform,Registration form completed",

    "ANTENATAL CARE",
    "ANC_loc,ANC Location",
    "monthpreg_ANC,Month of pregnacy when ANC started",
    "ANC_v1,ANC#1",
    "ANC_v2,ANC#2",
    "ANC_v3,ANC#3",
    "ANC_v4,ANC#4",
    "ANC_v5,ANC#5",
    "TTV1,TTV#1",
    "TTV2,TTV#2",
    "TTV3,TTV#3",
    "TTV4,TTV#4",
    "TTV5,TTV#5",
    "malaria_pro_given,Malaria Prophylaxis given",
    "albend_given,Albendazole given",
    "iron,Iron given",
    "ITN,ITN given",
    "health_cond,Maternal Health Conditions",
    "health_cond_other1,Maternal Health Conditions Other#1",
    "health_cond_other2,Maternal Health Conditions Other#2",
    "HIV_test,HIV Testing",
    "birthplan_loc,Birth Plan Locaton",
    "birthplan_transport,Birth Plan Transport",
    "danger_signs,Danger Signs",

    "POSTNATAL",
    "date_delivery,Date of Deliviery",
    "deliv_facility,Deliviered in facility",
    "loc_delivery,Location of Deliviery",
    "infant_status,Infant Status",
    "postnatal_notes,Postnatal notes",
    "postnatal_chk,Postnatal Checkup",
    "date_PNcheckup,Date Attended",
    "postnatal_loc,Location",
    "vitA_given,Vitamin A given",
    "breastfeeding,Breastfeeding",
    "addtl_notes,Additional Notes",

    "HOME VISITS & EDUCATION",
    "V1_date,Visit Date #1",
    "V1_topics,Visit Topics",
    "V2_date,Visit Date #2",
    "V2_topics,Visit Topics",
    "V3_date,Visit Date #3",
    "V3_topics,Visit Topics",
    "V4_date,Visit Date #4",
    "V4_topics,Visit Topics",
    "V5_date,Visit Date #5",
    "V5_topics,Visit Topics",

    "OFFICIAL USE ONLY",
    "reg_comp,Registration Complete",
    "reg_initials,Initials",
    "dischar_comp,Discharge Complete",
    "dis_initials,Initials",
    "lantern,Client qualifies for solar lantern"
];

var visitTopicsKey = [
    "pregnancy,Pregnancy Danger Signs",
    "malaria,Malaria Prophylaxis",
    "HIV,HIV/TB Counseling",
    "activity,Activity Level",
    "nutrition,Nutrition",
    "birth,Birth Plan",
    "breastfeeding,Breastfeeding",
    "family,Family Planning",
    "postnatal,Postnatal Danger Signs",
    "neonatal,Neonatal Care/Danger Signs"
]

var dangerSignsKey = [
    "excessive,Excessive vaginal bleeding",
    "fever,Fever",
    "vaginal,Foul vaginal discharge",
    "convulsions,Convulsions",
    "headache,Severe Headache"
]

var maternalHealthCondKey = [
    "hypertension,Hypertension/Pre-eclampsia",
    "diabetes,Diabetes",
    "age,Under the age of 20",
    "underweight,Underweight",
    "carrying,Carrying twins or triplets",
    "preterm,History od preterm deliviery",
    "stillbirth,History of stillbirth or neonatal death"
]

String.prototype.contains = function(element){
    return this.indexOf(element) > -1;
};

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
    fieldNames.forEach(function(field) { 
        var arr_field = field.split(',');

        var para
        var text;

        if (arr_field.length == 1) {
            para = document.createElement("h3");
            text = document.createTextNode(arr_field[0]);
        } else {   
            var output = "";
            var topicsData = data.get(arr_field[0]);

            para = document.createElement("p");
            if (topicsData !== undefined && topicsData !== null) {
                if (arr_field[1] === "Visit Topics") {
                    if (topicsData.length > 0) {
                        for (var i = 0; i < visitTopicsKey.length; i++) {
                            var arr_visitTopicsKey = visitTopicsKey[i].split(',');
                            if (topicsData.contains(arr_visitTopicsKey[0])) {
                                output = output + arr_visitTopicsKey[1] + ", ";
                            }   
                        }
                        if (output.length > 3) {
                            output = output.substring(0, output.length-2)
                        }
                    }
                } else if (arr_field[1] === "Danger Signs") {
                    if (topicsData.length > 0) {
                        for (var i = 0; i < dangerSignsKey.length; i++) {
                            var arr_dangerSignsKey = dangerSignsKey[i].split(',');
                            if (topicsData.contains(arr_dangerSignsKey[0])) {
                                output = output + arr_dangerSignsKey[1] + ", ";
                            }   
                        }
                        if (output.length > 3) {
                            output = output.substring(0, output.length-2)
                        }
                    }
                } else if (arr_field[1] === "Maternal Health Conditions") {
                    if (topicsData.length > 0) {
                        for (var i = 0; i < maternalHealthCondKey.length; i++) {
                            var arr_maternalHealthCondKey = maternalHealthCondKey[i].split(',');
                            if (topicsData.contains(arr_maternalHealthCondKey[0])) {
                                output = output + arr_maternalHealthCondKey[1] + ", ";
                            }   
                        }
                        if (output.length > 3) {
                            output = output.substring(0, output.length-2)
                        }
                    }
                } else if (arr_field[1] === "HIV Testing") {
                    if (topicsData.contains("yes")) {
                        output = "yes";
                    } else {
                        output = "-";
                    }
                } else if (arr_field[1] === "Discharge Complete" ||
                            arr_field[1] === "Registration Complete") {
                    if (topicsData.contains("1")) {
                        output = "yes";
                    } else {
                        output = "-";
                    }
                } else {   
                    output = topicsData;        
                }
            } else {
                output = "-";
            }
            text = document.createTextNode(arr_field[1] + ": " + output);  
        }
        para.appendChild(text);
        document.getElementById("data").appendChild(para);  

        //add image
        if (arr_field.length > 1) {
            para = document.createElement("div");
            para.id = arr_field[0] + "_pic";

            var imageUriRelative = data.get(arr_field[0] + '_image0.uriFragment');
            var imageSrc = '';
            if (imageUriRelative !== null && imageUriRelative !== "") {
                var imageUriAbsolute = control.getRowFileAsUrl(data.getTableId(), data.getRowId(0), imageUriRelative);
                imageSrc = imageUriAbsolute;
            }

            var img = document.createElement("img");
            img.src = imageSrc;
            img.class = 'thumbnail';
            img.id = arr_field[0] + '_image0';
            img.style.maxWidth = "100%";
            para.appendChild(img);

            document.getElementById("data").appendChild(para);   
        }
    });
}