package com.winllc.pki.ra.beans.validator;

import com.winllc.pki.ra.beans.form.ServerEntryForm;
import org.springframework.util.CollectionUtils;

import java.util.regex.Pattern;

import static com.winllc.pki.ra.constants.ValidationRegex.FQDN_VALIDATION_REGEX;

public class ServerEntryFormValidator implements FormValidator<ServerEntryForm> {

    public ValidationResponse validate(ServerEntryForm form, boolean editMode){
        ValidationResponse validationResponse = new ValidationResponse();
        if(!CollectionUtils.isEmpty(form.getAlternateDnsValues())){
            for(String fqdn : form.getAlternateDnsValues()){
                boolean valid = isValidServerName(fqdn);
                if(!valid) validationResponse.addError("fqdn", "Is not valid fqdn: "+fqdn);
            }
        }
        return validationResponse;
    }

    private boolean isValidServerName(String fqdn){
        Pattern fqdnPattern = Pattern.compile(FQDN_VALIDATION_REGEX);
        return fqdnPattern.matcher(fqdn).matches();
    }
}
