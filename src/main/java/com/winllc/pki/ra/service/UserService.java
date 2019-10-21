package com.winllc.pki.ra.service;

import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal RAUser raUser){
        //todo
        return ResponseEntity.ok().build();
    }
}
