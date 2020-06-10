package com.winllc.pki.ra.service.external.vendorimpl;

import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.service.external.SecurityPolicyConnection;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OpenDJSecurityPolicyConnection implements SecurityPolicyConnection {

    @Override
    public Map<String, String> getSecurityPolicyMapForService(ServerEntry serverEntry) {
        //todo
        Map<String, String> testMap = new HashMap<>();

        return testMap;
    }

    @Override
    public String getConnectionName() {
        return "opendj";
    }
}
