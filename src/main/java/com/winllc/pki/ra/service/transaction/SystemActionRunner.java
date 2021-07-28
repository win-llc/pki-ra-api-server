package com.winllc.pki.ra.service.transaction;

import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.UniqueEntity;
import com.winllc.acme.common.constants.AuditRecordType;
import com.winllc.acme.common.domain.*;
import com.winllc.acme.common.repository.AuditRecordRepository;
import com.winllc.acme.common.repository.NotificationRepository;
import com.winllc.acme.common.repository.PocEntryRepository;
import com.winllc.pki.ra.util.EmailUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class SystemActionRunner {

    private static final Logger log = LogManager.getLogger(SystemActionRunner.class);

    private final ApplicationContext context;
    private AuditRecord auditRecord;
    private Notification notification;
    private boolean entityIsTask = false;
    private Timestamp taskDueBy;
    private boolean sendNotification = false;
    private List<String> notificationEmails;
    private Object entity;

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

    public SystemActionRunner markEntityAsTask(LocalDateTime dueBy){
        this.entityIsTask = true;
        this.taskDueBy = Timestamp.valueOf(dueBy);
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

        return executor.submit(() -> execute(action));
    }

    public <T, E extends Exception> T execute(ThrowingSupplier<T, E> action) throws Exception {
        T result = action.get();

        this.entity = result;

        if(result instanceof AccountOwnedEntity){
            AccountOwnedEntity entity = (AccountOwnedEntity) result;
            Hibernate.initialize(entity.getOwnerAccount());
            Account account = entity.getOwnerAccount();
            addAccountPocsForNotification(account);
        }

        //Check if a task notification is associated with this entity, if yes mark complete if relevant
        if(result instanceof TaskEntity && result instanceof AbstractPersistable){
            if(((TaskEntity) result).isComplete()){
                Long id = ((AbstractPersistable<Long>) result).getId();
                String clazz = result.getClass().getCanonicalName();

                NotificationRepository notiRepo = context.getBean(NotificationRepository.class);
                List<Notification> found = notiRepo.findAllByTaskObjectIdAndTaskObjectClass(id, clazz);

                if(!CollectionUtils.isEmpty(found)){
                    for(Notification notification : found){
                        notification.setTaskComplete(true);
                        notiRepo.save(notification);
                    }
                }
            }
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
                    Notification toSave = this.notification.clone();

                    if(this.entityIsTask && entity instanceof AbstractPersistable){
                        toSave.markAsTask((AbstractPersistable<Long>) entity, taskDueBy);
                    }

                    if(this.auditRecord != null){
                        toSave.setType(this.auditRecord.getType());
                    }

                    toSave.setForUser(email);
                    repository.save(toSave);
                }
            }

            if(sendNotification && !CollectionUtils.isEmpty(notificationEmails)){
                //todo send notification in different thread
                log.info("Going to send emails to: "+String.join(", ", notificationEmails));

                EmailUtil emailUtil = context.getBean(EmailUtil.class);
                for(String email : notificationEmails){
                    try {
                        SimpleMailMessage message = new SimpleMailMessage();
                        message.setTo(email);
                        //todo dynamic
                        message.setFrom("postmaster@winllc-dev.com");
                        message.setSubject("WIN RA Notification");
                        message.setText(notification.getMessage());
                        emailUtil.sendEmail(message);
                    }catch (Exception e){
                        log.error("Could not send email to: "+email, e);
                    }
                }
            }
        }
    }
}
