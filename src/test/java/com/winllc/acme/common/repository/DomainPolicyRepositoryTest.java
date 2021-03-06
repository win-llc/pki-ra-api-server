package com.winllc.acme.common.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.acme.common.domain.Domain;
import com.winllc.acme.common.domain.DomainPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainPolicyRepositoryTest extends BaseTest {

    @Autowired
    private DomainPolicyRepository domainPolicyRepository;
    @Autowired
    private DomainRepository domainRepository;

    @Test
    void findAllByTargetDomainEquals() {
        Domain domain = new Domain();
        domain.setBase("test.winllc-dev.com");
        domain = domainRepository.save(domain);

        DomainPolicy domainPolicy = new DomainPolicy(domain);
        domainPolicyRepository.save(domainPolicy);

        List<DomainPolicy> allByTargetDomainEquals = domainPolicyRepository.findAllByTargetDomainEquals(domain);
        assertEquals(1, allByTargetDomainEquals.size());

        domainPolicyRepository.deleteAll();
        domainRepository.deleteAll();
    }
}