package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.service.external.beans.ExternalSecurityPolicy;
import org.springframework.stereotype.Service;

//service to pull security attributes from, like Xacta
//Security attributes will be used by AttributePolicyService to apply security attributes to Server Entries
@Service
public class SecurityPolicyService {
    //todo

    public ExternalSecurityPolicy lookupSecurityPolicyForFqdn(String fqdn){
        //todo
        return null;
    }
}
