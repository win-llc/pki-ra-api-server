package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AuthCredential;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AuthCredentialRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class AuthCredentialServiceTest {

    @Autowired
    private AuthCredentialService authCredentialService;
    @Autowired
    private AuthCredentialRepository authCredentialRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void beforeEach() throws Exception {
        AccountRequestForm form = new AccountRequestForm();
        form.setProjectName("Test Project 7");
        accountService.createNewAccount(form);
    }

    @AfterEach
    void afterEach(){
        accountRepository.deleteAll();
        authCredentialRepository.deleteAll();
    }

    @Test
    void getLatestAuthCredentialForAccount() {
        Account account = accountRepository.findDistinctByProjectName("Test Project 7").get();

        Optional<AuthCredential> latestAuthCredentialForAccount = authCredentialService.getLatestAuthCredentialForAccount(account);
        assertTrue(latestAuthCredentialForAccount.isPresent());
    }

    @Test
    @Transactional
    void getAssociatedAccount() throws RAObjectNotFoundException {
        Account account = accountRepository.findDistinctByProjectName("Test Project 7").get();

        AuthCredential authCredential = new ArrayList<>(account.getAuthCredentials()).get(0);

        Optional<Account> accountOptional
                = authCredentialService.getAssociatedAccount(authCredential.getKeyIdentifier());

        assertTrue(accountOptional.isPresent());
    }
}