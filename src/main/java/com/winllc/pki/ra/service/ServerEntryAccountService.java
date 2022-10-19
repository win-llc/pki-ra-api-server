package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.BaseAccountRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/serverEntryAccount")
public class ServerEntryAccountService extends AccountDataTableService<ServerEntry, ServerEntryForm>{

    public ServerEntryAccountService(ApplicationContext context,
                                     AccountRepository accountRepository,
                                     ServerEntryRepository entityAccountRepository) {
        super(context, accountRepository, entityAccountRepository);
    }

    @Override
    protected ServerEntryForm entityToForm(ServerEntry entity) {
        Hibernate.initialize(entity.getAlternateDnsValues());
        return new ServerEntryForm(entity);
    }

    @Override
    protected ServerEntry formToEntity(ServerEntryForm form) throws RAObjectNotFoundException {
        return null;
    }

    @Override
    protected ServerEntry combine(ServerEntry original, ServerEntry updated) {
        return null;
    }
}
