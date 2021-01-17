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

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    //todo all user level notifications (manifested with emails) must go through this

    @GetMapping("/forCurrentUser")
    public List<Notification> getCurrentNotificationsForUser(Authentication authentication){
        return notificationRepository.findAllByForUserNamesLikeAndNotificationRead(
                authentication.getName(), false);
    }

    @GetMapping("/forCurrentUser/count")
    public Integer getCurrentNotificationsCountForUser(Authentication authentication){
        return notificationRepository.countAllByForUserNamesLikeAndNotificationRead(
                authentication.getName(), false);
    }

    @GetMapping("/byId/{id}")
    public Notification getById(@PathVariable Long id, Authentication authentication){
        return notificationRepository.findById(id).get();
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


    private boolean checkNotificationBelongsToUser(Notification notification, Authentication authentication){
        String currentUserName = authentication.getName();
        List<String> userNamesAsList = notification.getUserNamesAsList();
        return userNamesAsList.contains(currentUserName);
    }

}
