package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.TermsOfService;
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
class TermsOfServiceRepositoryTest {

    @Autowired
    private TermsOfServiceRepository termsOfServiceRepository;

    @BeforeEach
    @Transactional
    void before(){
        TermsOfService termsOfService = new TermsOfService();
        termsOfService.setForDirectoryName("directory1");
        termsOfService.setVersionId("v1");
        termsOfService.setText("test text");
        termsOfServiceRepository.save(termsOfService);
    }

    @AfterEach
    @Transactional
    void after(){
        termsOfServiceRepository.deleteAll();
    }

    @Test
    void findAllByForDirectoryName() {
        List<TermsOfService> directory1 = termsOfServiceRepository.findAllByForDirectoryName("directory1");
        assertEquals(1, directory1.size());
    }

    @Test
    void findByVersionId() {
        Optional<TermsOfService> v1 = termsOfServiceRepository.findByVersionId("v1");
        assertTrue(v1.isPresent());
    }
}