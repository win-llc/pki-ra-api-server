package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AccountRestrictionForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.constants.AccountRestrictionAction;
import com.winllc.pki.ra.constants.AccountRestrictionType;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRestriction;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.AccountRestrictionRepository;
import com.winllc.pki.ra.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class AccountRestrictionServiceTest {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    private AccountRestrictionService accountRestrictionService;
    @Autowired
    private AccountRestrictionRepository accountRestrictionRepository;
    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = new Account();
        account.setKeyIdentifier("kidtest1");
        account.setProjectName("Test Project 2");
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        accountRestrictionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void getRestrictionTypes() {
        List<AccountRestrictionType> restrictionTypes = accountRestrictionService.getRestrictionTypes();
        assertTrue(restrictionTypes.size() > 0);
    }

    @Test
    void getRestrictionActions() {
        List<AccountRestrictionAction> restrictionActions = accountRestrictionService.getRestrictionActions();
        assertTrue(restrictionActions.size() > 0);
    }

    @Test
    void getById() throws RAObjectNotFoundException {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();

        AccountRestriction accountRestriction = new AccountRestriction();
        accountRestriction.setAccount(account);
        accountRestriction.setAddedByUser("test@test.com");
        accountRestriction.setType(AccountRestrictionType.REQUIRE_ACCREDITATION_BY);
        accountRestriction.setAction(AccountRestrictionAction.DISABLE_ACCOUNT);
        accountRestriction.setDueBy(Timestamp.from(LocalDateTime.now().plusDays(3).toInstant(ZoneOffset.UTC)));
        accountRestriction = accountRestrictionRepository.save(accountRestriction);

        AccountRestrictionForm byId = accountRestrictionService.getById(accountRestriction.getId());
        assertNotNull(byId);
    }

    @Test
    void create() throws Exception {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();

        AccountRestrictionForm accountRestrictionForm = new AccountRestrictionForm();
        accountRestrictionForm.setAccountId(account.getId());
        accountRestrictionForm.setAction(AccountRestrictionAction.DISABLE_ACCOUNT.toString());
        accountRestrictionForm.setDueBy(formatter.format(LocalDateTime.now().plusDays(3)));
        Long id = accountRestrictionService.create(accountRestrictionForm);
        assertNotNull(id);
    }

    @Test
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void update() throws Exception {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();

        AccountRestrictionForm accountRestrictionForm = new AccountRestrictionForm();
        accountRestrictionForm.setAccountId(account.getId());
        accountRestrictionForm.setAction(AccountRestrictionAction.DISABLE_ACCOUNT.toString());
        accountRestrictionForm.setDueBy(formatter.format(LocalDateTime.now().plusDays(3)));
        Long id = accountRestrictionService.create(accountRestrictionForm);

        accountRestrictionForm.setId(id);
        accountRestrictionForm.setAction(AccountRestrictionAction.ENABLE_ACCOUNT.toString());

        AccountRestriction accountRestriction = accountRestrictionService.update(accountRestrictionForm);
        assertEquals(accountRestriction.getAction(), AccountRestrictionAction.ENABLE_ACCOUNT);
    }

    @Test
    void delete() {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();

        AccountRestriction accountRestriction = new AccountRestriction();
        accountRestriction.setAccount(account);
        accountRestriction.setAddedByUser("test@test.com");
        accountRestriction.setType(AccountRestrictionType.REQUIRE_ACCREDITATION_BY);
        accountRestriction.setAction(AccountRestrictionAction.DISABLE_ACCOUNT);
        accountRestriction = accountRestrictionRepository.save(accountRestriction);

        assertNotNull(accountRestriction);

        accountRestrictionService.delete(accountRestriction.getId());

        Optional<AccountRestriction> byId = accountRestrictionRepository.findById(accountRestriction.getId());
        assertFalse(byId.isPresent());
    }

    @Test
    void getAllForAccount() throws RAObjectNotFoundException {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();

        AccountRestriction accountRestriction = new AccountRestriction();
        accountRestriction.setAccount(account);
        accountRestriction.setAddedByUser("test@test.com");
        accountRestriction.setType(AccountRestrictionType.REQUIRE_ACCREDITATION_BY);
        accountRestriction.setAction(AccountRestrictionAction.DISABLE_ACCOUNT);
        accountRestriction.setDueBy(Timestamp.from(LocalDateTime.now().plusDays(3).toInstant(ZoneOffset.UTC)));
        accountRestrictionRepository.save(accountRestriction);

        List<AccountRestrictionForm> allForAccount = accountRestrictionService.getAllForAccount(account.getId());
        assertEquals(1, allForAccount.size());
    }

    @Test
    void checkIfAccountValid() {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();

        boolean valid = accountRestrictionService.checkIfAccountValid(account);
        assertTrue(valid);
    }
}