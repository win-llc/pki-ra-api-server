package com.winllc.pki.ra.service.validators;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertAuthorityConnectionInfoForm;
import com.winllc.pki.ra.beans.form.CertificateRequestForm;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.repository.AccountRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

@Component
public class CertificateRequestValidator implements Validator {

    private static final Logger log = LogManager.getLogger(CertificateRequestValidator.class);

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return CertificateRequestForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        CertificateRequestForm form = (CertificateRequestForm) target;

        Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
        if(!optionalAccount.isPresent()){
            errors.rejectValue("accountId", "certificaterequest.invalidAccountId");
        }

        try {
            CertUtil.convertPemToPKCS10CertificationRequest(form.getCsr());
        } catch (Exception e) {
            errors.rejectValue("csr", "certificaterequest.invalidcsr");
        }
    }
}
