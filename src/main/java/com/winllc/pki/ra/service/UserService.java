package com.winllc.pki.ra.service;

import com.winllc.pki.ra.security.RAUser;
import com.winllc.pki.ra.service.external.beans.IdentityExternal;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakIdentityProviderConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserService {

    private final KeycloakIdentityProviderConnection keycloakService;

    public UserService(KeycloakIdentityProviderConnection keycloakService) {
        this.keycloakService = keycloakService;
    }


    @GetMapping("/profile")
    @ResponseStatus(HttpStatus.OK)
    public RAUser getProfile(@AuthenticationPrincipal UserDetails userDetails){
        RAUser raUser = new RAUser(userDetails.getUsername());
        raUser.setPermissions(userDetails.getAuthorities().stream().map(ga -> ga.toString()).collect(Collectors.toList()));
        return raUser;
    }

    @PostMapping("/search/{search}")
    @ResponseStatus(HttpStatus.OK)
    public List<String> searchUsersFromIdentityProvider(@PathVariable String search){

        List<IdentityExternal> identityExternals = keycloakService.searchByEmailLike(search);
        return identityExternals.stream()
                .map(e -> e.getEmail())
                .collect(Collectors.toList());
    }

}
