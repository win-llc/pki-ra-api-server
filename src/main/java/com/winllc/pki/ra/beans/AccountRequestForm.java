package com.winllc.pki.ra.beans;

public class AccountRequestForm {

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
}
