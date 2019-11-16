package com.winllc.pki.ra.exception;

public class AcmeConnectionException extends Exception {
    public AcmeConnectionException(String error){
        super(error);
    }

    public AcmeConnectionException(Throwable cause) {
        super(cause);
    }
}
