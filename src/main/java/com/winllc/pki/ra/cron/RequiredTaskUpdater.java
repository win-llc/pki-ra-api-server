package com.winllc.pki.ra.cron;

import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.AccountRestriction;
import com.winllc.acme.common.domain.Notification;
import com.winllc.acme.common.domain.PocEntry;
import com.winllc.acme.common.repository.AccountRestrictionRepository;
import com.winllc.acme.common.repository.NotificationRepository;
import com.winllc.acme.common.repository.PocEntryRepository;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RequiredTaskUpdater {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private AccountRestrictionRepository accountRestrictionRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;

    @Scheduled(fixedDelay = 1000 * 60 * 60)
    public void findNewTasks(){
        //todo
        //findAccountRestrictionTasks();

    }

    private void findAccountRestrictionTasks(){
        ZonedDateTime nowMinusWeeks = ZonedDateTime.now().minusWeeks(2);
        List<AccountRestriction> allByDueByBefore = accountRestrictionRepository.findAllByDueByBefore(nowMinusWeeks);

        for(AccountRestriction accountRestriction : allByDueByBefore){
            List<Notification> existing = notificationRepository.findAllByTaskObjectIdAndTaskObjectClass(
                    accountRestriction.getId(), AccountRestriction.class.getCanonicalName());

            if(CollectionUtils.isEmpty(existing)){
                Hibernate.initialize(accountRestriction.getAccount());
                List<String> pocs = getAccountPocs(accountRestriction.getAccount());

                for(String poc : pocs){
                    Notification notification = Notification.buildNew(poc);
                    notification.markAsTask(accountRestriction, accountRestriction.getDueBy());
                    notification.setMessage("Account action required on "+
                            accountRestriction.getAccount().getProjectName());

                    notificationRepository.save(notification);
                }
            }


        }
    }

    private void findExpiringCertificateTasks(){

    }

    private List<String> getAccountPocs(Account account){
        List<PocEntry> pocs = pocEntryRepository.findAllByAccount(account);
        return pocs.stream()
                .map(p -> p.getEmail())
                .collect(Collectors.toList());
    }

}
