package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.endpoint.acme.AcmeServerConnection;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class AcmeServerConnectionInfoRepositoryTest {

    @Autowired
    private AcmeServerConnectionInfoRepository acmeServerConnectionInfoRepository;

    @Test
    void findByName() {
        AcmeServerConnectionInfo acmeServerConnectionInfo = new AcmeServerConnectionInfo();
        acmeServerConnectionInfo.setName("connectInfo1");

        acmeServerConnectionInfo = acmeServerConnectionInfoRepository.save(acmeServerConnectionInfo);

        AcmeServerConnectionInfo found = acmeServerConnectionInfoRepository.findByName(acmeServerConnectionInfo.getName());

        assertNotNull(found);

        acmeServerConnectionInfoRepository.delete(acmeServerConnectionInfo);
    }
}