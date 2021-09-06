package com.winllc.acme.common.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.acme.common.constants.AuditRecordType;
import com.winllc.acme.common.domain.AuditRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditRecordRepositoryTest extends BaseTest {

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @BeforeEach
    @Transactional
    void before(){
        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setAccountKid("test1");
        auditRecord.setTimestamp(ZonedDateTime.now());
        auditRecord.setType(AuditRecordType.CERTIFICATE_ISSUED);

        auditRecordRepository.save(auditRecord);
    }

    @AfterEach
    @Transactional
    void after(){
        auditRecordRepository.deleteAll();
    }

    @Test
    void countAllByTypeEquals() {
        Integer count = auditRecordRepository.countAllByTypeEquals(AuditRecordType.CERTIFICATE_ISSUED);
        assertEquals(1, count);
    }

    @Test
    void countAllByTypeEqualsAndTimestampAfterAndTimestampBefore() {
        ZonedDateTime notAfter = ZonedDateTime.now().plusDays(1);
        ZonedDateTime notBefore = ZonedDateTime.now().minusDays(1);
        Integer count = auditRecordRepository.countAllByTypeEqualsAndTimestampAfterAndTimestampBefore(AuditRecordType.CERTIFICATE_ISSUED,
                notBefore, notAfter);
        assertEquals(1, count);
    }

    @Test
    void findAllByTypeEquals() {
        List<AuditRecord> records = auditRecordRepository.findAllByTypeEquals(AuditRecordType.CERTIFICATE_ISSUED);
        assertEquals(1, records.size());
    }

    @Test
    void findAllByTypeEqualsAndTimestampAfterAndTimestampBefore() {
        ZonedDateTime notAfter = ZonedDateTime.now().plusDays(1);
        ZonedDateTime notBefore = ZonedDateTime.now().minusDays(1);
        List<AuditRecord> records = auditRecordRepository.findAllByTypeEqualsAndTimestampAfterAndTimestampBefore(AuditRecordType.CERTIFICATE_ISSUED,
                notBefore, notAfter);
        assertEquals(1, records.size());
    }
}