package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public interface AuditRecordRepository extends PagingAndSortingRepository<AuditRecord, Long> {
    List<AuditRecord> findAll();
    Integer countAllByTypeEquals(AuditRecordType type);
    Integer countAllByTypeEqualsAndTimestampAfterAndTimestampBefore(AuditRecordType type, Timestamp after, Timestamp before);
    List<AuditRecord> findAllByTypeEquals(AuditRecordType type);
    List<AuditRecord> findAllByTypeEqualsAndTimestampAfterAndTimestampBefore(AuditRecordType type, Timestamp after, Timestamp before);

    Page<AuditRecord> findAllByType(AuditRecordType type, Pageable pageable);

    //todo, use this to find audit records for an entry
    Page<AuditRecord> findAllByObjectClassAndObjectUuid(String objectClass, String uuid, Pageable pageable);
}
