package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.constants.AccountRestrictionAction;
import com.winllc.acme.common.constants.AccountRestrictionType;
import com.winllc.acme.common.domain.AccountRestriction;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class AccountRestrictionForm extends ValidForm<AccountRestriction> {

    public static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

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
        if(restriction.getAccount() != null) {
            this.accountId = restriction.getAccount().getId();
        }
        if(restriction.getType() != null) {
            this.type = restriction.getType().name();
        }
        if(restriction.getAction() != null) {
            this.action = restriction.getAction().name();
        }
        if(restriction.getDueBy() != null) {
            this.dueBy = restriction.getDueBy().toString();
        }
        this.completed = restriction.isCompleted();
    }


}
