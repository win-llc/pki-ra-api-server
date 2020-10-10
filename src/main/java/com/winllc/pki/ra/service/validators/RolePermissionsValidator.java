package com.winllc.pki.ra.service.validators;

import com.winllc.pki.ra.beans.form.DomainForm;
import com.winllc.pki.ra.config.PermissionProperties;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.repository.DomainRepository;
import com.winllc.pki.ra.util.FormValidationUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

@Component
public class RolePermissionsValidator implements Validator {

    private static final Logger log = LogManager.getLogger(RolePermissionsValidator.class);

    @Autowired
    private PermissionProperties permissionProperties;

    @Override
    public boolean supports(Class<?> clazz) {
        return DomainForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

   //todo



    }
}
