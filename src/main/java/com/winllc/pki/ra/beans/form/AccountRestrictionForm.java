package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.constants.AccountRestrictionAction;
import com.winllc.acme.common.constants.AccountRestrictionType;
import com.winllc.acme.common.domain.AccountRestriction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AccountRestrictionForm extends ValidForm<AccountRestriction> {

    private DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    private Long accountId;
    private String type;
    private String action;
    private String dueBy;
    private boolean completed;

    public AccountRestrictionForm(){}

    @Override
    protected void processIsValid() {
        try {
            AccountRestrictionType.valueOf(type);
        }catch (Exception e){
            errors.put("type", "Invalid type");
        }

        try{
            AccountRestrictionAction.valueOf(action);
        }catch (Exception e){
            errors.put("action", "Invalid action");
        }

        try {
            LocalDateTime.from(formatter.parse(dueBy));
        }catch (Exception e){
            errors.put("dueBy", "Invalid dueBy");
        }
    }

    public AccountRestrictionForm(AccountRestriction restriction){
        super(restriction);
        this.accountId = restriction.getAccount().getId();
        this.type = restriction.getType().name();
        this.action = restriction.getAction().name();
        this.dueBy = restriction.getDueBy().toString();
        this.completed = restriction.isCompleted();
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
