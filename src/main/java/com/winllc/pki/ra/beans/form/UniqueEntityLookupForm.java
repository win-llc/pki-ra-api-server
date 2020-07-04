package com.winllc.pki.ra.beans.form;

import java.util.UUID;

public class UniqueEntityLookupForm {
    private String objectClass;
    private String objectUuid;


    public UUID convertObjectUuid(){
        return UUID.fromString(objectUuid);
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getObjectUuid() {
        return objectUuid;
    }

    public void setObjectUuid(String objectUuid) {
        this.objectUuid = objectUuid;
    }
}
