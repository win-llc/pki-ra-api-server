package com.winllc.pki.ra.exception;

public class RAException extends Exception {
    public RAException(String message) {
        super(message);
    }

    protected RAException(){
        super();
    }
}
