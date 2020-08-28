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
    private String securityPolicyProjectId;

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

    public String getSecurityPolicyProjectId() {
        return securityPolicyProjectId;
    }

    public void setSecurityPolicyProjectId(String securityPolicyProjectId) {
        this.securityPolicyProjectId = securityPolicyProjectId;
    }

    @Override
    protected void processIsValid() {

    }

    @Override
    public String toString() {
        return "AccountRequestForm{" +
                "accountOwnerEmail='" + accountOwnerEmail + '\'' +
                ", projectName='" + projectName + '\'' +
                ", securityPolicyProjectId='" + securityPolicyProjectId + '\'' +
                "} " + super.toString();
    }
}
