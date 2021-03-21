package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.service.external.LdapSecurityPolicyServerService;
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

    //@Autowired
    //private List<SecurityPolicyConnection> connections;

    @Autowired
    private LdapSecurityPolicyServerService policyServerService;

    //private String temporaryStaticPolicyServerName = "opendj-security-policy-service";

    @GetMapping("/allProjects")
    public List<SecurityPolicyServerProjectDetails> getAllProjectDetails() throws Exception {
        return policyServerService.getAllProjects();
    }

    @GetMapping("/projectDetails/{projectId}")
    public SecurityPolicyServerProjectDetails getProjectDetails(@PathVariable String projectId) throws Exception {
        Optional<SecurityPolicyServerProjectDetails> optionalDetails = policyServerService.getProjectDetails(projectId);
        if(optionalDetails.isPresent()){
            return optionalDetails.get();
        }else{
            throw new RAObjectNotFoundException(SecurityPolicyServerProjectDetails.class, projectId);
        }
    }

    @GetMapping("/projectDetails/{projectId}/attributes")
    public Map<String, List<String>> getProjectAttributes(@PathVariable String projectId) throws Exception {
        Optional<SecurityPolicyServerProjectDetails> optionalDetails = policyServerService.getProjectDetails(projectId);
        if(optionalDetails.isPresent()){
            return optionalDetails.get().getAllSecurityAttributesMap();
        }else{
            throw new RAObjectNotFoundException(SecurityPolicyServerProjectDetails.class, projectId);
        }
    }


}
