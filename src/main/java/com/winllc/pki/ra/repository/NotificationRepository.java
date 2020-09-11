package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Notification;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
@Transactional
public interface NotificationRepository extends BaseRepository<Notification> {

    List<Notification> findAllByForUserNamesLike(String userName);
}
