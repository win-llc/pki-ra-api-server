package com.winllc.pki.ra.cron;

import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.pki.ra.service.AccountRestrictionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;

@Component
public class AccountPolicyServerSync {

    private final AccountRepository accountRepository;
    private final AccountRestrictionService accountRestrictionService;

    public AccountPolicyServerSync(AccountRepository accountRepository, AccountRestrictionService accountRestrictionService) {
        this.accountRepository = accountRepository;
        this.accountRestrictionService = accountRestrictionService;
    }

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    @Transactional
    public void sync(){
        List<Account> all = accountRepository.findAll();

        for(Account account : all){
            accountRestrictionService.syncPolicyServerBackedAccountRestrictions(account);
        }
    }
}
