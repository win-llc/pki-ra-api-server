package com.winllc.pki.ra.beans.form;

import javax.validation.constraints.NotEmpty;
import java.util.UUID;

public class UniqueEntityLookupForm {
    @NotEmpty
    private String objectClass;
    @NotEmpty
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
