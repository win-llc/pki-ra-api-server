package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.domain.AccountRequest;

public class AccountRequestUpdateForm extends ValidForm<AccountRequest> {

    private Long accountRequestId;
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
    protected boolean isValid() {
        //todo
        return true;
    }
}
