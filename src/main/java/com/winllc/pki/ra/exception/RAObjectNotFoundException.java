package com.winllc.pki.ra.exception;

import com.winllc.pki.ra.beans.form.ValidForm;

public class RAObjectNotFoundException extends RAException {

    public RAObjectNotFoundException(ValidForm form){
        this(form.getFormObjectType(), form.getId());
    }

    public RAObjectNotFoundException(Class clazz, String id){
        super("Could not find object of: "+clazz.getName() + ", with ID: "+id);
    }

    public RAObjectNotFoundException(Class clazz, Long id){
        this(clazz, Long.toString(id));
    }
}
