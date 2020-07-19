package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.service.external.SecurityPolicyConnection;
import com.winllc.pki.ra.service.external.SecurityPolicyServerProjectDetails;
import com.winllc.pki.ra.service.external.beans.ExternalSecurityPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

//service to pull security attributes from, like Xacta
//Security attributes will be used by AttributePolicyService to apply security attributes to Server Entries
@RestController
@RequestMapping("/api/securityPolicyService")
public class SecurityPolicyService {
    //todo

    @Autowired
    private List<SecurityPolicyConnection> connections;

    private String temporaryStaticPolicyServerName = "opendj-security-policy-service";

    @GetMapping("/allProjects")
    public List<SecurityPolicyServerProjectDetails> getAllProjectDetails() throws Exception {
        return getAllProjects(temporaryStaticPolicyServerName);
    }

    @GetMapping("/projectDetails/{projectId}")
    public SecurityPolicyServerProjectDetails getProjectDetails(@PathVariable String projectId) throws Exception {
        Optional<SecurityPolicyServerProjectDetails> optionalDetails =
                getPolicyServerProjectDetails(temporaryStaticPolicyServerName, projectId);
        if(optionalDetails.isPresent()){
            return optionalDetails.get();
        }else{
            throw new RAObjectNotFoundException(SecurityPolicyServerProjectDetails.class, projectId);
        }
    }

    public Map<String, String> getSecurityPolicyMapForService(String serviceName, String fqdn,
                                                              String projectId) throws Exception {
        SecurityPolicyConnection connection = getConnection(temporaryStaticPolicyServerName);

        return connection.getSecurityPolicyMapForService(fqdn, projectId);
    }

    public List<String> getSecurityPolicyNamesForService(String serviceName, String fqdn,
                                                         String projectId) throws Exception {
        Map<String, String> map = getSecurityPolicyMapForService(temporaryStaticPolicyServerName, fqdn, projectId);
        return new ArrayList<>(map.keySet());
    }

    public Optional<SecurityPolicyServerProjectDetails> getPolicyServerProjectDetails(String serviceName,
                                                                                      String projectId) throws Exception {
        SecurityPolicyConnection connection = getConnection(temporaryStaticPolicyServerName);

        return connection.getProjectDetails(projectId);
    }

    public List<SecurityPolicyServerProjectDetails> getAllProjects(String serviceName) throws Exception {
        SecurityPolicyConnection connection = getConnection(temporaryStaticPolicyServerName);

        return connection.getAllProjects();
    }

    private SecurityPolicyConnection getConnection(String name) throws Exception {
        for(SecurityPolicyConnection connection : connections){
            if(connection.getConnectionName().equals(name)) return connection;
        }
        throw new Exception("No connection: "+name);
    }

    public List<SecurityPolicyConnection> getAllConnections(){
        return connections;
    }

}
