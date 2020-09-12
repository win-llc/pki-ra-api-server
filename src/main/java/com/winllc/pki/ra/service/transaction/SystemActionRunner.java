package com.winllc.pki.ra.service.transaction;

import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.repository.AuditRecordRepository;
import com.winllc.pki.ra.repository.NotificationRepository;
import com.winllc.pki.ra.repository.PocEntryRepository;
import com.winllc.pki.ra.util.EmailUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class SystemActionRunner {

    private static final Logger log = LogManager.getLogger(SystemActionRunner.class);

    private final ApplicationContext context;
    private AuditRecord auditRecord;
    private Notification notification;
    private boolean sendNotification = false;
    private List<String> notificationEmails;

    private SystemActionRunner(ApplicationContext context){
        this.context = context;
        this.notificationEmails = new ArrayList<>();
    }

    public static SystemActionRunner build(ApplicationContext context){
        return new SystemActionRunner(context);
    }

    public SystemActionRunner createAuditRecord(AuditRecordType type){
        this.auditRecord = AuditRecord.buildNew(type);
        return this;
    }

    public SystemActionRunner createAuditRecord(AuditRecordType type, UniqueEntity uniqueEntity){
        this.auditRecord = AuditRecord.buildNew(type, uniqueEntity);
        return this;
    }

    public SystemActionRunner createAuditRecord(AuditRecord auditRecord){
        this.auditRecord = auditRecord;
        return this;
    }

    public SystemActionRunner createNotification(Notification notification){
        this.notification = notification;
        return this;
    }

    public SystemActionRunner createNotificationForAccountPocs(Notification notification, Account account){

        addAccountPocsForNotification(account);
        this.notification = notification;
        return this;
    }

    private void addAccountPocsForNotification(Account account){
        PocEntryRepository repository = context.getBean(PocEntryRepository.class);
        List<PocEntry> accountPocs = repository.findAllByAccount(account);
        if(!CollectionUtils.isEmpty(accountPocs)){
            List<String> emails = accountPocs.stream()
                    .map(p -> p.getEmail())
                    .collect(Collectors.toList());
            this.notificationEmails = emails;
        }
    }



    public SystemActionRunner sendNotification(){
        this.sendNotification = true;
        return this;
    }

    public <T, E extends Exception> Future<T> executeAsync(ThrowingSupplier<T, E> action){
        ThreadPoolTaskExecutor executor = context.getBean("taskExecutor", ThreadPoolTaskExecutor.class);

        return executor.submit(() -> {
           return execute(action);
        });
    }

    public <T, E extends Exception> T execute(ThrowingSupplier<T, E> action) throws Exception {
        T result = action.get();

        if(result instanceof AccountOwnedEntity){
            AccountOwnedEntity entity = (AccountOwnedEntity) result;
            Hibernate.initialize(entity.getAccount());
            Account account = entity.getAccount();
            addAccountPocsForNotification(account);
        }

        execute();
        return result;
    }

    public void execute() {
        if(this.auditRecord != null){
            AuditRecordRepository repository = context.getBean(AuditRecordRepository.class);

            log.info("Create audit record: "+this.auditRecord);

            repository.save(this.auditRecord);
        }

        if(notification != null){
            NotificationRepository repository = context.getBean(NotificationRepository.class);
            log.info("Add notification: "+this.notification);

            if(!CollectionUtils.isEmpty(notificationEmails)){
                for(String email : notificationEmails){
                    this.notification.setForUserNames(email);
                    repository.save(this.notification);
                }
            }

            if(sendNotification && !CollectionUtils.isEmpty(notificationEmails)){
                //todo send notification in different thread
                log.info("Going to send emails to: "+String.join(", ", notificationEmails));

                EmailUtil emailUtil = context.getBean(EmailUtil.class);
                for(String email : notificationEmails){
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(email);
                    //todo dynamic
                    message.setFrom("postmaster@winllc-dev.com");
                    message.setSubject("WIN RA Notification");
                    message.setText(notification.getMessage());
                    emailUtil.sendEmail(message);
                }
            }
        }
    }
}
