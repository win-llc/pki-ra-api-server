package com.winllc.pki.ra.beans.validator;

import java.util.HashMap;
import java.util.Map;

public class ValidationResponse {
    private Map<String, String> errors;

    public ValidationResponse(){
        this.errors = new HashMap<>();
    }

    public void addError(String field, String message){
        this.errors.put(field, message);
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public boolean isValid(){
        return this.errors.size() == 0;
    }
}
