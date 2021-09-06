package com.winllc.acme.common.repository;

import com.winllc.acme.common.repository.CertAuthorityConnectionInfoRepository;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
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

class CertAuthorityConnectionInfoRepositoryTest extends BaseTest {

    @Autowired
    private CertAuthorityConnectionInfoRepository repository;

    @BeforeEach
    @Transactional
    void before(){
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        info.setName("test");
        //info.setType(CertAuthorityConnectionType.INTERNAL);

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