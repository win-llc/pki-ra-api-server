package com.winllc.pki.ra.service.validators;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.AppKeyStoreEntryForm;
import com.winllc.pki.ra.beans.form.AttributePolicyGroupForm;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AttributePolicy;
import com.winllc.pki.ra.repository.AccountRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

@Component
public class AttributePolicyGroupValidator implements Validator {

    private static final Logger log = LogManager.getLogger(AttributePolicyGroupValidator.class);

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return AttributePolicyGroupForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        AttributePolicyGroupForm form = (AttributePolicyGroupForm) target;

        Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
        if(optionalAccount.isEmpty()){
            errors.rejectValue("accountId", "attributePolicyGroup.invalidAccountId");
        }

        for(AttributePolicy policy : form.getAttributePolicies()){
            //must be one or the other

            if(StringUtils.isEmpty(policy.getAttributeName())){
                errors.rejectValue("attributeName", "attributePolicyGroup.invalidName");
            }
        }

    }
}
