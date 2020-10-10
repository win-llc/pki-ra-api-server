package com.winllc.pki.ra.service;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.mock.MockUtil;
import com.winllc.pki.ra.security.RAUser;
import com.winllc.pki.ra.service.external.beans.IdentityExternal;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakIdentityProviderConnection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UserServiceTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserService userService;
    @MockBean
    private KeycloakIdentityProviderConnection keycloakService;

    @Test
    void getProfile() {
        UserDetails userDetails = MockUtil.generateUserDetails();

        RAUser profile = userService.getProfile(userDetails);
        assertEquals(userDetails.getUsername(), profile.getUsername());
    }

    @Test
    void searchUsersFromIdentityProvider() {
        IdentityExternal identityExternal = new IdentityExternal();
        identityExternal.setEmail("test@test.com");

        when(keycloakService.searchByEmailLike(any())).thenReturn(Collections.singletonList(identityExternal));

        List<String> user = userService.searchUsersFromIdentityProvider("user");
        assertEquals(1, user.size());
    }
}