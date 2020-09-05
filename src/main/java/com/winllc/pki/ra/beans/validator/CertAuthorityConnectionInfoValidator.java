package com.winllc.pki.ra.beans.validator;

import com.winllc.pki.ra.beans.form.CertAuthorityConnectionInfoForm;
import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.repository.CertAuthorityConnectionInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CertAuthorityConnectionInfoValidator implements FormValidator<CertAuthorityConnectionInfoForm> {

    @Autowired
    private CertAuthorityConnectionInfoRepository repository;

    @Override
    public ValidationResponse validate(CertAuthorityConnectionInfoForm form, boolean editMode) {
        //todo
        ValidationResponse validationResponse = new ValidationResponse();
        Optional<CertAuthorityConnectionInfo> connectionInfoOptional = repository.findByName(form.getName());

        if(connectionInfoOptional.isPresent()){
            CertAuthorityConnectionInfo connectionInfo = connectionInfoOptional.get();
            //if object exists and not edit mode, not a valid operation

            //connectionInfo
        }

        return validationResponse;
    }
}
