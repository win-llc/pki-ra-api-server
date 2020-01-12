package com.winllc.pki.ra.security;

import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class RAUserDetailsService implements UserDetailsService {

    private static final Logger log = LogManager.getLogger(RAUserDetailsService.class);

    private UserRepository userRepository;

    public RAUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<User> userOptional = userRepository.findOneByUsername(username);
        User user;
        if(userOptional.isPresent()){
            user = userOptional.get();
        }else{
            log.debug(username+" did not exist, save to DB");
            User temp = new User();
            temp.setUsername(username);
            temp.setIdentifier(UUID.randomUUID());

            user = userRepository.save(temp);
        }

        return new RAUser(user);
    }
}