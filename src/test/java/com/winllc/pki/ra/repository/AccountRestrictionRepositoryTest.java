package com.winllc.acme.common.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.acme.common.constants.AccountRestrictionAction;
import com.winllc.acme.common.constants.AccountRestrictionType;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.AccountRestriction;
import com.winllc.pki.ra.service.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountRestrictionRepositoryTest extends BaseTest {

    @Autowired
    private AccountRestrictionRepository accountRestrictionRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountService accountService;

    @BeforeEach
    @Transactional
    void before(){

    }

    @AfterEach
    @Transactional
    void after(){

    }

    @Test
    void findAllByAccount() throws Exception {
        AccountRequestForm form = new AccountRequestForm();
        form.setProjectName("Test Project");
        Long id = accountService.createNewAccount(form);
        Account account = accountRepository.findById(id).get();

        AccountRestriction accountRestriction = new AccountRestriction();
        accountRestriction.setAccount(account);
        accountRestriction.setAction(AccountRestrictionAction.DISABLE_ACCOUNT);
        accountRestriction.setType(AccountRestrictionType.REQUIRE_ACCREDITATION_BY);

        accountRestriction = accountRestrictionRepository.save(accountRestriction);

        List<AccountRestriction> allByAccount = accountRestrictionRepository.findAllByAccount(account);

        assertEquals(1, allByAccount.size());

        accountRestrictionRepository.delete(accountRestriction);
        accountRepository.delete(account);
    }

    @Test
    void findAllByAccountAndCompleted() {
        Account account = Account.buildNew("Test Project");
        account = accountRepository.save(account);

        AccountRestriction accountRestriction = new AccountRestriction();
        accountRestriction.setAccount(account);
        accountRestriction.setAction(AccountRestrictionAction.DISABLE_ACCOUNT);
        accountRestriction.setType(AccountRestrictionType.REQUIRE_ACCREDITATION_BY);

        accountRestriction = accountRestrictionRepository.save(accountRestriction);

        List<AccountRestriction> completedTrue = accountRestrictionRepository.findAllByAccountAndCompleted(account, true);
        List<AccountRestriction> completedFalse = accountRestrictionRepository.findAllByAccountAndCompleted(account, false);

        assertEquals(0, completedTrue.size());
        assertEquals(1, completedFalse.size());

        accountRestrictionRepository.delete(accountRestriction);
        accountRepository.delete(account);
    }

    @Test
    void findAllByAccountAndDueByBefore() {
        Account account = Account.buildNew("Test Project");
        account = accountRepository.save(account);

        AccountRestriction accountRestriction = new AccountRestriction();
        accountRestriction.setAccount(account);
        accountRestriction.setAction(AccountRestrictionAction.DISABLE_ACCOUNT);
        accountRestriction.setType(AccountRestrictionType.REQUIRE_ACCREDITATION_BY);
        accountRestriction.setDueBy(Timestamp.valueOf(LocalDateTime.now()));

        accountRestriction = accountRestrictionRepository.save(accountRestriction);

        List<AccountRestriction> allByAccountAndDueByBefore = accountRestrictionRepository.findAllByAccountAndDueByBefore(account,
                Timestamp.valueOf(LocalDateTime.now().plusDays(1)));

        assertEquals(1, allByAccountAndDueByBefore.size());

        accountRestrictionRepository.delete(accountRestriction);
        accountRepository.delete(account);
    }

    @Test
    void findAllByAccountAndDueByBeforeAndCompletedEquals() {
        Account account = Account.buildNew("Test Project");
        account = accountRepository.save(account);

        AccountRestriction accountRestriction = new AccountRestriction();
        accountRestriction.setAccount(account);
        accountRestriction.setAction(AccountRestrictionAction.DISABLE_ACCOUNT);
        accountRestriction.setType(AccountRestrictionType.REQUIRE_ACCREDITATION_BY);
        accountRestriction.setDueBy(Timestamp.valueOf(LocalDateTime.now()));

        accountRestriction = accountRestrictionRepository.save(accountRestriction);

        List<AccountRestriction> completedFalse = accountRestrictionRepository.findAllByAccountAndDueByBeforeAndCompletedEquals(account,
                Timestamp.valueOf(LocalDateTime.now().plusDays(1)), false);
        List<AccountRestriction> completedTrue = accountRestrictionRepository.findAllByAccountAndDueByBeforeAndCompletedEquals(account,
                Timestamp.valueOf(LocalDateTime.now().plusDays(1)), true);

        assertEquals(0, completedTrue.size());
        assertEquals(1, completedFalse.size());

        accountRestrictionRepository.delete(accountRestriction);
        accountRepository.delete(account);
    }
}