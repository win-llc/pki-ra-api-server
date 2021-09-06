package com.winllc.pki.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.beans.form.AccountRestrictionForm;
import com.winllc.acme.common.constants.AccountRestrictionAction;
import com.winllc.acme.common.constants.AccountRestrictionType;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.AccountRestriction;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.AccountRestrictionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountRestrictionServiceTest extends BaseTest {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRestrictionService accountRestrictionService;
    @Autowired
    private AccountRestrictionRepository accountRestrictionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountService accountService;

    @BeforeEach
    @Transactional
    void before() throws Exception {
        AccountRequestForm form = new AccountRequestForm();
        form.setProjectName("Test Project 2");
        accountService.createNewAccount(form);
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
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void getById() throws RAObjectNotFoundException {
        Account account = accountRepository.findDistinctByProjectName("Test Project 2").get();

        AccountRestriction accountRestriction = new AccountRestriction();
        accountRestriction.setAccount(account);
        accountRestriction.setAddedByUser("test@test.com");
        accountRestriction.setType(AccountRestrictionType.REQUIRE_ACCREDITATION_BY);
        accountRestriction.setAction(AccountRestrictionAction.DISABLE_ACCOUNT);
        accountRestriction.setDueBy(ZonedDateTime.now());
        accountRestriction = accountRestrictionRepository.save(accountRestriction);

        AccountRestrictionForm byId = accountRestrictionService.getById(accountRestriction.getId());
        assertNotNull(byId);
    }

    @Test
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void create() throws Exception {
        Account account = accountRepository.findDistinctByProjectName("Test Project 2").get();

        AccountRestrictionForm accountRestrictionForm = new AccountRestrictionForm();
        accountRestrictionForm.setAccountId(account.getId());
        accountRestrictionForm.setAction(AccountRestrictionAction.DISABLE_ACCOUNT.toString());
        accountRestrictionForm.setDueBy(formatter.format(LocalDateTime.now().plusDays(3)));
        Long id = accountRestrictionService.create(accountRestrictionForm);
        assertNotNull(id);

        accountRestrictionForm.setAction("badaction");
        String badJson = new ObjectMapper().writeValueAsString(accountRestrictionForm);
        mockMvc.perform(
                post("/api/accountRestriction/create")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }

    @Test
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void update() throws Exception {
        Account account = accountRepository.findDistinctByProjectName("Test Project 2").get();

        AccountRestrictionForm accountRestrictionForm = new AccountRestrictionForm();
        accountRestrictionForm.setAccountId(account.getId());
        accountRestrictionForm.setAction(AccountRestrictionAction.DISABLE_ACCOUNT.toString());
        accountRestrictionForm.setDueBy(formatter.format(LocalDateTime.now().plusDays(3)));
        Long id = accountRestrictionService.create(accountRestrictionForm);

        accountRestrictionForm.setId(id);
        accountRestrictionForm.setAction(AccountRestrictionAction.ENABLE_ACCOUNT.toString());

        AccountRestriction accountRestriction = accountRestrictionService.update(accountRestrictionForm);
        assertEquals(accountRestriction.getAction(), AccountRestrictionAction.ENABLE_ACCOUNT);

        accountRestrictionForm.setAction("bad");
        String badJson = new ObjectMapper().writeValueAsString(accountRestrictionForm);
        mockMvc.perform(
                post("/api/accountRestriction/update")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }

    @Test
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void delete() {
        Account account = accountRepository.findDistinctByProjectName("Test Project 2").get();

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
    @WithMockUser(authorities = {"super_admin"})
    void getAllForAccount() throws RAObjectNotFoundException {
        Account account = accountRepository.findDistinctByProjectName("Test Project 2").get();

        AccountRestriction accountRestriction = new AccountRestriction();
        accountRestriction.setAccount(account);
        accountRestriction.setAddedByUser("test@test.com");
        accountRestriction.setType(AccountRestrictionType.REQUIRE_ACCREDITATION_BY);
        accountRestriction.setAction(AccountRestrictionAction.DISABLE_ACCOUNT);
        accountRestriction.setDueBy(ZonedDateTime.now());
        accountRestrictionRepository.save(accountRestriction);

        List<AccountRestrictionForm> allForAccount = accountRestrictionService.getAllForAccount(account.getId());
        assertEquals(1, allForAccount.size());
    }

    @Test
    void checkIfAccountValid() {
        Account account = accountRepository.findDistinctByProjectName("Test Project 2").get();

        boolean valid = accountRestrictionService.checkIfAccountValid(account);
        assertTrue(valid);
    }
}