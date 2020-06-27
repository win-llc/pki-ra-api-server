package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.domain.ServerEntry;

import java.util.Map;

public interface SecurityPolicyConnection extends ExternalServiceConnection {
    //todo
    Map<String, String> getSecurityPolicyMapForService(String fqdn);
}
