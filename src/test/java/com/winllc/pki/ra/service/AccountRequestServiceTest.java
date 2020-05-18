package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.beans.form.AccountRequestUpdateForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRequest;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AccountRequestRepository;
import com.winllc.pki.ra.repository.UserRepository;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.annotation.BeforeTestExecution;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;


@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class AccountRequestServiceTest {

    @Autowired
    private AccountRequestService accountRequestService;
    @Autowired
    private AccountRequestRepository accountRequestRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    @Transactional
    void init(){
        Account account = new Account();
        account.setKeyIdentifier("kidtest1");
        account.setProjectName("Test Project 2");
        account = accountRepository.save(account);

        User user = new User();
        user.setUsername("test@test.com");
        user.setIdentifier(UUID.randomUUID());
        user = userRepository.save(user);

        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccountOwner(user);
        accountRequest.setProjectName(account.getProjectName());
        accountRequest.setState("new");
        accountRequestRepository.save(accountRequest);
    }

    @AfterEach
    @Transactional
    void after(){
        userRepository.deleteAll();
        accountRepository.deleteAll();
        accountRequestRepository.deleteAll();
    }

    @Test
    void findAll() {
        List<AccountRequest> all = accountRequestService.findAll();
        assertTrue("Response null check", all.size() == 0);
    }

    @Test
    void findPending() throws RAObjectNotFoundException {
        AccountRequestForm form = new AccountRequestForm();
        form.setAccountOwnerEmail("test@test.com");
        form.setProjectName("project1");
        accountRequestService.createAccountRequest(form);

        List<AccountRequest> all = accountRequestService.findPending();
        assertTrue("Response null check", all.size() == 1);
    }

    @Test
    void createAccountRequest() throws RAObjectNotFoundException {
        AccountRequestForm form = new AccountRequestForm();
        form.setAccountOwnerEmail("test@test.com");
        form.setProjectName("project1");
        Long id = accountRequestService.createAccountRequest(form);

        AccountRequest accountRequest = accountRequestService.findById(id);
        assertNotNull("Account Request null check", accountRequest);
    }

    @Test
    void accountRequestUpdate() throws RAObjectNotFoundException {
        AccountRequest accountRequest = accountRequestRepository.findAll().get(0);
        AccountRequestUpdateForm form = new AccountRequestUpdateForm();
        form.setAccountRequestId(accountRequest.getId());
        form.setState("approve");

        accountRequestService.accountRequestUpdate(form);

        accountRequest = accountRequestRepository.findAll().get(0);
        assertTrue("Account update check", accountRequest.getState().contentEquals("approve"));
    }

    @Test
    void findById() {
        //todo
    }

    @Test
    void delete() {
        //todo
    }
}