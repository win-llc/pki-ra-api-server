package com.winllc.pki.ra.service.validators;

import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.beans.form.AccountRequestUpdateForm;
import com.winllc.pki.ra.service.SecurityPolicyService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class AccountRequestUpdateValidator implements Validator {

    private static final Logger log = LogManager.getLogger(AccountRequestUpdateValidator.class);

    private final SecurityPolicyService securityPolicyService;

    public AccountRequestUpdateValidator(SecurityPolicyService securityPolicyService) {
        this.securityPolicyService = securityPolicyService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return AccountRequestUpdateForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        AccountRequestUpdateForm form = (AccountRequestUpdateForm) target;

        if(!form.getState().equalsIgnoreCase("approve") && !form.getState().equalsIgnoreCase("reject")){
            errors.rejectValue("state", "accountrequest.update.invalidstate");
        }
    }
}
