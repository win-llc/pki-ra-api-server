package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.endpoint.acme.AcmeServerConnection;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

class AcmeServerConnectionInfoRepositoryTest extends BaseTest {

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