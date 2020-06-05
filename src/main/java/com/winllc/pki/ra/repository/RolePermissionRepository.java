package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.RolePermission;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface RolePermissionRepository extends BaseRepository<RolePermission> {

    List<RolePermission> findAllByRoleName(String roleName);
    @Query(value = "select distinct roleName from RolePermission")
    List<String> getAllRoleNames();
    Optional<RolePermission> findDistinctByRoleNameAndPermission(String roleName, String permission);
}
