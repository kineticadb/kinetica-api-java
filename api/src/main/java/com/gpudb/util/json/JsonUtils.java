package com.gpudb.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpudb.GPUdbException;
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
    	return
    			list == null ||
    			list.isEmpty() ||
    			list.get(0) instanceof String;
    }

    public static boolean isListOfValidJsonStrings( List<String> list) {
        return list.stream().allMatch(JsonUtils::isValidJson);
    }

    public static <T> boolean isListOfRecordBase( List<T> list) {

    	return
    			list == null ||
    			list.isEmpty() ||
    			list.get(0) instanceof RecordBase;
    }

    /**
     * Converts a List<String> to a JSON array where each element is a valid JSON String
     * @param list - the parameterized input list
     * 
     * @return a string representation of a JSON array of the given list of JSON strings
     *
     * @return     - a JSON array as a String
     */
    public static String toJsonArray(List<String> list) {
        if( !isListOfStrings( list )) {
            GPUdbLogger.warn( "List is not a list of Strings; cannot convert to JSON" );
            return null;
        }
        return String.format("[%s]", String.join(",", list));
    }

    /**
     * Wrapper for ObjectMapper().writeValueAsString that throws GPUdbException
     * @param value - object to convert to JSON string
     * 
     * @return a JSON string representation of the given object
     *
     * @throws GPUdbException
     */
    public static String toJsonString(Object value) throws GPUdbException
    {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new GPUdbException("Error converting to JSON: " + value.toString(), e);
        }
    }
}
