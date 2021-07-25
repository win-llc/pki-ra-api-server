package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.service.AccountService;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DomainRepositoryTest extends BaseTest {

    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private DomainPolicyRepository domainPolicyRepository;

    @BeforeEach
    @Transactional
    void before() throws Exception {
        AccountRequestForm form = new AccountRequestForm();
        form.setProjectName("Test Project");
        Long id = accountService.createNewAccount(form);
        Account account = accountRepository.findById(id).get();

        Domain domain1 = new Domain();
        domain1.setBase("test8.winllc-dev.com");
        domain1 = domainRepository.save(domain1);

        Domain domain2 = new Domain();
        domain2.setBase("test9.winllc-dev.com");
        domain2 = domainRepository.save(domain2);
    }

    @AfterEach
    @Transactional
    void after(){
        domainPolicyRepository.deleteAll();;
        domainRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void findAllByBaseContains() {
        List<Domain> allByBaseContains = domainRepository.findAllByBaseContains("winllc-dev.com");
        assertEquals(2, allByBaseContains.size());
    }

    @Test
    void findAllByIdIn() {
        Domain domain = domainRepository.findAll().get(0);
        List<Domain> allByIdIn = domainRepository.findAllByIdIn(Collections.singleton(domain.getId()));
        assertEquals(1, allByIdIn.size());
    }

    @Test
    @Transactional
    void subDomainTest(){
        Domain parentDomain = new Domain();
        parentDomain.setBase("parent");
        parentDomain.setFullDomainName("parent.com");

        parentDomain = domainRepository.save(parentDomain);

        Domain subDomain = new Domain();
        subDomain.setFullDomainName("sub.parent.com");
        subDomain.setBase("sub");
        subDomain.setParentDomain(parentDomain);

        subDomain = domainRepository.save(subDomain);

        parentDomain.getSubDomains().add(subDomain);
        parentDomain = domainRepository.save(parentDomain);

        Domain checkParent = domainRepository.findById(parentDomain.getId()).get();
        Domain checkSub = domainRepository.findById(subDomain.getId()).get();
        Hibernate.initialize(checkParent.getSubDomains());
        assertEquals("sub.parent.com", new ArrayList<>(checkParent.getSubDomains()).get(0).getFullDomainName());
        assertEquals(checkParent.getId(), checkSub.getParentDomain().getId());
    }
}