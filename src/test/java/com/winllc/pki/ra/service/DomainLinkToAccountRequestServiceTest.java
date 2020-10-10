package com.winllc.pki.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.pki.ra.beans.form.DomainLinkRequestDecisionForm;
import com.winllc.pki.ra.beans.form.DomainLinkToAccountRequestForm;
import com.winllc.pki.ra.beans.info.DomainLinkToAccountRequestInfo;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.NotAuthorizedException;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DomainLinkToAccountRequestServiceTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DomainLinkToAccountRequestService linkToAccountRequestService;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private DomainLinkToAccountRequestRepository requestRepository;
    @Autowired
    private DomainPolicyRepository domainPolicyRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = Account.buildNew("Test Project");
        account.setKeyIdentifier("testkid1");
        account = accountRepository.save(account);

        PocEntry pocEntry = PocEntry.buildNew("test@test.com", account);
        pocEntryRepository.save(pocEntry);

        Domain domain = new Domain();
        domain.setBase("winllc-dev.com");
        domain = domainRepository.save(domain);

        DomainPolicy domainPolicy = new DomainPolicy(domain);
        domainPolicy = domainPolicyRepository.save(domainPolicy);
        account.getAccountDomainPolicies().add(domainPolicy);
        //domain = domainRepository.save(domain);

        account = accountRepository.save(account);

        DomainLinkToAccountRequest request = DomainLinkToAccountRequest.buildNew();
        request.setAccountId(account.getId());
        request.setRequestedDomainIds(Collections.singleton(domain.getId()));
        requestRepository.save(request);
    }

    @AfterEach
    @Transactional
    void after(){
        domainRepository.deleteAll();
        requestRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void getAllRequests() {
        List<DomainLinkToAccountRequestInfo> allRequests = linkToAccountRequestService.getAllRequests();
        assertEquals(1, allRequests.size());
    }

    @Test
    void getUnapprovedRequests() {
        List<DomainLinkToAccountRequestInfo> allRequests = linkToAccountRequestService.getUnapprovedRequests();
        assertEquals(1, allRequests.size());
    }

    @Test
    void getById() throws RAObjectNotFoundException {
        DomainLinkToAccountRequest request = requestRepository.findAll().get(0);
        request = linkToAccountRequestService.getById(request.getId());
        assertNotNull(request);
    }

    @Test
    void createDomainRequest() throws Exception {
        Account account = accountRepository.findAll().get(0);
        Domain domain = domainRepository.findAll().get(0);

        DomainLinkToAccountRequestForm form = new DomainLinkToAccountRequestForm();
        form.setAccountId(account.getId());
        form.setRequestedDomainIds(Collections.singletonList(domain.getId()));

        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User("test@test.com", "", new ArrayList<>());

        Long domainRequest = linkToAccountRequestService.createDomainRequest(form, userDetails);
        assertTrue(domainRequest > 0);

        form.setAccountId(0L);
        form.setRequestedDomainIds(Collections.singletonList(0L));
        String badJson = new ObjectMapper().writeValueAsString(form);
        mockMvc.perform(
                post("/api/domain/request/linkAccount/create")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }

    @Test
    @Transactional
    void domainRequestDecision() throws Exception {
        DomainLinkToAccountRequest request = requestRepository.findAll().get(0);

        DomainLinkRequestDecisionForm decision = new DomainLinkRequestDecisionForm();
        decision.setRequestId(request.getId());
        decision.setStatus("approve");
        linkToAccountRequestService.domainRequestDecision(decision);

        Account account = accountRepository.findAll().get(0);
        Set<DomainPolicy> canIssueDomains = account.getAccountDomainPolicies();
        boolean canIssue = canIssueDomains.stream().anyMatch(d -> d.getTargetDomain().getBase().contentEquals("winllc-dev.com"));
        assertTrue(canIssue);

        decision.setStatus("bad");
        String badJson = new ObjectMapper().writeValueAsString(decision);
        mockMvc.perform(
                post("/api/domain/request/linkAccount/update")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }
}