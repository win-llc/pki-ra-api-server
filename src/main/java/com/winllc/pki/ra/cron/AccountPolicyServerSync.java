package com.winllc.pki.ra.cron;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.service.AccountRestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;

@Component
public class AccountPolicyServerSync {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountRestrictionService accountRestrictionService;

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    @Transactional
    public void sync(){
        List<Account> all = accountRepository.findAll();

        for(Account account : all){
            accountRestrictionService.syncPolicyServerBackedAccountRestrictions(account);
        }
    }
}
