package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.DomainPolicyForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.DomainPolicy;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainPolicyRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class DomainPolicyServiceTest {

    @Autowired
    private DomainPolicyService restrictionService;
    @Autowired
    private DomainPolicyRepository restrictionRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    @Transactional
    void before() throws Exception {
        Domain domain = new Domain();
        domain.setBase("test.com");
        domainRepository.save(domain);
    }

    @AfterEach
    @Transactional
    void after() throws Exception {
        domainRepository.deleteAll();
        accountRepository.deleteAll();
        restrictionRepository.deleteAll();
    }

    @Test
    @Transactional
    void getRestrictionsForType() throws Exception {
        Account account = Account.buildNew("Test Project");
        account = accountRepository.save(account);

        Domain domain = domainRepository.findDistinctByBaseEquals("test.com").get();

        DomainPolicy restriction = new DomainPolicy();
        restriction.setTargetDomain(domain);

        restriction = restrictionRepository.save(restriction);

        domain.getAllDomainPolicies().add(restriction);
        domainRepository.save(domain);

        account.getAccountDomainPolicies().add(restriction);
        accountRepository.save(account);

        Set<DomainPolicyForm> restrictions = restrictionService.getRestrictionsForType("account", account.getId());
        assertEquals(1, restrictions.size());
    }

    @Test
    @Transactional
    void addRestrictionForType() throws Exception {
        Account account = Account.buildNew("Test Project");
        account = accountRepository.save(account);

        Domain domain = domainRepository.findDistinctByBaseEquals("test.com").get();
        DomainPolicy restriction = new DomainPolicy();
        restriction.setTargetDomain(domain);

        DomainPolicyForm form = new DomainPolicyForm(restriction);

        restrictionService.addForType("account", account.getId(), form);

        Account foundAccount = accountRepository.findById(account.getId()).get();
        assertEquals(1, foundAccount.getAccountDomainPolicies().size());
    }

    @Test
    @Transactional
    void deleteRestrictionForType() throws Exception {
        Account account = Account.buildNew("Test Project");
        account = accountRepository.save(account);

        Domain domain = domainRepository.findDistinctByBaseEquals("test.com").get();
        DomainPolicy restriction = new DomainPolicy();
        restriction.setTargetDomain(domain);

        DomainPolicyForm form = new DomainPolicyForm(restriction);

        Long restrictionId = restrictionService.addForType("account", account.getId(), form);

        account = accountRepository.findById(account.getId()).get();
        assertEquals(1, account.getDomainIssuanceRestrictions().size());

        restrictionService.deleteForType("account", account.getId(), restrictionId);

        account = accountRepository.findById(account.getId()).get();
        assertEquals(0, account.getDomainIssuanceRestrictions().size());
    }
}