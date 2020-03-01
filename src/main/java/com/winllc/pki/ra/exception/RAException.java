package com.winllc.pki.ra.exception;

import com.winllc.pki.ra.beans.validator.ValidationResponse;

import java.util.Map;

public class RAException extends Exception {

    private Map<String, String> errors;

    public RAException(String message) {
        super(message);
    }

    public RAException(ValidationResponse validationResponse){
        if(!validationResponse.isValid()) this.errors = validationResponse.getErrors();
    }

    protected RAException(){
        super();
    }
}
