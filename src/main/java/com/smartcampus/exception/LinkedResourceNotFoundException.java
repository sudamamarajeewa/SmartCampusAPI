package com.smartcampus.exception;

/**
 * Thrown when a POST /sensors request references a roomId that does not exist.
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 *
 * Unlike a 404 (resource path not found), 422 signals that the request is syntactically
 * valid JSON but semantically invalid — the referenced resource doesn't exist.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private final String fieldName;
    private final String missingId;

    public LinkedResourceNotFoundException(String fieldName, String missingId) {
        super("The referenced '" + fieldName + "' with id '" + missingId
                + "' does not exist. Please create it before linking.");
        this.fieldName = fieldName;
        this.missingId = missingId;
    }

    public String getFieldName()  { return fieldName; }
    public String getMissingId()  { return missingId; }
}
