package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Notification;
import com.winllc.acme.common.repository.NotificationRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        List<Notification> notifications = notificationRepository.findAllByForUserEqualsIgnoreCaseAndNotificationReadAndTaskCompleteOrderByCreationDateDesc(
                authentication.getName(), false, false);

        notifications.forEach(n -> {
            if(n.getType() != null && n.getType().getUiBasePath() != null && n.getTaskObjectId() != null) {
                n.setLink(n.getType().getUiBasePath() + "/" + n.getTaskObjectId());
            }
        });

        return notifications.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @GetMapping("/forCurrentUser/tasks")
    public List<Notification> getCurrentTasksForUser(Authentication authentication){
        return notificationRepository.findAllByForUserEqualsIgnoreCaseAndNotificationReadAndTaskCompleteOrderByCreationDateDesc(
                authentication.getName(), false, false);
    }

    @GetMapping("/forCurrentUser/count")
    public Integer getCurrentNotificationsCountForUser(Authentication authentication){
        return notificationRepository.countAllByForUserEqualsIgnoreCaseAndNotificationReadAndTaskComplete(
                authentication.getName(), false, false);
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

    @PostMapping("/markAllRead")
    public void markAllRead(Authentication authentication){
        List<Notification> allUnread = notificationRepository.
                findAllByForUserEqualsIgnoreCaseAndNotificationReadAndTaskCompleteOrderByCreationDateDesc(
                        authentication.getName(), false, false
                );

        if(CollectionUtils.isNotEmpty(allUnread)){
            for(Notification notification : allUnread){
                notification.setNotificationRead(true);
                notificationRepository.save(notification);
            }
        }
    }

    private boolean checkNotificationBelongsToUser(Notification notification, Authentication authentication){
        String currentUserName = authentication.getName();
        return notification.getForUser().equalsIgnoreCase(currentUserName);
    }

}
