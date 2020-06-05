package com.winllc.pki.ra.exception;

public class NotAuthorizedException extends RAException {
    public NotAuthorizedException(String user, String action){
        super(user + " not authorized: "+action);
    }
}
