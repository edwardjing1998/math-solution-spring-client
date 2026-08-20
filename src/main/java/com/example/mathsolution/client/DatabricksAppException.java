package com.example.mathsolution.client;

public class DatabricksAppException
        extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public DatabricksAppException(
            int statusCode,
            String responseBody) {

        super(buildMessage(statusCode, responseBody));

        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    private static String buildMessage(
            int statusCode,
            String responseBody) {

        return "Databricks App request failed with HTTP "
                + statusCode
                + "; response: "
                + responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}