package com.winllc.pki.ra.cron;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRestriction;
import com.winllc.pki.ra.domain.Notification;
import com.winllc.pki.ra.domain.PocEntry;
import com.winllc.pki.ra.repository.AccountRestrictionRepository;
import com.winllc.pki.ra.repository.NotificationRepository;
import com.winllc.pki.ra.repository.PocEntryRepository;
import org.apache.commons.collections.CollectionUtils;
import org.aspectj.weaver.ast.Not;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        Timestamp nowMinusWeeks = Timestamp.valueOf(LocalDateTime.now().minusWeeks(2));
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
