package com.winllc.pki.ra.beans.validator;

import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.repository.CertAuthorityConnectionInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CertAuthorityConnectionInfoValidator implements FormValidator<CertAuthorityConnectionInfo> {

    @Autowired
    private CertAuthorityConnectionInfoRepository repository;

    @Override
    public boolean validate(CertAuthorityConnectionInfo form, boolean editMode) {
        //todo
        Optional<CertAuthorityConnectionInfo> connectionInfoOptional = repository.findByName(form.getName());

        if(connectionInfoOptional.isPresent()){
            //if object exists and not edit mode, not a valid operation
            if(!editMode){
                return false;
            }
        }

        return true;
    }
}
