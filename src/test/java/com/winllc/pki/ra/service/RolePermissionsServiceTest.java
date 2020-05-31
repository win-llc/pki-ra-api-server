package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.RolePermissionsForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.config.PermissionProperties;
import com.winllc.pki.ra.domain.RolePermission;
import com.winllc.pki.ra.repository.RolePermissionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class RolePermissionsServiceTest {

    @Autowired
    private RolePermissionsService rolePermissionsService;
    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @BeforeEach
    @Transactional
    void before(){
        RolePermission rolePermission = new RolePermission();
        rolePermission.setPermission("can_edit_domain");
        rolePermission.setRoleName("ADMIN");
        rolePermissionRepository.save(rolePermission);

        rolePermission = new RolePermission();
        rolePermission.setPermission("can_add_domain");
        rolePermission.setRoleName("ADMIN");
        rolePermissionRepository.save(rolePermission);
    }

    @AfterEach
    @Transactional
    void after(){
        rolePermissionRepository.deleteAll();
    }

    @Test
    void getAvailableRoles() {
        List<String> availableRoles = rolePermissionsService.getAvailableRoles();
        assertEquals(1, availableRoles.size());
    }

    @Test
    void getAvailablePermissions() {
        List<String> availableRoles = rolePermissionsService.getAvailablePermissions();
        assertEquals(13, availableRoles.size());
    }

    @Test
    void getAllForRole() {
        List<RolePermission> admin = rolePermissionsService.getAllForRole("ADMIN");
        assertEquals(2, admin.size());
    }

    @Test
    void updateRolePermissions() {
        RolePermissionsForm form = new RolePermissionsForm();
        form.setRoleName("ADMIN");
        form.setPermissions(Collections.singletonList("new_permission"));
        rolePermissionsService.updateRolePermissions(form);

        List<RolePermission> admin = rolePermissionsService.getAllForRole("ADMIN");
        assertEquals(1, admin.size());
    }
}