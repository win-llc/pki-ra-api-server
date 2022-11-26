package com.winllc.pki.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.beans.form.AccountRequestUpdateForm;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.AccountRequest;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.AccountRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
import java.util.List;

import static org.springframework.test.util.AssertionErrors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class AccountRequestServiceTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRequestService accountRequestService;
    @Autowired
    private AccountRequestRepository accountRequestRepository;
    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    @Transactional
    void init(){
        Account account = Account.buildNew("Test Project 2");
        account.setKeyIdentifier("kidtest1");
        //account.setMacKey("testmac1");
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        accountRepository.deleteAll();
        accountRequestRepository.deleteAll();
    }

    @Test
    void findAll() throws Exception {
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountOwnerEmail("test@test.com");
        accountRequest.setProjectName("New Project");
        accountRequest.setState("new");
        accountRequestRepository.save(accountRequest);

        List<AccountRequestForm> all = accountRequestService.getAll(null);
        assertTrue("Response null check", all.size() == 1);

        mockMvc.perform(
                get("/api/account/request/all"))
                .andExpect(status().is(200))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("New Project")));
    }

    @Test
    @WithMockUser(authorities = {"super_admin"})
    void findPending() throws Exception {
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountOwnerEmail("test@test.com");
        accountRequest.setProjectName("New Project");
        accountRequest.setState("new");
        accountRequestRepository.save(accountRequest);

        AccountRequestForm form = new AccountRequestForm();
        form.setAccountOwnerEmail("test@test.com");
        form.setProjectName("project1");
        Authentication authentication = new TestingAuthenticationToken("test@test.com", "");
        accountRequestService.add(form, null, authentication);

        List<AccountRequest> all = accountRequestService.findPending();
        assertEquals("Response null check", 1, all.size());

        mockMvc.perform(
                get("/api/account/request/pending"))
                .andExpect(status().is(200))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("New Project")));
    }

    @Test
    @WithMockUser(authorities = {"super_admin"})
    void createAccountRequest() throws Exception {
        AccountRequestForm form = new AccountRequestForm();
        form.setAccountOwnerEmail("test@test.com");
        form.setProjectName("project1");
        Authentication authentication = new TestingAuthenticationToken("test@test.com", "");
        AccountRequestForm add = accountRequestService.add(form, null, authentication);

        AccountRequestForm accountRequest = accountRequestService.findRest(add.getId(), null);
        assertNotNull("Account Request null check", accountRequest);

        String json = new ObjectMapper().writeValueAsString(form);

        mockMvc.perform(
                post("/api/account/request/add")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().is(200));

        form.setAccountOwnerEmail("bademail");
        String badJson = new ObjectMapper().writeValueAsString(form);

        mockMvc.perform(
                post("/api/account/request/add")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(200));
    }

    @Test
    @WithMockUser(authorities = {"super_admin"})
    void accountRequestUpdate() throws Exception {
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountOwnerEmail("test@test.com");
        accountRequest.setProjectName("New Project");
        accountRequest.setState("new");
        accountRequest = accountRequestRepository.save(accountRequest);

        AccountRequestForm form = new AccountRequestForm(accountRequest);
        //form.setAccountRequestId(accountRequest.getId());
        form.setState("approve");

        Authentication authentication = new TestingAuthenticationToken("test@test.com", "");

        accountRequestService.update(form, null, authentication);

        accountRequest = accountRequestRepository.findAll().get(0);
        assertTrue("Account update check", accountRequest.getState().contentEquals("approve"));

        form.setState("badstate");
        String badJson = new ObjectMapper().writeValueAsString(form);
        mockMvc.perform(
                post("/api/account/request/update")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(200));
    }

    @Test
    @WithMockUser(authorities = {"super_admin"})
    void findById() throws Exception {
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountOwnerEmail("test@test.com");
        accountRequest.setProjectName("New Project");
        accountRequest.setState("new");
        accountRequest = accountRequestRepository.save(accountRequest);

        AccountRequestForm byId = accountRequestService.findRest(accountRequest.getId(), null);
        assertNotNull("Not null", byId);
    }

    @WithMockUser(authorities = {"super_admin"})
    @Test
    void delete() throws Exception {
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountOwnerEmail("test@test.com");
        accountRequest.setProjectName("New Project");
        accountRequest.setState("new");
        accountRequest = accountRequestRepository.save(accountRequest);

        AccountRequestForm byId = accountRequestService.findRest(accountRequest.getId(), null);
        assertNotNull("Not null", byId);

        accountRequestService.delete(accountRequest.getId(), null, null);

        try{
            accountRequestService.findRest(accountRequest.getId(), null);
            fail("Should have thrown error");
        }catch (Exception e){
        }
    }
}