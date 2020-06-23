package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.DomainForm;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.DomainRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class DomainServiceTest {

    @Autowired
    private DomainService domainService;
    @Autowired
    private DomainRepository domainRepository;

    @BeforeEach
    @Transactional
    void before(){
        Domain domain = new Domain();
        domain.setBase("winllc-dev.com");
        domainRepository.save(domain);
    }

    @AfterEach
    @Transactional
    void after(){
        domainRepository.deleteAll();
    }

    @Test
    void getAllAvailableDomains() {
        List<Domain> allAvailableDomains = domainService.getAllAvailableDomains();
        assertEquals(1, allAvailableDomains.size());
    }

    @Test
    void searchDomainByBaseDomain() {
        List<Domain> search = domainService.searchDomainByBaseDomain("com");
        assertEquals(1, search.size());
    }

    @Test
    void getDomainById() throws RAObjectNotFoundException {
        Domain domain = domainRepository.findAll().get(0);
        DomainInfo domainById = domainService.getDomainById(domain.getId());
        assertNotNull(domainById);
    }

    @Test
    void createDomain() {
        DomainForm form = new DomainForm("test.com");

        Long domain = domainService.createDomain(form);
        assertTrue(domain > 0);
    }

    @Test
    void updateDomain() throws RAObjectNotFoundException {
        Domain domain = domainRepository.findAll().get(0);
        DomainForm form = new DomainForm(domain);
        Domain domain1 = domainService.updateDomain(form);
        assertNotNull(domain1);
    }

    @Test
    void deleteDomain() {
        Domain domain = domainRepository.findAll().get(0);
        assertNotNull(domain);

        domainService.deleteDomain(domain.getId());

        assertEquals(0, domainRepository.findAll().size());
    }
}