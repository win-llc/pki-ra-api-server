package com.winllc.pki.ra.service.external;

import com.winllc.acme.common.domain.ServerEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SecurityPolicyConnection extends ExternalServiceConnection {
    //todo
    Map<String, String> getSecurityPolicyMapForService(String fqdn, String projectId);
    Optional<SecurityPolicyServerProjectDetails> getProjectDetails(String projectId);
    List<SecurityPolicyServerProjectDetails> getAllProjects();
    //todo for ldap, use javaSerializedObject
    //void saveProjectDetails(SecurityPolicyServerProjectDetails details);
}
