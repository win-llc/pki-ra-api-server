package com.winllc.pki.ra.cron;

import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.AttributePolicyGroup;
import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.acme.common.repository.ServerEntryRepository;
import com.winllc.pki.ra.service.external.EntityDirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;

//find all servers associated with policy group, update their corresponding ldap records
@Component
public class LdapObjectUpdater  {

    @Qualifier("taskExecutor")
    @Autowired
    private TaskExecutor taskExecutor;
    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private EntityDirectoryService entityDirectoryService;

    @Transactional
    public void update(AttributePolicyGroup apg){
        taskExecutor.execute(() -> run(apg));
    }

    private void run(AttributePolicyGroup apg){
        Account account = apg.getAccount();
        if(account != null){
            List<ServerEntry> serverEntries = serverEntryRepository.findAllByAccount(account);

            for(ServerEntry serverEntry : serverEntries) {
                entityDirectoryService.applyServerEntryToDirectory(serverEntry);
            }
        }
    }


}
