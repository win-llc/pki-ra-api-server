package com.winllc.pki.ra.service;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.CertAuthorityType;
import com.winllc.pki.ra.repository.CertAuthorityTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class CertAuthorityTypeServiceTest {

    @Autowired
    private CertAuthorityTypeService certAuthorityTypeService;
    @Autowired
    private CertAuthorityTypeRepository repository;

    @Test
    void getAll() {
        List<String> requiredSettings = new ArrayList<>();
        requiredSettings.add("req1");

        CertAuthorityType type = new CertAuthorityType();
        type.setName("type1");
        type.setRequiredSettings(requiredSettings);
        repository.save(type);

        List<CertAuthorityType> types = certAuthorityTypeService.getAll();
        assertEquals(1, types.size());
    }
}