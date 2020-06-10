package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.service.external.beans.ExternalSecurityPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//service to pull security attributes from, like Xacta
//Security attributes will be used by AttributePolicyService to apply security attributes to Server Entries
@Service
public class SecurityPolicyService {
    //todo

    @Autowired
    private List<SecurityPolicyConnection> connections;

    public ExternalSecurityPolicy lookupSecurityPolicyForFqdn(String fqdn){
        //todo
        return null;
    }

    public Map<String, String> getSecurityPolicyMapForService(String serviceName, ServerEntry serverEntry) throws Exception {
        //todo
        SecurityPolicyConnection connection = getConnection(serviceName);

        return connection.getSecurityPolicyMapForService(serverEntry);
    }

    public List<String> getSecurityPolicyNamesForService(String serviceName, ServerEntry serverEntry) throws Exception {
        Map<String, String> map = getSecurityPolicyMapForService(serviceName, serverEntry);
        return new ArrayList<>(map.keySet());
    }

    public SecurityPolicyConnection getConnection(String name) throws Exception {
        for(SecurityPolicyConnection connection : connections){
            if(connection.getConnectionName().equals(name)) return connection;
        }
        throw new Exception("No connection: "+name);
    }

}
