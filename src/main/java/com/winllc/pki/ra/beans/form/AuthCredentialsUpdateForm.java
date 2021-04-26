package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.AuthCredential;

import java.util.List;

public class AuthCredentialsUpdateForm {

    private UniqueEntityLookupForm lookupForm;
    private List<AuthCredential> authCredentials;

    public UniqueEntityLookupForm getLookupForm() {
        return lookupForm;
    }

    public void setLookupForm(UniqueEntityLookupForm lookupForm) {
        this.lookupForm = lookupForm;
    }

    public List<AuthCredential> getAuthCredentials() {
        return authCredentials;
    }

    public void setAuthCredentials(List<AuthCredential> authCredentials) {
        this.authCredentials = authCredentials;
    }
}
