package com.winllc.pki.ra.security;

import com.winllc.pki.ra.domain.RolePermission;
import com.winllc.pki.ra.repository.RolePermissionRepository;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
    private final RAUserDetailsService raUserDetailsService;

    public RAUserJwtAuthenticationConverter(
            RAUserDetailsService raUserDetailsService) {
        this.raUserDetailsService = raUserDetailsService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        String username = jwt.getClaimAsString("email");

        RAUser raUser = (RAUser) raUserDetailsService.loadUserByUsername(username);
        raUser.setRoles(authorities.stream().map(a -> a.getAuthority()).collect(Collectors.toList()));

        Set<String> permissions = new HashSet<>();
        for(String role : raUser.getRoles()){
            List<RolePermission> allByRoleName = rolePermissionRepository.findAllByRoleName(role);
            if(!CollectionUtils.isEmpty(allByRoleName)){
                permissions.addAll(allByRoleName.stream().map(r -> r.getPermission()).collect(Collectors.toList()));
            }
        }
        raUser.setPermissions(new ArrayList<>(permissions));

        return Optional.of(new UsernamePasswordAuthenticationToken(raUser, "n/a", authorities))
                .orElseThrow(() -> new BadCredentialsException("No user found"));
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