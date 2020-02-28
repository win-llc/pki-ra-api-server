package com.winllc.pki.ra.service;

import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private KeycloakService keycloakService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal RAUser raUser){
        //todo
        return ResponseEntity.ok().build();
    }

    @PostMapping("/search/{search}")
    public ResponseEntity<?> searchUsersFromIdentityProvider(@PathVariable String search){

        List<String> users = keycloakService.searchUsers(search);
        return ResponseEntity.ok(users);
    }
}
