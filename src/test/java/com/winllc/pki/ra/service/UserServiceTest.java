package com.winllc.pki.ra.service;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.mock.MockUtil;
import com.winllc.pki.ra.security.RAUser;
import com.winllc.pki.ra.service.external.KeycloakService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;
    @MockBean
    private KeycloakService keycloakService;

    @Test
    void getProfile() {
        UserDetails userDetails = MockUtil.generateUserDetails();

        RAUser profile = userService.getProfile(userDetails);
        assertEquals(userDetails.getUsername(), profile.getUsername());
    }

    @Test
    void searchUsersFromIdentityProvider() {
        List<String> users = new ArrayList<>();
        users.add("user1");
        users.add("user2");
        when(keycloakService.searchUsers(any())).thenReturn(users);

        List<String> user = userService.searchUsersFromIdentityProvider("user");
        assertEquals(2, user.size());
    }
}