package com.winllc.pki.ra.beans.validator;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertificateRequestForm;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CertRequestFormValidator implements FormValidator<CertificateRequestForm> {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public ValidationResponse validate(CertificateRequestForm form, boolean editMode) {
        ValidationResponse validationResponse = new ValidationResponse();

        if(form.getAccountId() != null){
            Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());
            if(optionalAccount.isEmpty()){
                validationResponse.addError("accountId", "certRequest.doesNotExist");
            }
        }else{
            validationResponse.addError("accoundId", "certRequest.accountId.empty");
        }

        if(form.getPrimaryDnsDomainId() != null){
            //todo
        }

        try {
            CertUtil.convertPemToPKCS10CertificationRequest(form.getCsr());
        } catch (Exception e) {
            validationResponse.addError("csr", e.getMessage());
        }

        return validationResponse;
    }
}
