package com.winllc.pki.ra.service;

import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.AuthCredential;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.AuthCredentialRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthCredentialServiceTest extends BaseTest {

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
        //authCredentialRepository.deleteAll();
        accountRepository.deleteAll();
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