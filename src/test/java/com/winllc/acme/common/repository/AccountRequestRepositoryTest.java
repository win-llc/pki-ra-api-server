package com.winllc.acme.common.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.acme.common.domain.AccountRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountRequestRepositoryTest  extends BaseTest {

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