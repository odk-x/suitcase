//package utils;
//
//import model.SpreedSheetColumn;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * Created by Kamil Kalfas
// * kkalfas@soldevelo.com
// * Date: 5/25/15
// * Time: 5:11 PM
// */
//public final class SortUtils {
//    private static final List<String> ORDERED_COLUMNS = Arrays.asList(
//            "HSA_name",
//            "HSA_addtl",
//            "name",
//            "clientID",
//            "client_code",
//            "age",
//            "village",
//            "EDD",
//            "num_preg",
//            "live_births",
//            "regCCPF",
//            "CCPFform",
//            "ANC_loc",
//            "monthpreg_ANC",
//            "ANC_v1",
//            "ANC_v2",
//            "ANC_v3",
//            "ANC_v4",
//            "ANC_v5",
//            "TTV1",
//            "TTV2",
//            "TTV3",
//            "TTV4",
//            "TTV5",
//            "malaria_pro_given",
//            "albend_given",
//            "iron",
//            "ITN",
//            "health_cond",
//            "health_cond_other1",
//            "health_cond_other2",
//            "HIV_test",
//            "birthplan_loc",
//            "birthplan_transport",
//            "danger_signs",
//            "date_delivery",
//            "deliv_facility",
//            "loc_delivery",
//            "infant_status",
//            "postnatal_notes",
//            "postnatal_chk",
//            "date_PNcheckup",
//            "postnatal_loc",
//            "vitA_given",
//            "breastfeeding",
//            "addtl_notes",
//            "addtl_text",
//            "V1_date",
//            "V1_topics",
//            "V2_date",
//            "V2_topics",
//            "V3_date",
//            "V3_topics",
//            "V4_date",
//            "V4_topics",
//            "V5_date",
//            "V5_topics",
//            "reg_comp",
//            "reg_initials",
//            "dischar_comp",
//            "dis_initials",
//            "lantern"
//    );
//
//    private SortUtils() {
//    }
//
//    public static List<SpreedSheetColumn> sort(List<SpreedSheetColumn> rowData) {
//        SpreedSheetColumn[] empty = new SpreedSheetColumn[ORDERED_COLUMNS.size()];
//        List<SpreedSheetColumn> sorted = Arrays.asList(empty);
//        for (SpreedSheetColumn column : rowData) {
//            int index = ORDERED_COLUMNS.indexOf(column.getFieldLabelColumn());
//            sorted.set(index, column);
//        }
//
//        return sorted;
//    }
//}
