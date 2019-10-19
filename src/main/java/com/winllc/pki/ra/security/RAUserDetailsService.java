package com.winllc.pki.ra.security;

import com.winllc.pki.ra.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RAUserDetailsService implements UserDetailsService {

    private UserRepository userRepository;

    public RAUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findOneByUsername(username)
                .map(RAUser::new)
                .orElseThrow(
                        () -> new UsernameNotFoundException(String.format("No user found for %s", username)));
    }
}