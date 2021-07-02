package org.opendatakit.suitcase.model;

public class SyncClientError {
    private static final int UNKNOWN_ERROR_STATUS_CODE = 500;
    private final int statusCode;
    private final String message;

    public SyncClientError(Exception e) {
        String errorMsg = e.getCause().getMessage();
        int startIndex = errorMsg.indexOf("Status");
        if (startIndex != -1) {
            statusCode = Integer.parseInt(errorMsg.substring(startIndex + 7, startIndex + 10));
            message = errorMsg.substring(startIndex + 13);
        } else {
            statusCode = UNKNOWN_ERROR_STATUS_CODE;
            message = e.getCause().getMessage();
        }
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

}