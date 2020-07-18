package com.winllc.pki.ra.service;

import com.winllc.pki.ra.config.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class SecurityPolicyServiceTest {

    @Autowired
    private SecurityPolicyService securityPolicyService;

    @BeforeEach
    @Transactional
    void before(){

    }

    @AfterEach
    @Transactional
    void after(){

    }

    @Test
    void getAllProjectDetails() {
        //todo
    }

    @Test
    void getProjectDetails() {

        //todo
    }

    @Test
    void getSecurityPolicyMapForService() {
        //todo
    }
}