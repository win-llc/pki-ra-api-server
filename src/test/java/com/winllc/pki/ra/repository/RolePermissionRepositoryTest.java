package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.RolePermission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RolePermissionRepositoryTest extends BaseTest {

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @BeforeEach
    @Transactional
    void before(){
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleName("test_role");
        rolePermission.setPermission("add_account");
        rolePermissionRepository.save(rolePermission);

        rolePermission = new RolePermission();
        rolePermission.setRoleName("test_role");
        rolePermission.setPermission("delete_account");
        rolePermissionRepository.save(rolePermission);
    }

    @AfterEach
    @Transactional
    void after(){
        rolePermissionRepository.deleteAll();
    }

    @Test
    void findAllByRoleName() {
        List<RolePermission> rolePermissions = rolePermissionRepository.findAllByRoleName("test_role");
        assertEquals(2, rolePermissions.size());
    }

    @Test
    void getAllRoleNames() {
        List<String> allRoleNames = rolePermissionRepository.getAllRoleNames();
        assertEquals(1, allRoleNames.size());
    }
}