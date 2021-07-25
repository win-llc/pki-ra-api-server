package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.service.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CertificateRequestRepositoryTest extends BaseTest {

    @Autowired
    private CertificateRequestRepository requestRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountService accountService;

    @BeforeEach
    @Transactional
    void before() throws Exception {
        AccountRequestForm form = new AccountRequestForm();
        form.setProjectName("Test Project");
        Long id = accountService.createNewAccount(form);
        Account account = accountRepository.findById(id).get();
        
        CertificateRequest request = new CertificateRequest();
        request.setAccount(account);
        request.setRequestedBy("test@test.com");
        request.setStatus("new");
        requestRepository.save(request);
    }

    @AfterEach
    @Transactional
    void after(){
        requestRepository.deleteAll();
        accountRepository.deleteAll();
    }
    
    @Test
    void findAllByStatusEquals() {
        List<CertificateRequest> newRequests = requestRepository.findAllByStatusEquals("new");
        assertEquals(1, newRequests.size());
    }

    @Test
    void findAllByRequestedByEquals() {
        List<CertificateRequest> requests = requestRepository.findAllByRequestedByEquals("test@test.com");
        assertEquals(1, requests.size());
    }
}