package com.winllc.pki.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.DomainLinkRequestDecisionForm;
import com.winllc.pki.ra.beans.form.DomainLinkToAccountRequestForm;
import com.winllc.pki.ra.beans.info.DomainLinkToAccountRequestInfo;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DomainLinkToAccountRequestServiceTest extends BaseTest {

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
        domain.setBase("winllc-dev");
        domain.setFullDomainName("winllc-dev.com");
        domain = domainRepository.save(domain);

        DomainPolicy domainPolicy = new DomainPolicy(domain);
        domainPolicy.setAccount(account);
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
    void getAllRequests() throws Exception {
        List<DomainLinkToAccountRequestForm> allRequests = linkToAccountRequestService.getAll(null);
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

    //todo @Test
    void createDomainRequest() throws Exception {
        Account account = accountRepository.findAll().get(0);
        Domain domain = domainRepository.findAll().get(0);

        DomainLinkToAccountRequestForm form = new DomainLinkToAccountRequestForm();
        form.setAccountId(account.getId());
        form.setRequestedDomainIds(Collections.singletonList(domain.getId()));

        Authentication authentication = new TestingAuthenticationToken("test@test.com", "");

        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User("test@test.com", "", new ArrayList<>());

        DomainLinkToAccountRequestForm domainRequest = linkToAccountRequestService.add(form, null, authentication);
        assertTrue(domainRequest.getId() > 0);

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

        Authentication authentication = new TestingAuthenticationToken("test@test.com", "");

        DomainLinkRequestDecisionForm decision = new DomainLinkRequestDecisionForm();
        decision.setRequestId(request.getId());
        decision.setStatus("approve");
        linkToAccountRequestService.domainRequestDecision(decision, authentication);

        Account account = accountRepository.findAll().get(0);
        Set<DomainPolicy> canIssueDomains = account.getAccountDomainPolicies();
        boolean canIssue = canIssueDomains.stream().anyMatch(d -> d.getTargetDomain().getFullDomainName().contentEquals("winllc-dev.com"));
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