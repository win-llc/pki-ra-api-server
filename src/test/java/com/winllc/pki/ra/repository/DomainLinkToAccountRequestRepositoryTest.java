package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainLinkToAccountRequestRepositoryTest extends BaseTest {

    @Autowired
    private DomainLinkToAccountRequestRepository requestRepository;

    @Test
    void findAllByStatusEquals() {
        DomainLinkToAccountRequest request = new DomainLinkToAccountRequest();
        request.setStatusRequested();
        request.setAccountId(3L);
        request.setRequestedDomainIds(Collections.singleton(1L));
        request = requestRepository.save(request);

        List<DomainLinkToAccountRequest> all = requestRepository.findAllByStatusEquals(request.getStatus());
        assertEquals(1, all.size());

        requestRepository.deleteAll();
    }
}