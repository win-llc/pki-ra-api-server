package com.winllc.pki.ra.service;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.service.external.SecurityPolicyServerProjectDetails;
import com.winllc.pki.ra.service.external.vendorimpl.OpenDJSecurityPolicyConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityPolicyServiceTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SecurityPolicyService securityPolicyService;
    @MockBean
    private OpenDJSecurityPolicyConnection policyConnection;

    @BeforeEach
    @Transactional
    void before(){
        SecurityPolicyServerProjectDetails details = new SecurityPolicyServerProjectDetails();
        details.setProjectId("testid1");

        when(policyConnection.getAllProjects()).thenReturn(Collections.singletonList(details));
        when(policyConnection.getConnectionName()).thenReturn("opendj-security-policy-service");
        when(policyConnection.getSecurityPolicyMapForService(any(), any())).thenReturn(new HashMap<>());
        when(policyConnection.getProjectDetails(any())).thenReturn(Optional.of(details));
    }

    @AfterEach
    @Transactional
    void after(){

    }

    @Test
    void getAllProjectDetails() throws Exception {
        List<SecurityPolicyServerProjectDetails> allProjectDetails = securityPolicyService.getAllProjectDetails();
        assertEquals(1, allProjectDetails.size());
    }

    @Test
    void getProjectDetails() throws Exception {
        SecurityPolicyServerProjectDetails details = securityPolicyService.getProjectDetails("testid1");
        assertEquals("testid1", details.getProjectId());
    }

    @Test
    void getSecurityPolicyMapForService() throws Exception {
        Map<String, String> test = securityPolicyService.getSecurityPolicyMapForService(
                "opendj-security-policy-service", "test.com", "test");
        assertNotNull(test);
    }
}