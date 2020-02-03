package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.AuditRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface AuditRecordRepository extends CrudRepository<AuditRecord, Long> {
    Integer countAllByTypeEquals(AuditRecordType type);
    Integer countAllByTypeEqualsAndTimestampAfterAndTimestampBefore(AuditRecordType type, Timestamp after, Timestamp before);
    List<AuditRecord> findAllByTypeEquals(AuditRecordType type);
    List<AuditRecord> findAllByTypeEqualsAndTimestampAfterAndTimestampBefore(AuditRecordType type, Timestamp after, Timestamp before);
}
