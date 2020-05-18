package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.AuditRecord;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.List;

@Repository
@Transactional
public interface AuditRecordRepository extends BaseRepository<AuditRecord> {
    Integer countAllByTypeEquals(AuditRecordType type);
    Integer countAllByTypeEqualsAndTimestampAfterAndTimestampBefore(AuditRecordType type, Timestamp after, Timestamp before);
    List<AuditRecord> findAllByTypeEquals(AuditRecordType type);
    List<AuditRecord> findAllByTypeEqualsAndTimestampAfterAndTimestampBefore(AuditRecordType type, Timestamp after, Timestamp before);
}
