package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.AccountRequest;
import com.winllc.pki.ra.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class AccountRequestRepositoryTest {

    @Autowired
    private AccountRequestRepository accountRequestRepository;

    @Test
    void findAllByStateEquals() {
        AccountRequest accountRequest = AccountRequest.createNew();
        accountRequest.setAccountOwnerEmail("test@test.com");
        accountRequest.setProjectName("Test Project");

        accountRequest = accountRequestRepository.save(accountRequest);

        List<AccountRequest> allByStateEquals = accountRequestRepository.findAllByStateEquals(accountRequest.getState());
        assertEquals(1, allByStateEquals.size());

        accountRequestRepository.delete(accountRequest);
    }
}