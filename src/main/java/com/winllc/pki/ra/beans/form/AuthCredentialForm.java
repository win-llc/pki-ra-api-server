package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.AuthCredential;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import java.time.ZonedDateTime;

@Getter
@Setter
public class AuthCredentialForm extends ValidForm<AuthCredential> {

    private String keyIdentifier;
    private String macKeyBase64;
    private boolean valid = false;
    private String pocAssignedTo;
    private String createdOn;
    private String expiresOn;

    public AuthCredentialForm(AuthCredential entity) {
        super(entity);
        setKeyIdentifier(entity.getKeyIdentifier());
        setMacKeyBase64(entity.getMacKeyBase64());
        setValid(entity.getValid() != null ? entity.getValid() : false);
        setPocAssignedTo(entity.getPocAssignedTo());
        if(entity.getCreatedOn() != null) setCreatedOn(entity.getCreatedOn().toString());
        if(entity.getExpiresOn() != null) setExpiresOn(entity.getExpiresOn().toString());
    }

    public AuthCredentialForm() {
        super();
    }

    @Override
    protected void processIsValid() {

    }
}
