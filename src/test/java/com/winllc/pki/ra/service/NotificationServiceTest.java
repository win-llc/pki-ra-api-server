package com.winllc.pki.ra.service;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Notification;
import com.winllc.pki.ra.mock.MockUtil;
import com.winllc.pki.ra.repository.NotificationRepository;
import com.winllc.pki.ra.util.EmailUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private NotificationRepository notificationRepository;
    @MockBean
    private EmailUtil emailUtil;

    @AfterEach
    @Transactional
    void after(){
        notificationRepository.deleteAll();
    }

    @Test
    void getCurrentNotificationsForUser() {

        Notification notification = Notification.buildNew("test@test.com");
        notificationService.save(notification);

        UsernamePasswordAuthenticationToken token
                = new UsernamePasswordAuthenticationToken("test@test.com", "");
        List<Notification> currentNotificationsForUser
                = notificationService.getCurrentNotificationsForUser(token);

        assertEquals(1, currentNotificationsForUser.size());
    }

    @Test
    void save() {
        Notification notification =
                notificationService.save(Notification.buildNew("test@test.com"));
        assertNotNull(notification);
    }

    @Test
    void getNotificationById() {
        Notification notification =
                notificationService.save(Notification.buildNew("test@test.com"));

        Optional<Notification> optionalNotification
                = notificationService.getNotificationById(notification.getId());

        assertTrue(optionalNotification.isPresent());
    }

    @Test
    void markNotificationRead() {
        UsernamePasswordAuthenticationToken token
                = new UsernamePasswordAuthenticationToken("test@test.com", "");

        Notification notification = notificationService.save(Notification.buildNew("test@test.com"));
        assertFalse(notification.getNotificationRead());
        notificationService.markNotificationRead(notification.getId(), token);

        Optional<Notification> optionalNotification
                = notificationService.getNotificationById(notification.getId());

        assertTrue(optionalNotification.get().getNotificationRead());
    }

    @Test
    void sendNotificationEmail() {
        Notification notification = notificationService.save(Notification.buildNew("test@test.com"));
        notificationService.sendNotificationEmail(notification);
    }
}