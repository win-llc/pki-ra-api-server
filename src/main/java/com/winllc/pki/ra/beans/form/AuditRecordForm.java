package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.constants.AuditRecordType;
import com.winllc.acme.common.domain.AuditRecord;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
public class AuditRecordForm extends ValidForm<AuditRecord> {

    private AuditRecordType type;
    private ZonedDateTime timestamp;
    private String accountKid;
    private String source;
    private String objectClass;
    private String objectUuid;

    public AuditRecordForm(AuditRecord entity) {
        super(entity);

        setType(entity.getType());
        setTimestamp(entity.getTimestamp());
        setAccountKid(entity.getAccountKid());
        setSource(entity.getSource());
        setObjectClass(entity.getObjectClass());
        setObjectUuid(entity.getObjectUuid());
    }


    @Override
    protected void processIsValid() {

    }
}
