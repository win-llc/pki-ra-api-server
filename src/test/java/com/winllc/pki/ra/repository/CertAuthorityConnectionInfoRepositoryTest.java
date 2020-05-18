package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.ca.CertAuthorityConnectionType;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class CertAuthorityConnectionInfoRepositoryTest {

    @Autowired
    private CertAuthorityConnectionInfoRepository repository;

    @BeforeEach
    @Transactional
    void before(){
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        info.setName("test");
        info.setType(CertAuthorityConnectionType.INTERNAL);

        repository.save(info);
    }

    @AfterEach
    @Transactional
    void after(){
        repository.deleteAll();
    }

    @Test
    void findAllNames() {
        List<String> names = repository.findAllNames();
        assertTrue(names.contains("test"));
    }

    @Test
    void findByName() {
        Optional<CertAuthorityConnectionInfo> optionalInfo = repository.findByName("test");
        assertTrue(optionalInfo.isPresent());
    }
}