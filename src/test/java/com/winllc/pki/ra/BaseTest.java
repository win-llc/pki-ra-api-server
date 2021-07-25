package com.winllc.pki.ra;

import com.winllc.pki.ra.util.EmailUtil;
import org.keycloak.admin.client.Keycloak;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class BaseTest {

    @MockBean
    private EmailUtil emailUtil;
    @MockBean
    private Keycloak keycloak;

}
