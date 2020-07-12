package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.UniqueEntityLookupForm;
import com.winllc.pki.ra.beans.info.InfoObject;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.repository.*;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakOIDCProviderConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class AuditRecordServiceTest {

    @Autowired
    private AuditRecordService auditRecordService;
    @Autowired
    private AuditRecordRepository auditRecordRepository;
    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private DomainPolicyRepository domainPolicyRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = Account.buildNew("Test Project 3");
        account.setKeyIdentifier("kidtest1");
        account.setMacKey("testmac1");
        account = accountRepository.save(account);

        PocEntry pocEntry = PocEntry.buildNew("test@test.com", account);
        pocEntryRepository.save(pocEntry);

        Domain domain = new Domain();
        domain.setBase("winllc-dev.com");
        domain = domainRepository.save(domain);

        DomainPolicy domainPolicy = new DomainPolicy(domain);
        domainPolicy = domainPolicyRepository.save(domainPolicy);
        account.getAccountDomainPolicies().add(domainPolicy);

        accountRepository.save(account);

        ServerEntry serverEntry = ServerEntry.buildNew();
        serverEntry.setFqdn("test.winllc-dev.com");
        serverEntry.setAccount(account);
        serverEntry.setDomainParent(domain);
        serverEntry = serverEntryRepository.save(serverEntry);

        account.getServerEntries().add(serverEntry);
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        domainRepository.deleteAll();
        serverEntryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        pocEntryRepository.deleteAll();
        auditRecordRepository.deleteAll();
    }

    @Test
    void save() {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        AuditRecord auditRecord = AuditRecord.buildNew(AuditRecordType.SERVER_ENTRY_ADDED, serverEntry);

        auditRecordService.save(auditRecord);

        List<AuditRecord> recordList = auditRecordRepository.findAll();
        assertEquals(1, recordList.size());
    }

    @Test
    void getPagedRecordsByType() {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        AuditRecord auditRecord = AuditRecord.buildNew(AuditRecordType.SERVER_ENTRY_ADDED, serverEntry);
        auditRecordService.save(auditRecord);

        Pageable unpaged = Pageable.unpaged();

        Page<AuditRecord> pagedRecordsByType = auditRecordService.getPagedRecordsByType(AuditRecordType.SERVER_ENTRY_ADDED.name(), unpaged);

        assertEquals(1, pagedRecordsByType.getTotalElements());

        pagedRecordsByType = auditRecordService.getPagedRecordsByType(AuditRecordType.SERVER_ENTRY_REMOVED.name(), unpaged);

        assertEquals(0, pagedRecordsByType.getTotalElements());
    }

    @Test
    void getAll() {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        AuditRecord auditRecord = AuditRecord.buildNew(AuditRecordType.SERVER_ENTRY_ADDED, serverEntry);
        auditRecordService.save(auditRecord);

        Pageable unpaged = Pageable.unpaged();
        Page<AuditRecord> all = auditRecordService.getAll(unpaged);
        assertTrue(all.getTotalElements() > 0);
    }

    @Test
    void getRecordsForEntity() {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        AuditRecord auditRecord = AuditRecord.buildNew(AuditRecordType.SERVER_ENTRY_ADDED, serverEntry);
        auditRecordService.save(auditRecord);

        UniqueEntityLookupForm uniqueEntityLookupForm = new UniqueEntityLookupForm();
        uniqueEntityLookupForm.setObjectClass(serverEntry.getClass().getCanonicalName());
        uniqueEntityLookupForm.setObjectUuid(serverEntry.getUuid().toString());

        Pageable unpaged = Pageable.unpaged();
        Page<AuditRecord> recordsForEntity = auditRecordService.getRecordsForEntity(uniqueEntityLookupForm, unpaged);
        assertEquals(1, recordsForEntity.getTotalElements());
    }
}