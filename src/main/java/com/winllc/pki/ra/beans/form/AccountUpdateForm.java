package com.winllc.pki.ra.beans.form;

import com.winllc.pki.ra.beans.PocFormEntry;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.regex.Pattern;

public class AccountUpdateForm extends ValidForm {
    //TODO

    private static final String VALID_EMAIL_REGEX = "^(.+)@(.+)$";

    private List<PocFormEntry> pocEmails;
    private boolean acmeRequireHttpValidation;

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

    public boolean isValid(){
        boolean valid;
        if(!CollectionUtils.isEmpty(pocEmails)){
            Pattern pattern = Pattern.compile(VALID_EMAIL_REGEX);
            valid = pocEmails.stream()
                    .allMatch(p -> pattern.matcher(p.getEmail()).matches());
        }else{
            valid = true;
        }
        return valid;
    }
}
