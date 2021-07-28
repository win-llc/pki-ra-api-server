package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.constants.AccountStatusType;
import com.winllc.acme.common.domain.AccountRequest;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class AccountRequestUpdateForm extends ValidForm<AccountRequest> {

    @NotNull
    private Long accountRequestId;
    @NotEmpty(message = "State must not be empty")
    private String state;

    public Long getAccountRequestId() {
        return accountRequestId;
    }

    public void setAccountRequestId(Long accountRequestId) {
        this.accountRequestId = accountRequestId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }


    @Override
    protected void processIsValid() {
        try{
            AccountStatusType.valueToType(state);
        }catch (Exception e){
            e.printStackTrace();
            errors.put("state", "Invalid state");
        }
    }

}
