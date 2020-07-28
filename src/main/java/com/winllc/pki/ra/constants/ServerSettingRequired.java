package com.winllc.pki.ra.constants;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ServerSettingRequired {
    EMAIL_SERVER_HOST("emailServer", "Email Server Host", "Email", false),
    EMAIL_SERVER_PORT("emailServerPort", "Email Server Port", "Email", false),
    EMAIL_SERVER_FROM_ADDRESS("emailFromAddress", "Default From Address", "Email", false),

    ENTITY_DIRECTORY_LDAP_URL("entityDirectoryLdapUrl", "Entity Directory LDAP URL", "Entity Directory", false),
    ENTITY_DIRECTORY_LDAP_USERNAME("entityDirectoryLdapUsername", "Username", "Entity Directory", false),
    ENTITY_DIRECTORY_LDAP_PASSWORD("entityDirectoryLdapPassword", "Password", "Entity Directory", true),
    ENTITY_DIRECTORY_LDAP_BASEDN("entityDirectoryLdapBaseDn", "Base DN", "Entity Directory", false),
    //OIDC_SERVER_BASE_URL("openIdServerBaseUrl", "OIDC", false),
    //OIDC_ENABLED("openIdConnectEnabled", "OIDC", false),

    ;

    private final String settingName;
    private final String friendlyName;
    private final String settingGroupName;
    private final boolean passwordField;

    ServerSettingRequired(String settingName, String friendlyName, String settingGroupName, boolean passwordField) {
        this.settingName = settingName;
        this.friendlyName = friendlyName;
        this.settingGroupName = settingGroupName;
        this.passwordField = passwordField;
    }

    public String getSettingName() {
        return settingName;
    }

    public String getSettingGroupName() {
        return settingGroupName;
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
                .collect(Collectors.toList());
    }
}
