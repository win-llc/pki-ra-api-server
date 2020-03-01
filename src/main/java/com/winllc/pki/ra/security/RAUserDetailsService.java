package com.winllc.pki.ra.security;

import com.winllc.pki.ra.domain.RolePermission;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.repository.RolePermissionRepository;
import com.winllc.pki.ra.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RAUserDetailsService implements UserDetailsService {

    private static final Logger log = LogManager.getLogger(RAUserDetailsService.class);

    private UserRepository userRepository;
    @Autowired
    private RolePermissionRepository rolePermissionRepository;

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

        RAUser raUser = new RAUser(user);


        //todo for testing, remove
        //raUser.setPermissions(Collections.singletonList("update_account_restriction"));

        return raUser;
    }
}