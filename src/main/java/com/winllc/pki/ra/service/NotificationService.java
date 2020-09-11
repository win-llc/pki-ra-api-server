package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Notification;
import com.winllc.pki.ra.domain.PocEntry;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.NotificationRepository;
import com.winllc.pki.ra.util.EmailUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/notification")
public class NotificationService {

    private static final Logger log = LogManager.getLogger(NotificationService.class);

    @Autowired
    private EmailUtil emailUtil;
    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;

    public NotificationService(NotificationRepository notificationRepository, AccountRepository accountRepository) {
        this.notificationRepository = notificationRepository;
        this.accountRepository = accountRepository;
    }

    //todo all user level notifications (manifested with emails) must go through this

    @GetMapping("/forCurrentUser}")
    public List<Notification> getCurrentNotificationsForUser(Authentication authentication){
        return notificationRepository.findAllByForUserNamesLike(authentication.getName());
    }

    public Notification save(Notification notification){
        return notificationRepository.save(notification);
    }

    public Optional<Notification> getNotificationById(Long id){
        return notificationRepository.findById(id);
    }

    @PostMapping("/markNotificationRead")
    public void markNotificationRead(@RequestParam Long id, Authentication authentication){
        Optional<Notification> optionalNotification = notificationRepository.findById(id);
        if(optionalNotification.isPresent()){
            Notification notification = optionalNotification.get();

            if(checkNotificationBelongsToUser(notification, authentication)){
                notification.setNotificationRead(true);
                notificationRepository.save(notification);
            }else{
                log.info("User not listed in notification, don't mark read");
            }

        }else{
            log.debug("Could not find notification: "+id);
        }
    }

    public void sendNotificationToAccountPocs(Notification notification, Account account){
        Optional<Account> optionalAccount = accountRepository.findById(account.getId());
        if(optionalAccount.isPresent()){
            Account loadedAccount = optionalAccount.get();
            Set<PocEntry> pocs = loadedAccount.getPocs();

            for(PocEntry pocEntry : pocs){

            }
        }else{
            log.debug("Could not find notification: "+notification.getId());
        }
    }

    public void sendNotificationEmail(Notification notification) {

        for(String userName : notification.getUserNamesAsList()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userName);
            //message.setSubject();


            emailUtil.sendEmail(message);
        }
    }

    private boolean checkNotificationBelongsToUser(Notification notification, Authentication authentication){
        String currentUserName = authentication.getName();
        List<String> userNamesAsList = notification.getUserNamesAsList();
        return userNamesAsList.contains(currentUserName);
    }

}
