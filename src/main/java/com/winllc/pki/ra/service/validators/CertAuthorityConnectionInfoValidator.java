package com.winllc.pki.ra.service.validators;

import com.winllc.acme.common.domain.CertAuthorityConnectionProperty;
import com.winllc.acme.common.keystore.ApplicationKeystore;
import com.winllc.pki.ra.beans.form.CertAuthorityConnectionInfoForm;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStoreException;
import java.util.List;

@Component
public class CertAuthorityConnectionInfoValidator implements Validator {

    private static final Logger log = LogManager.getLogger(CertAuthorityConnectionInfoValidator.class);

    private final ApplicationKeystore applicationKeystore;

    public CertAuthorityConnectionInfoValidator(ApplicationKeystore applicationKeystore) {
        this.applicationKeystore = applicationKeystore;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return CertAuthorityConnectionInfoForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        CertAuthorityConnectionInfoForm form = (CertAuthorityConnectionInfoForm) target;

        if(StringUtils.isNotBlank(form.getAuthKeyAlias())){
            try {
                List<String> allAliases = applicationKeystore.getAllAliases();
                if(!allAliases.contains(form.getAuthKeyAlias())){
                    errors.rejectValue("authKeyAlias", "certAuthorityConnectionInfo.invalidAuthKeyAlias");
                }
            } catch (KeyStoreException e) {
                log.error("Could not check keystore", e);
                errors.reject("system.error");

            }
        }

        if(StringUtils.isNotBlank(form.getBaseUrl())){
            try {
                new URL(form.getBaseUrl());
            } catch (MalformedURLException e) {
                log.info("Bad URL", e);
                errors.rejectValue("baseUrl", "certAuthorityConnectionInfo.invalidBaseUrl");
            }
        }

        if(!CollectionUtils.isEmpty(form.getProperties())){
            for(CertAuthorityConnectionProperty prop : form.getProperties()){
                if(StringUtils.isBlank(prop.getName())){
                    errors.rejectValue("propertyName", "certAuthorityConnectionInfo.invalidPropertyName");
                }
            }
        }
    }
}
