package com.winllc.pki.ra.constants;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ServerSettingRequired implements Comparable<ServerSettingRequired> {
    EMAIL_SERVER_HOST("emailServer", "Email Server Host", "Email", false,1),
    EMAIL_SERVER_PORT("emailServerPort", "Email Server Port", "Email", false, 2),
    EMAIL_SERVER_FROM_ADDRESS("emailFromAddress", "Default From Address", "Email", false, 3),

    ENTITY_DIRECTORY_LDAP_URL("entityDirectoryLdapUrl", "Entity Directory LDAP URL", "Entity Directory", false, 1),
    ENTITY_DIRECTORY_LDAP_USERNAME("entityDirectoryLdapUsername", "Username", "Entity Directory", false, 2),
    ENTITY_DIRECTORY_LDAP_PASSWORD("entityDirectoryLdapPassword", "Password", "Entity Directory", true, 3),
    ENTITY_DIRECTORY_LDAP_BASEDN("entityDirectoryLdapBaseDn", "Base DN", "Entity Directory", false, 4),
    ENTITY_DIRECTORY_LDAP_SERVERBASEDN("entityDirectoryLdapServerBaseDn", "Server Base DN", "Entity Directory", false, 5),
    //OIDC_SERVER_BASE_URL("openIdServerBaseUrl", "OIDC", false),
    //OIDC_ENABLED("openIdConnectEnabled", "OIDC", false),

    POLICY_SERVER_LDAP_URL("policyServerLdapUrl", "LDAP URL", "Policy Server", false, 1),
    POLICY_SERVER_LDAP_USERNAME("policyServerLdapUsername", "Username", "Policy Server", false, 2),
    POLICY_SERVER_LDAP_PASSWORD("policyServerLdapPassword", "Password", "Policy Server", true, 3),
    POLICY_SERVER_LDAP_BASEDN("policyServerLdapBaseDn", "Base DN", "Policy Server", false, 4),
    POLICY_SERVER_LDAP_PROJECTSBASEDN("policyServerLdapProjectsBaseDn", "Projects Base DN", "Policy Server", false, 5),
    POLICY_SERVER_LDAP_SCHEMATYPE("policyServerLdapSchemaType", "Project LDAP Schema Type", "Policy Server", false, 6),
    POLICY_SERVER_LDAP_DOMAINSATTRIBUTE("policyServerLdapDomainsAttribute", "Domains Attribute", "Policy Server", false, 7),
    POLICY_SERVER_LDAP_POCSATTRIBUTE("policyServerLdapPocsAttribute", "POCs Attribute", "Policy Server", false, 8),
    POLICY_SERVER_LDAP_VALIDFROMATTRIBUTE("policyServerValidFromAttribute", "Valid From Attribute", "Policy Server", false, 9),
    POLICY_SERVER_LDAP_VALIDTOATTRIBUTE("policyServerValidToAttribute", "Valid To Attribute", "Policy Server", false, 10),
    POLICY_SERVER_LDAP_ENABLEDATTRIBUTE("policyServerEnabledAttribute", "Enabled Attribute", "Policy Server", false, 11),
    ;

    private final String settingName;
    private final String friendlyName;
    private final String settingGroupName;
    private final boolean passwordField;
    private final Integer weight;

    ServerSettingRequired(String settingName, String friendlyName, String settingGroupName, boolean passwordField, int weight) {
        this.settingName = settingName;
        this.friendlyName = friendlyName;
        this.settingGroupName = settingGroupName;
        this.passwordField = passwordField;
        this.weight = weight;
    }

    public String getSettingName() {
        return settingName;
    }

    public String getSettingGroupName() {
        return settingGroupName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public boolean isPasswordField() {
        return passwordField;
    }

    public static Map<String, List<ServerSettingRequired>> getGroupMap(){
        return Stream.of(values())
                .collect(Collectors.groupingBy(v -> v.getSettingGroupName()));
    }

    public static Set<String> getGroupNames(){
        return Stream.of(values())
                .map(v -> v.getSettingGroupName())
                .collect(Collectors.toSet());
    }

    public static List<ServerSettingRequired> getByGroupName(String groupName){
        return Stream.of(values())
                .filter(s -> s.getSettingGroupName().contentEquals(groupName))
                .sorted((s1, s2) -> s1.weight.compareTo(s2.weight))
                .collect(Collectors.toList());
    }


}
