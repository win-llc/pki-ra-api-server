package com.winllc.pki.ra.service.validators;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.pki.ra.beans.form.AccountUpdateForm;
import com.winllc.pki.ra.beans.form.AppKeyStoreEntryForm;
import com.winllc.pki.ra.util.FormValidationUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AppKeyStoreEntryValidator implements Validator {

    private static final Logger log = LogManager.getLogger(AppKeyStoreEntryValidator.class);

    @Override
    public boolean supports(Class<?> clazz) {
        return AppKeyStoreEntryForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        AppKeyStoreEntryForm form = (AppKeyStoreEntryForm) target;

        if(StringUtils.isNotEmpty(form.getUploadCertificate())){
            try {
                CertUtil.base64ToCert(form.getUploadCertificate());
            } catch (Exception e) {
                log.error("Could not parse cert", e);
                errors.rejectValue("uploadCertificate", "appkeystoreentry.invaliduploadcertificate");
            }
        }
    }
}
