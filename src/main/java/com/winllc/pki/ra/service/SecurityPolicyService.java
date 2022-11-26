package com.winllc.pki.ra.service;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.pki.ra.beans.search.GridModel;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.service.external.LdapSecurityPolicyServerService;
import com.winllc.pki.ra.service.external.SecurityPolicyConnection;
import com.winllc.pki.ra.service.external.SecurityPolicyServerProjectDetails;
import com.winllc.pki.ra.service.external.beans.ExternalSecurityPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.*;

//service to pull security attributes from, like Xacta
//Security attributes will be used by AttributePolicyService to apply security attributes to Server Entries
@RestController
@RequestMapping("/api/securityPolicyService")
public class SecurityPolicyService implements UpdatedDataService<SecurityPolicyServerProjectDetails, String> {
    //todo

    //@Autowired
    //private List<SecurityPolicyConnection> connections;

    private final LdapSecurityPolicyServerService policyServerService;

    public SecurityPolicyService(LdapSecurityPolicyServerService policyServerService) {
        this.policyServerService = policyServerService;
    }

    //private String temporaryStaticPolicyServerName = "opendj-security-policy-service";

    @GetMapping("/allProjects")
    public List<SecurityPolicyServerProjectDetails> getAllProjectDetails() throws Exception {
        return policyServerService.getAllProjects();
    }

    @GetMapping("/projectDetails/{projectId}")
    public SecurityPolicyServerProjectDetails getProjectDetails(@PathVariable String projectId) {
        Optional<SecurityPolicyServerProjectDetails> optionalDetails = policyServerService.getProjectDetails(projectId);
        if(optionalDetails.isPresent()){
            return optionalDetails.get();
        }else{
            return null;
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


    @Override
    @PostMapping("/paged")
    public Page<SecurityPolicyServerProjectDetails> getPaged(@RequestParam Integer page,
                                                @RequestParam Integer pageSize,
                                                @RequestParam(defaultValue = "asc") String order,
                                                @RequestParam(required = false) String sortBy,
                                                @RequestParam Map<String, String> allRequestParams,
                                                @RequestBody GridModel gridModel,
                                                Authentication authentication) {

        List<SecurityPolicyServerProjectDetails> allProjects = policyServerService.getAllProjects();
        return new PageImpl<>(allProjects);
    }

    @Override
    public Page<SecurityPolicyServerProjectDetails> getMyPaged(Integer page, Integer pageSize, String order, String sortBy, Map<String, String> allRequestParams, GridModel gridModel, Authentication authentication) {
        return null;
    }

    @Override
    public List<SecurityPolicyServerProjectDetails> getAll(Authentication authentication) throws Exception {
        return null;
    }

    @Override
    @GetMapping("/id/{id}")
    public SecurityPolicyServerProjectDetails findRest(@PathVariable String id,
                                                       Authentication authentication) throws Exception {
        Optional<SecurityPolicyServerProjectDetails> optionalDetails = policyServerService.getProjectDetails(id);
        return optionalDetails.orElse(null);
    }

    @Override
    public SecurityPolicyServerProjectDetails addRest(SecurityPolicyServerProjectDetails entity, Map<String, String> allRequestParams, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    public SecurityPolicyServerProjectDetails updateRest(SecurityPolicyServerProjectDetails entity, Map<String, String> allRequestParams, Authentication authentication) throws Exception {
        return null;
    }

    @Override
    public void deleteRest(String id, SecurityPolicyServerProjectDetails form, Authentication authentication) throws Exception {

    }
}
