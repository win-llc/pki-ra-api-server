package com.winllc.pki.ra.beans.validator;

import com.winllc.pki.ra.beans.form.ServerEntryForm;
import org.springframework.util.CollectionUtils;

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
        //todo
        return true;
    }
}
