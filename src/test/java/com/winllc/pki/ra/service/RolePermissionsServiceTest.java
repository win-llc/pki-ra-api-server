package com.winllc.pki.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.pki.ra.beans.form.RolePermissionsForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.config.PermissionProperties;
import com.winllc.pki.ra.domain.RolePermission;
import com.winllc.pki.ra.repository.RolePermissionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RolePermissionsServiceTest {

    @Autowired
    private MockMvc mockMvc;
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
        assertEquals(2, availableRoles.size());
    }

    @Test
    void getAllForRole() {
        List<RolePermission> admin = rolePermissionsService.getAllForRole("ADMIN");
        assertEquals(2, admin.size());
    }

    @Test
    void updateRolePermissions() throws Exception {
        RolePermissionsForm form = new RolePermissionsForm();
        form.setRoleName("ADMIN");
        form.setPermissions(Collections.singletonList("new_permission"));
        rolePermissionsService.updateRolePermissions(form);

        List<RolePermission> admin = rolePermissionsService.getAllForRole("ADMIN");
        assertEquals(1, admin.size());

        /*
        form.setRoleName("invalid");
        String badJson = new ObjectMapper().writeValueAsString(form);
        mockMvc.perform(
                post("/api/roles/permissions/update")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(409));

         */
    }

    @Test
    void updateAllRolePermissions() throws Exception {
        List<RolePermission> rolePermissions = new ArrayList<>();
        RolePermission rp1 = new RolePermission();
        rp1.setRoleName("ADMIN");
        rp1.setPermission("add");
        rolePermissions.add(rp1);

        RolePermission rp2 = new RolePermission("USER", "view");
        rolePermissions.add(rp2);

        List<RolePermission> updated = rolePermissionsService.updateAllRolePermissions(rolePermissions);
        assertEquals(2, updated.size());

        /*
        rp2.setPermission("invalid");
        String badJson = new ObjectMapper().writeValueAsString(rolePermissions);
        mockMvc.perform(
                post("/api/roles/permissions/updateAll")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(409));

         */
    }
}