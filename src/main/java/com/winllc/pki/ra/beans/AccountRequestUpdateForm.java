package com.winllc.pki.ra.beans;

public class AccountRequestUpdateForm {

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
}
