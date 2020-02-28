package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.SettingsDocument;

public class AcmeSettingsForm<T extends SettingsDocument> {

    private String id;

    protected AcmeSettingsForm(T entity){
        this.id = entity.getId();
    }

    protected AcmeSettingsForm(){}

    public String getId() {
        return id;
    }
}
