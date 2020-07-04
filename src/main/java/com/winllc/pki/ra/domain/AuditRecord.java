package com.winllc.pki.ra.domain;

import com.winllc.pki.ra.constants.AuditRecordType;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class AuditRecord extends AbstractPersistable<Long> {

    @Column(nullable = false)
    private AuditRecordType type;
    private Timestamp timestamp;
    private String accountKid;
    private String source;

    private String objectClass;
    private String objectUuid;

    public AuditRecord(){}

    public static AuditRecord buildNew(AuditRecordType type){
        AuditRecord record = new AuditRecord();
        record.timestamp = Timestamp.valueOf(LocalDateTime.now());
        record.type = type;
        return record;
    }

    public static AuditRecord buildNew(AuditRecordType type, UniqueEntity uniqueEntity){
        AuditRecord auditRecord = buildNew(type);
        auditRecord.setObjectClass(uniqueEntity.getClass().getCanonicalName());
        auditRecord.setObjectUuid(uniqueEntity.getUuid().toString());
        return auditRecord;
    }

    public AuditRecordType getType() {
        return type;
    }

    public void setType(AuditRecordType type) {
        this.type = type;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getAccountKid() {
        return accountKid;
    }

    public void setAccountKid(String accountKid) {
        this.accountKid = accountKid;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getObjectUuid() {
        return objectUuid;
    }

    public void setObjectUuid(String objectUuid) {
        this.objectUuid = objectUuid;
    }
}
