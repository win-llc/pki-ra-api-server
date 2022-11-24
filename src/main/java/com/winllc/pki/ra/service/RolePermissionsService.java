package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.AppRolePermissionsForm;
import com.winllc.pki.ra.beans.form.RolePermissionsForm;
import com.winllc.pki.ra.beans.info.AppRoleInfo;
import com.winllc.pki.ra.config.KeycloakProperties;
import com.winllc.pki.ra.config.PermissionProperties;
import com.winllc.acme.common.domain.AppRole;
import com.winllc.acme.common.domain.EntityPermission;
import com.winllc.acme.common.domain.RolePermission;
import com.winllc.acme.common.repository.AppRoleRepository;
import com.winllc.acme.common.repository.EntityPermissionRepository;
import com.winllc.acme.common.repository.RolePermissionRepository;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakOIDCProviderConnection;
import org.hibernate.Hibernate;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.parameters.P;
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
    private final AppRoleRepository appRoleRepository;
    private final EntityPermissionRepository entityPermissionRepository;

    public RolePermissionsService(PermissionProperties permissionProperties,
                                  RolePermissionRepository rolePermissionRepository,
                                  KeycloakOIDCProviderConnection oidcProviderConnection,
                                  AppRoleRepository appRoleRepository,
                                  EntityPermissionRepository entityPermissionRepository) {
        this.permissionProperties = permissionProperties;
        this.rolePermissionRepository = rolePermissionRepository;
        this.oidcProviderConnection = oidcProviderConnection;
        this.appRoleRepository = appRoleRepository;
        this.entityPermissionRepository = entityPermissionRepository;
    }



    @Transactional
    public List<AppRole> getAppRoles(){
        List<AppRole> allRoles = new ArrayList<>();
        List<RoleRepresentation> frontendClientRoles = oidcProviderConnection.getFrontendClientRoles();
        for(RoleRepresentation roleRepresentation : frontendClientRoles){
            String roleName = roleRepresentation.getName();

            Optional<AppRole> optionalRole = appRoleRepository.findFirstByNameEquals(roleName);
            AppRole appRole;
            if(optionalRole.isPresent()){
                appRole = optionalRole.get();
            }else{
                appRole = registerAppRole(roleName);
            }

            appRole = populatePermissionsForRole(appRole);
            allRoles.add(appRole);
        }
        return allRoles;
    }

    private AppRole registerAppRole(String roleName){
        AppRole appRole = new AppRole();
        appRole.setName(roleName);
        return appRoleRepository.save(appRole);
    }

    private AppRole populatePermissionsForRole(AppRole appRole){
        Hibernate.initialize(appRole.getPermissions());
        boolean change = false;
        for(String protectedEntity : permissionProperties.getProtectedEntities()){
            Optional<EntityPermission> optionalPermission = entityPermissionRepository.findFirstByEntityNameAndRole(protectedEntity, appRole);

            EntityPermission permission;
            if(optionalPermission.isEmpty()){
                change = true;
                permission = registerPermission(appRole, protectedEntity);
                appRole.getPermissions().add(permission);
            }
        }

        if(change){
            appRole = appRoleRepository.save(appRole);
        }

        return appRole;
    }

    private EntityPermission registerPermission(AppRole appRole, String entityName){
        EntityPermission entityPermission = new EntityPermission();
        entityPermission.setRole(appRole);
        entityPermission.setEntityName(entityName);

        return entityPermissionRepository.save(entityPermission);
    }

    @Transactional
    @GetMapping("/retrieve")
    public AppRolePermissionsForm getAppRolesForm(){
        AppRolePermissionsForm form = new AppRolePermissionsForm();

        List<AppRoleInfo> entityRoles = getAppRoles().stream()
                .map(a -> new AppRoleInfo(a))
                .collect(Collectors.toList());

       form.setRoles(entityRoles);
        return form;
    }

    @Transactional
    @PostMapping("/update")
    public AppRolePermissionsForm updateRolePermissions(@RequestBody AppRolePermissionsForm form){
        for(AppRoleInfo info : form.getRoles()){
            Optional<AppRole> roleOptional = appRoleRepository.findById(info.getId());
            if(roleOptional.isPresent()){
                AppRole appRole = roleOptional.get();
                for(EntityPermission permission : info.getPermissions()){
                    Optional<EntityPermission> permissionOptional = entityPermissionRepository.findById(permission.getId());
                    if(permissionOptional.isPresent()){
                        EntityPermission entityPermission = permissionOptional.get();
                        entityPermission.setAllowCreate(permission.isAllowCreate());
                        entityPermission.setAllowUpdate(permission.isAllowUpdate());
                        entityPermission.setAllowRead(permission.isAllowRead());
                        entityPermission.setAllowViewAll(permission.isAllowViewAll());
                        entityPermission.setAllowDelete(permission.isAllowDelete());
                        entityPermissionRepository.save(entityPermission);
                    }
                }

                List<RolePermission> existingAdditional = rolePermissionRepository.findAllByRole(appRole);

                //Add new
                for(RolePermission rolePermission : info.getAdditionalPermissions()){
                    if(existingAdditional.stream().noneMatch(e -> e.getPermission().equalsIgnoreCase(rolePermission.getPermission()))){
                        rolePermission.setRole(appRole);
                        rolePermission.setRoleName(appRole.getName());
                        rolePermissionRepository.save(rolePermission);
                    }
                }

                //Delete old
                for(RolePermission rolePermission : existingAdditional){
                    if(info.getAdditionalPermissions().stream().noneMatch(e -> e.getPermission().equalsIgnoreCase(rolePermission.getPermission()))){
                        rolePermissionRepository.delete(rolePermission);
                    }
                }
            }
        }
        
        
        return getAppRolesForm();
    }

    @GetMapping("/permissions/options")
    public Map<String, String> optionsPermissions(){
        return permissionProperties.getAvailable().stream()
                .collect(Collectors.toMap(d -> d, d -> d));
    }

    @GetMapping("/roles/options")
    public Map<String, String> optionsRoles(){
        List<RoleRepresentation> frontendClientRoles = oidcProviderConnection.getFrontendClientRoles();
        return frontendClientRoles.stream()
                .collect(Collectors.toMap(d -> d.getName(), d -> d.getName()));
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
