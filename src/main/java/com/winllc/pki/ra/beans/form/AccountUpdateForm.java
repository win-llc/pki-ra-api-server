package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.util.FormValidationUtil;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

public class AccountUpdateForm extends ValidForm<Account> {

    private List<PocFormEntry> pocEmails;
    private boolean acmeRequireHttpValidation;

    public AccountUpdateForm(Account entity) {
        super(entity);
    }

    private AccountUpdateForm(){}

    @Override
    protected void processIsValid() {
        if(!CollectionUtils.isEmpty(pocEmails)){
            List<String> invalidEmails = pocEmails.stream()
                    .filter(p -> !FormValidationUtil.isValidEmailAddress(p.getEmail()))
                    .map(p -> p.getEmail())
                    .collect(Collectors.toList());
            if(invalidEmails.size() > 0){
                errors.put("pocEmails", "Invalid emails: "+String.join(", ", invalidEmails));
            }
        }
    }

    public List<PocFormEntry> getPocEmails() {
        return pocEmails;
    }

    public void setPocEmails(List<PocFormEntry> pocEmails) {
        this.pocEmails = pocEmails;
    }

    public boolean isAcmeRequireHttpValidation() {
        return acmeRequireHttpValidation;
    }

    public void setAcmeRequireHttpValidation(boolean acmeRequireHttpValidation) {
        this.acmeRequireHttpValidation = acmeRequireHttpValidation;
    }
}
