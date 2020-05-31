package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.RolePermissionsForm;
import com.winllc.pki.ra.config.PermissionProperties;
import com.winllc.pki.ra.domain.RolePermission;
import com.winllc.pki.ra.repository.RolePermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.management.relation.Role;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RolePermissionsService {
    //todo add permissions to roles

    @Autowired
    private PermissionProperties permissionProperties;
    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @GetMapping("/available")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getAvailableRoles(){
        return rolePermissionRepository.getAllRoleNames();
    }

    @GetMapping("/permissions/available")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getAvailablePermissions(){
        return permissionProperties.getAvailable();
    }

    @GetMapping("/permissions/byRoleName/{role}")
    @ResponseStatus(HttpStatus.OK)
    public List<RolePermission> getAllForRole(@PathVariable String role){

        List<RolePermission> allByRoleName = rolePermissionRepository.findAllByRoleName(role);

        return allByRoleName;
    }

    @PostMapping("/permissions/update")
    @ResponseStatus(HttpStatus.OK)
    public RolePermissionsForm updateRolePermissions(@Valid @RequestBody RolePermissionsForm form){
        List<RolePermission> currentRolePermissions = rolePermissionRepository.findAllByRoleName(form.getRoleName());

        Map<Boolean, List<RolePermission>> existingPermissionInNewList = currentRolePermissions.stream()
                .collect(Collectors.groupingBy(r -> form.getPermissions().contains(r.getPermission())));

        //delete role permission that were removed
        List<RolePermission> removePermissions = existingPermissionInNewList.get(false);
        List<RolePermission> keepPermissions = existingPermissionInNewList.get(true);

        if(!CollectionUtils.isEmpty(removePermissions)) {
            for (RolePermission rolePermission : existingPermissionInNewList.get(false)) {
                rolePermissionRepository.delete(rolePermission);
            }
        }

        List<RolePermission> existingKeep = new ArrayList<>();
        if(!CollectionUtils.isEmpty(keepPermissions)){
            existingKeep.addAll(keepPermissions);
        }

        List<RolePermission> permissionRoleToCreate = form.getPermissions()
                .stream().filter(p -> !existingKeep.contains(p))
                .map(p -> new RolePermission(form.getRoleName(), p))
                .collect(Collectors.toList());

        for(RolePermission rolePermission : permissionRoleToCreate){
            RolePermission temp = rolePermissionRepository.save(rolePermission);
            existingKeep.add(temp);
        }

        RolePermissionsForm newForm = new RolePermissionsForm();
        newForm.setRoleName(form.getRoleName());
        newForm.setPermissions(existingKeep.stream().map(p -> p.getPermission()).collect(Collectors.toList()));

        return newForm;
    }
}
