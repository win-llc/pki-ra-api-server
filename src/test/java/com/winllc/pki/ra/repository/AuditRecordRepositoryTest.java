package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.AuditRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class AuditRecordRepositoryTest {

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @BeforeEach
    @Transactional
    void before(){
        AuditRecord auditRecord = new AuditRecord();
        auditRecord.setAccountKid("test1");
        auditRecord.setTimestamp(Timestamp.valueOf(LocalDateTime.now()));
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
        Timestamp notAfter = Timestamp.valueOf(LocalDateTime.now().plusDays(1));
        Timestamp notBefore = Timestamp.valueOf(LocalDateTime.now().minusDays(1));
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
        Timestamp notAfter = Timestamp.valueOf(LocalDateTime.now().plusDays(1));
        Timestamp notBefore = Timestamp.valueOf(LocalDateTime.now().minusDays(1));
        List<AuditRecord> records = auditRecordRepository.findAllByTypeEqualsAndTimestampAfterAndTimestampBefore(AuditRecordType.CERTIFICATE_ISSUED,
                notBefore, notAfter);
        assertEquals(1, records.size());
    }
}