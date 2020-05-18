package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = new Account();
        account.setKeyIdentifier("kidtest1");
        account.setProjectName("Test Project");
        account = accountRepository.save(account);

        User user = new User();
        user.setIdentifier(UUID.randomUUID());
        user.setUsername("test@test.com");
        user.getAccounts().add(account);

        user = userRepository.save(user);

        account.getAccountUsers().add(user);
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void findOneByUsername() {
        Optional<User> oneByUsername = userRepository.findOneByUsername("test@test.com");
        assertTrue(oneByUsername.isPresent());
    }

    @Test
    void findOneByIdentifier() {
        User oneByUsername = userRepository.findOneByUsername("test@test.com").get();
        Optional<User> oneByIdentifier = userRepository.findOneByIdentifier(oneByUsername.getIdentifier());
        assertTrue(oneByIdentifier.isPresent());
    }

    @Test
    void findAllByAccountsContains() {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();
        List<User> allByAccountsContains = userRepository.findAllByAccountsContains(account);
        assertEquals(1, allByAccountsContains.size());
    }

    @Test
    void deleteUserByIdentifier() {
        User oneByUsername = userRepository.findOneByUsername("test@test.com").get();
        assertEquals(1, userRepository.count());
        userRepository.deleteUserByIdentifier(oneByUsername.getIdentifier());
        assertEquals(0, userRepository.count());
    }

    @Test
    void deleteByUsernameEquals() {
        assertEquals(1, userRepository.count());
        userRepository.deleteByUsernameEquals("test@test.com");
        assertEquals(0, userRepository.count());
    }
}