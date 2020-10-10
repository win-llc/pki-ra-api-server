package com.winllc.pki.ra.service.validators;

import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.pki.ra.beans.form.AccountRestrictionForm;
import com.winllc.pki.ra.beans.form.AccountUpdateForm;
import com.winllc.pki.ra.util.FormValidationUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccountUpdateValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return AccountUpdateForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        AccountUpdateForm form = (AccountUpdateForm) target;

        final List<PocFormEntry> pocEmails = form.getPocEmails();
        if(!CollectionUtils.isEmpty(pocEmails)){
            List<String> invalidEmails = pocEmails.stream()
                    .filter(p -> !FormValidationUtil.isValidEmailAddress(p.getEmail()))
                    .map(p -> p.getEmail())
                    .collect(Collectors.toList());
            if(invalidEmails.size() > 0){
                errors.rejectValue("pocEmails", "accountupdate.pocEmails",
                        new Object[]{String.join(", ", invalidEmails)}, "invalid email(s)");
            }
        }
    }
}
