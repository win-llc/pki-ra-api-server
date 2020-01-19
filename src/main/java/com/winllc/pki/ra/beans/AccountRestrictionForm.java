package com.winllc.pki.ra.beans;

import com.winllc.pki.ra.domain.AccountRestriction;

public class AccountRestrictionForm {

    private Long id;
    private Long accountId;
    private String type;
    private String action;
    private String dueBy;
    private boolean completed;

    public AccountRestrictionForm(){}

    public AccountRestrictionForm(AccountRestriction restriction){
        this.id = restriction.getId();
        this.accountId = restriction.getAccount().getId();
        this.type = restriction.getType().name();
        this.action = restriction.getAction().name();
        this.dueBy = restriction.getDueBy().toString();
        this.completed = restriction.isCompleted();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDueBy() {
        return dueBy;
    }

    public void setDueBy(String dueBy) {
        this.dueBy = dueBy;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
