package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.RevocationRequest;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;

@Getter
@Setter
public class RevocationRequestForm extends ValidForm<RevocationRequest> {

    private String subjectDn;
    private String issuerDn;
    private String serial;
    private Integer reason;
    private String requestBy;
    private String requestedOn;

    public RevocationRequestForm(RevocationRequest entity) {
        super(entity);

        setSubjectDn(entity.getSubjectDn());
        setIssuerDn(entity.getIssuerDn());
        setSerial(entity.getSerial());
        setReason(entity.getReason());
        setRequestBy(entity.getRequestedBy());
        if(entity.getRequestedOn() != null) {
            setRequestedOn(entity.getRequestedOn().toString());
        }
    }

    public RevocationRequestForm() {
    }

    @Override
    protected void processIsValid() {

    }
}
