package com.winllc.pki.ra.beans.validator;

import com.winllc.pki.ra.beans.ServerEntryForm;
import com.winllc.pki.ra.domain.ServerEntry;
import org.springframework.util.CollectionUtils;

public class ServerEntryFormValidator implements FormValidator<ServerEntryForm> {

    public boolean validate(ServerEntryForm form){
        if(!CollectionUtils.isEmpty(form.getAlternateDnsValues())){
            for(String fqdn : form.getAlternateDnsValues()){
                boolean valid = isValidServerName(fqdn);
                if(!valid) return false;
            }
        }
        return true;
    }

    private boolean isValidServerName(String fqdn){
        //todo
        return true;
    }
}
