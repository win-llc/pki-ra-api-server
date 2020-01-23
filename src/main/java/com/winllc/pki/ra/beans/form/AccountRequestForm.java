package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.AccountRequest;

public class AccountRequestForm extends ValidForm<AccountRequest> {

    private String accountOwnerEmail;
    private String projectName;

    public String getAccountOwnerEmail() {
        return accountOwnerEmail;
    }

    public void setAccountOwnerEmail(String accountOwnerEmail) {
        this.accountOwnerEmail = accountOwnerEmail;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    protected boolean isValid() {
        //todo
        return true;
    }
}
