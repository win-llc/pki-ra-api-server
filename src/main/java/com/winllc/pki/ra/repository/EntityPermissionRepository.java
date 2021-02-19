package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.AppRole;
import com.winllc.pki.ra.domain.EntityPermission;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
@Transactional
public interface EntityPermissionRepository extends BaseRepository<EntityPermission> {
    Optional<EntityPermission> findFirstByEntityNameAndRole(String entityName, AppRole role);
}
