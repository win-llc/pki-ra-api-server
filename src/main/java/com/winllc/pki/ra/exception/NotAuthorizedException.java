package com.winllc.pki.ra.exception;

import com.winllc.pki.ra.domain.User;

public class NotAuthorizedException extends RAException {
    public NotAuthorizedException(User user, String action){
        super(user.getUsername() + " not authorized: "+action);
    }
}
