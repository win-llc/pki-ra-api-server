package com.winllc.pki.ra.security;

import com.winllc.pki.ra.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class RAUserDetailsService implements UserDetailsService {

    private static final Logger log = LogManager.getLogger(RAUserDetailsService.class);

    private UserRepository userRepository;

    public RAUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        //todo remove all this
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(username,
                username,
                new ArrayList<>());

        //todo for testing, remove
        //raUser.setPermissions(Collections.singletonList("update_account_restriction"));

        return userDetails;
    }
}