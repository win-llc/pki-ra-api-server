package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class CertificateRequestRepositoryTest {

    @Autowired
    private CertificateRequestRepository requestRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = new Account();
        account.setKeyIdentifier("testkid1");
        account.setProjectName("Test Project");
        account = accountRepository.save(account);
        
        User user = new User();
        user.getAccounts().add(account);
        user.setIdentifier(UUID.randomUUID());
        user.setUsername("test@test.com");
        user = userRepository.save(user);
        
        CertificateRequest request = new CertificateRequest();
        request.setAccount(account);
        request.setRequestedBy(user);
        request.setStatus("new");
        requestRepository.save(request);
    }

    @AfterEach
    @Transactional
    void after(){
        requestRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }
    
    @Test
    void findAllByStatusEquals() {
        List<CertificateRequest> newRequests = requestRepository.findAllByStatusEquals("new");
        assertEquals(1, newRequests.size());
    }

    @Test
    void findAllByRequestedByEquals() {
        Optional<User> optionalUser = userRepository.findOneByUsername("test@test.com");
        List<CertificateRequest> requests = requestRepository.findAllByRequestedByEquals(optionalUser.get());
        assertEquals(1, requests.size());
    }
}