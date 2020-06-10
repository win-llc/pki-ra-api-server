package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.service.external.beans.ExternalSecurityPolicy;
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

    public ExternalSecurityPolicy lookupSecurityPolicyForFqdn(String fqdn){
        //todo
        return null;
    }

    public Map<String, String> getSecurityPolicyMapForService(String serviceName){
        //todo
        return new HashMap<>();
    }

    public List<String> getSecurityPolicyNamesForService(String serviceName){
        Map<String, String> map = getSecurityPolicyMapForService(serviceName);
        return new ArrayList<>(map.keySet());
    }
}
