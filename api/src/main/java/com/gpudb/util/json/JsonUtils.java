package com.gpudb.util.json;

import com.gpudb.GPUdbLogger;
import com.gpudb.RecordBase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class JsonUtils {

    public static boolean isValidJson(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            return true;
        } catch( JSONException je ) {
            return false;
        }
    }

    public static boolean isValidJsonArray(String jsonStr) {
        try {
            JSONArray json = new JSONArray(jsonStr);
            return true;
        } catch( JSONException je ) {
            return false;
        }
    }

    public static <T> boolean isListOfStrings( List<T> list) {
        return list.stream().allMatch( String.class::isInstance );
    }

    public static boolean isListOfValidJsonStrings( List<String> list) {
        return list.stream().allMatch(JsonUtils::isValidJson);
    }

    public static <T> boolean isListOfRecordBase( List<T> list) {
        return list.stream().allMatch( RecordBase.class::isInstance );
    }

    /**
     * Converts a List<String> to a JSON array where each element is a valid JSON String
     * @param list - the parameterized input list
     * @return - a JSON array as a String
     */
    public static String toJsonArray(List<String> list) {
        if( !isListOfStrings( list )) {
            GPUdbLogger.warn( "List is not a list of Strings; cannot convert to JSON" );
            return null;
        }
        return String.format("[%s]", String.join(",", list));
    }

}
