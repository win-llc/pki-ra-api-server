package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.AccountRequest;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class AccountRequestForm extends ValidForm<AccountRequest> {


    @NotEmpty(message = "State must not be empty")
    private String state;
    @NotEmpty
    @Email(message = "Email not valid")
    private String accountOwnerEmail;
    @NotEmpty(message = "Project Name must not be empty")
    private String projectName;
    private String securityPolicyServerProjectId;

    public AccountRequestForm() {}

    public AccountRequestForm(AccountRequest entity) {
        super(entity);
        this.state = entity.getState();
        this.accountOwnerEmail = entity.getAccountOwnerEmail();
        this.projectName = entity.getProjectName();
        this.securityPolicyServerProjectId = entity.getSecurityPolicyServerProjectId();
    }


    @Override
    protected void processIsValid() {

    }

    @Override
    public String toString() {
        return "AccountRequestForm{" +
                "accountOwnerEmail='" + accountOwnerEmail + '\'' +
                ", projectName='" + projectName + '\'' +
                ", securityPolicyProjectId='" + securityPolicyServerProjectId + '\'' +
                "} " + super.toString();
    }
}
