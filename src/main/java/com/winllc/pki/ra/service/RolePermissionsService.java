package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.RolePermissionsForm;
import com.winllc.pki.ra.config.KeycloakProperties;
import com.winllc.pki.ra.config.PermissionProperties;
import com.winllc.pki.ra.domain.RolePermission;
import com.winllc.pki.ra.repository.RolePermissionRepository;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakOIDCProviderConnection;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RolePermissionsService {

    private final PermissionProperties permissionProperties;
    private final RolePermissionRepository rolePermissionRepository;
    private final KeycloakOIDCProviderConnection oidcProviderConnection;

    public RolePermissionsService(PermissionProperties permissionProperties, RolePermissionRepository rolePermissionRepository, KeycloakOIDCProviderConnection oidcProviderConnection) {
        this.permissionProperties = permissionProperties;
        this.rolePermissionRepository = rolePermissionRepository;
        this.oidcProviderConnection = oidcProviderConnection;
    }

    @GetMapping("/validRoles")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getValidRoles(){
        List<RoleRepresentation> clientRoles = oidcProviderConnection.getFrontendClientRoles();
        return clientRoles.stream()
                .map(r -> r.getName())
                .collect(Collectors.toList());
    }

    @GetMapping("/usersInRole/{roleName}")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getUsersInRole(@PathVariable String roleName){
        Set<UserRepresentation> frontendUsersForRole = oidcProviderConnection.getFrontendUsersForRole(roleName);
        return frontendUsersForRole.stream()
                .map(r -> r.getEmail())
                .collect(Collectors.toList());
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<RolePermission> getAll(){
        return rolePermissionRepository.findAll();
    }

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

    @PostMapping("/permissions/updateAll")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<RolePermission> updateAllRolePermissions(@RequestBody List<RolePermission> rolePermissions){
        List<RolePermission> currentRolePermissions = rolePermissionRepository.findAll();

        //Delete entries that were removed
        List<RolePermission> toDelete = currentRolePermissions.stream()
                .filter(existing -> !rolePermissions.contains(existing))
                .collect(Collectors.toList());
        toDelete.forEach(perm -> rolePermissionRepository.delete(perm));

        if(!CollectionUtils.isEmpty(rolePermissions)){
            for(RolePermission rolePermission : rolePermissions){
                Optional<RolePermission> rolePermissionOptional = rolePermissionRepository.findDistinctByRoleNameAndPermission(
                        rolePermission.getRoleName(), rolePermission.getPermission());
                //Add to DB if does not exist
                if(!rolePermissionOptional.isPresent()){
                    rolePermissionRepository.save(rolePermission);
                }
            }
        }

        return rolePermissionRepository.findAll();
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
