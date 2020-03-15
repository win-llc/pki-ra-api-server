package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.domain.AttributePolicyGroup;
import com.winllc.pki.ra.domain.ServerEntry;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EntityDirectoryService {
    //todo attach to a directory, allow attribute updating and validation


    public void addServerEntryToDirectory(ServerEntry serverEntry){
        //todo
        serverEntry.getPolicyGroups();
    }

    public void applyAttributesToServerEntry(ServerEntry serverEntry, Map<String, Object> attributeValueMap){
        //todo
    }

    public void applyAttributePolicyGroup(ServerEntry serverEntry, AttributePolicyGroup policyGroup){
        //todo


    }
}
