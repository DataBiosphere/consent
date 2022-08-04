package org.broadinstitute.consent.http.enumeration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public enum DatasetPropertyType {
    String,
    Boolean,
    Number,
    Json,
    Date;

    public static DatasetPropertyType parse(String type) {
        if (type == null) {
            return DatasetPropertyType.String;
        }
        switch (type) {
            case "Boolean":
                return DatasetPropertyType.Boolean;
            case "Json":
                return DatasetPropertyType.Json;
            case "Date":
                return DatasetPropertyType.Date;
            case "Number":
                return DatasetPropertyType.Number;
            default: // always default to string.
                return DatasetPropertyType.String;
        }
    }

    public Object coerce(String value) throws IllegalArgumentException {
        switch (this) {
            case Boolean:
                return coerceToBoolean(value);
            case String:
                return value;
            case Date:
                return coerceToDate(value);
            case Json:
                return coerceToJson(value);
            case Number:
                return coerceToNumber(value);
            default:
                throw new IllegalArgumentException("Invalid DatasetPropertyType");
        }
    }

    public static Boolean coerceToBoolean(String value) throws IllegalArgumentException {
        return java.lang.Boolean.valueOf(value);
    }

    public static Instant coerceToDate(String value) throws IllegalArgumentException {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Could not parse as Date: " + e.getMessage());
        }
    }

    public static Number coerceToNumber(String value) throws IllegalArgumentException {
        try {
            return Integer.valueOf(value);
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Could not parse as Integer: " + e.getMessage());
        }
    }
    public static JsonObject coerceToJson(String value) throws IllegalArgumentException {
        try {
            return new Gson().fromJson(value, JsonObject.class);
        } catch(JsonSyntaxException e) {
            throw new IllegalArgumentException("Could not parse as Json: " + e.getMessage());
        }
    }
}