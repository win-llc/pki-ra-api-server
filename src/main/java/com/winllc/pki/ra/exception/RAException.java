package com.winllc.pki.ra.exception;

import com.winllc.pki.ra.beans.validator.ValidationResponse;

import java.util.Map;

public class RAException extends Exception {

    private Map<String, String> errors;
    private Exception rootException;

    public RAException(String message) {
        super(message);
    }

    public RAException(String message, Exception e){
        super(message);
        this.rootException = e;
    }

    public RAException(ValidationResponse validationResponse){
        if(!validationResponse.isValid()) this.errors = validationResponse.getErrors();
    }

    public RAException(){
        super();
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public Exception getRootException() {
        return rootException;
    }
}
