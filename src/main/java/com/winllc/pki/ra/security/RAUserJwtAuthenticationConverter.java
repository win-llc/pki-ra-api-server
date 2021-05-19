package com.winllc.pki.ra.security;

import com.winllc.pki.ra.domain.RolePermission;
import com.winllc.pki.ra.repository.RolePermissionRepository;
import com.winllc.pki.ra.service.RolePermissionsService;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/** JWT converter that takes the roles from 'groups' claim of JWT token. */
@SuppressWarnings("unused")
public class RAUserJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {
    private static final String GROUPS_CLAIM = "groups";
    private static final String ROLE_PREFIX = "";

    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    @Autowired
    private RolePermissionsService rolePermissionsService;
    private final RAUserDetailsService raUserDetailsService;

    public RAUserJwtAuthenticationConverter(
            RAUserDetailsService raUserDetailsService) {
        this.raUserDetailsService = raUserDetailsService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        String username = jwt.getClaimAsString("email");

        List<String> roles = authorities.stream().map(a -> a.getAuthority()).collect(Collectors.toList());

        Set<GrantedAuthority> permissions = buildEntityPermissionsFromRoles(authorities);
        permissions.addAll(buildAdditionalPermissions(authorities));

        //raUser.setPermissions(new ArrayList<>(permissions));
        UserDetails userDetails = new User(username, "", permissions);

        return Optional.of(new UsernamePasswordAuthenticationToken(userDetails, "n/a", permissions))
                .orElseThrow(() -> new BadCredentialsException("No user found"));
    }

    private Set<GrantedAuthority> buildEntityPermissionsFromRoles(Collection<GrantedAuthority> authorities){
        return rolePermissionsService.getAppRoles().stream()
                .filter(a -> authorities.contains(new SimpleGrantedAuthority(a.getName())))
                .flatMap(a -> a.getPermissions().stream())
                .flatMap(e -> e.convertToPermissionsList().stream())
                .map(p -> new SimpleGrantedAuthority(p))
                .collect(Collectors.toSet());
    }

    private Set<GrantedAuthority> buildAdditionalPermissions(Collection<GrantedAuthority> authorities){
        Set<GrantedAuthority> permissions = new HashSet<>();
        for(GrantedAuthority role : authorities){
            List<RolePermission> allByRoleName = rolePermissionRepository.findAllByRoleName(role.toString());
            if(!CollectionUtils.isEmpty(allByRoleName)){
                permissions.addAll(allByRoleName.stream()
                        .map(r -> new SimpleGrantedAuthority(r.getPermission()))
                        .collect(Collectors.toList()));
            }
        }

        //Override from OIDC provider
        if(authorities.stream().map(a -> a.getAuthority())
                .anyMatch(a -> a.equalsIgnoreCase("SUPER_ADMIN"))){
            permissions.add(new SimpleGrantedAuthority("super_admin"));
        }

        return permissions;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return this.getGroups(jwt).stream()
                .map(authority -> ROLE_PREFIX + authority.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getGroups(Jwt jwt) {
        Object groups = getGroupsFromResourceAccess(jwt);
        if (groups instanceof Collection) {
            return (Collection<String>) groups;
        }

        return Collections.emptyList();
    }

    //todo make more dynamic
    private Collection<String> getGroupsFromResourceAccess(Jwt jwt) {
        List<String> returnRoles = new ArrayList<>();
        Object resourceAccess = jwt.getClaims().get("resource_access");
        if (resourceAccess instanceof JSONObject) {
            JSONObject jsonRA = (JSONObject) resourceAccess;
            JSONObject clientAccess = (JSONObject) jsonRA.get("pki-ra-client-public");
            Collection<String> roles = (Collection<String>) clientAccess.get("roles");
            returnRoles.addAll(roles);
        }
        return returnRoles;
    }
}