package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.AccountRequest;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

public class AccountRequestForm extends ValidForm<AccountRequest> {

    @NotEmpty
    @Email(message = "Email not valid")
    private String accountOwnerEmail;
    @NotEmpty(message = "Project Name must not be empty")
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
    protected void processIsValid() {
        System.out.println();
    }

}
