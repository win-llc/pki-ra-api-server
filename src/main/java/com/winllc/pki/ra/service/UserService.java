package com.winllc.pki.ra.service;

import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import com.winllc.pki.ra.service.external.KeycloakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserService {

    @Autowired
    private KeycloakService keycloakService;

    @GetMapping("/profile")
    @ResponseStatus(HttpStatus.OK)
    public RAUser getProfile(@AuthenticationPrincipal UserDetails userDetails){
        RAUser raUser = new RAUser();
        raUser.setUsername(userDetails.getUsername());
        raUser.setPermissions(userDetails.getAuthorities().stream().map(ga -> ga.toString()).collect(Collectors.toList()));
        return raUser;
    }

    @PostMapping("/search/{search}")
    @ResponseStatus(HttpStatus.OK)
    public List<String> searchUsersFromIdentityProvider(@PathVariable String search){

        List<String> users = keycloakService.searchUsers(search);
        return users;
    }
}
