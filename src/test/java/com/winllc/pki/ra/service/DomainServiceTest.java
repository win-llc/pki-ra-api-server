package com.winllc.pki.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.DomainForm;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.DomainPolicyRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DomainServiceTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DomainService domainService;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private DomainPolicyRepository domainPolicyRepository;

    @BeforeEach
    @Transactional
    void before(){
        Domain domain = new Domain();
        domain.setBase("winllc-dev");
        domain.setFullDomainName("winllc-dev.com");
        domainRepository.save(domain);
    }

    @AfterEach
    @Transactional
    void after(){
        domainPolicyRepository.deleteAll();
        domainRepository.deleteAll();
    }

    @Test
    void getAllAvailableDomains() {
        List<Domain> allAvailableDomains = domainService.getAllAvailableDomains();
        assertEquals(1, allAvailableDomains.size());
    }

    @Test
    void searchDomainByBaseDomain() {
        List<Domain> search = domainService.searchDomainByBaseDomain("winllc-dev");
        assertEquals(1, search.size());
    }

    @Test
    @Transactional
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void getDomainById() throws RAObjectNotFoundException {
        Domain domain = domainRepository.findAll().get(0);
        DomainInfo domainById = domainService.getDomainById(domain.getId());
        assertNotNull(domainById);

        DomainForm form = new DomainForm("test.com");

        Long domainId = domainService.createDomain(form);
        assertTrue(domainId > 0);

        DomainForm withParentForm = new DomainForm("sub.test.com");
        withParentForm.setParentDomainId(domainId);

        Long subDomainId = domainService.createDomain(withParentForm);
        DomainInfo subDomainInfo = domainService.getDomainById(subDomainId);

        assertEquals("test.com", subDomainInfo.getParentDomainInfo().getBase());

        DomainInfo parentDomainInfo = domainService.getDomainById(domainId);
        assertEquals(1, parentDomainInfo.getSubDomainInfo().size());
    }

    @Test
    @Transactional
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void createDomain() throws Exception {
        DomainForm parentForm = new DomainForm("com");
        Long parentId = domainService.createDomain(parentForm);

        DomainForm form = new DomainForm("test");
        form.setParentDomainId(parentId);

        Long domainId = domainService.createDomain(form);
        assertTrue(domainId > 0);

        DomainForm withParentForm = new DomainForm("sub");
        withParentForm.setParentDomainId(domainId);

        Long subDomainId = domainService.createDomain(withParentForm);
        Domain checkSubDomain = domainRepository.findById(subDomainId).get();

        assertEquals("sub.test.com", checkSubDomain.getFullDomainName());
        assertEquals(domainId, checkSubDomain.getParentDomain().getId());

        form.setBase("bad base");
        String badJson = new ObjectMapper().writeValueAsString(form);
        mockMvc.perform(
                post("/api/domain/create")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }

    @Test
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void updateDomain() throws Exception {
        Domain domain = domainRepository.findAll().get(0);
        DomainForm form = new DomainForm(domain);
        Domain domain1 = domainService.updateDomain(form);
        assertNotNull(domain1);

        form.setParentDomainId(0L);
        String badJson = new ObjectMapper().writeValueAsString(form);
        mockMvc.perform(
                post("/api/domain/update")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }

    @Test
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void deleteDomain() {
        Domain domain = domainRepository.findAll().get(0);
        assertNotNull(domain);

        domainService.deleteDomain(domain.getId());

        assertEquals(0, domainRepository.findAll().size());
    }
}