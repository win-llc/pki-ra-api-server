package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRequest;
import com.winllc.pki.ra.domain.PocEntry;
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
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;

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

        PocEntry pocEntry = new PocEntry();
        pocEntry.setAccount(account);
        pocEntry.setEmail("poc@test.com");

        pocEntryRepository.save(pocEntry);

        user = userRepository.save(user);

        account.getAccountUsers().add(user);
        account.getPocs().add(pocEntry);

        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();

        pocEntryRepository.deleteByEmailEqualsAndAccount("poc@test.com", account);
        accountRepository.deleteByKeyIdentifierEquals("kidtest1");
        userRepository.deleteByUsernameEquals("test@test.com");
    }

    @Test
    void findByKeyIdentifierEquals() {
        Optional<Account> optionalAccount = accountRepository.findByKeyIdentifierEquals("kidtest1");
        assertTrue(optionalAccount.isPresent());
    }

    @Test
    void findAllByAccountUsersContains() {
        User user = userRepository.findOneByUsername("test@test.com").get();

        List<Account> accountList = accountRepository.findAllByAccountUsersContains(user);
        assertEquals(1, accountList.size());
    }

    @Test
    void findAllByAccountUsersContainsOrPocsIn() {
        Optional<User> optionalUser = userRepository.findOneByUsername("test@test.com");
        List<PocEntry> allByEmailEquals = pocEntryRepository.findAllByEmailEquals("poc@test.com");

        List<Account> accounts = accountRepository.findAllByAccountUsersContainsOrPocsIn(optionalUser.get(), allByEmailEquals);
        assertEquals(1, accounts.size());
    }
}