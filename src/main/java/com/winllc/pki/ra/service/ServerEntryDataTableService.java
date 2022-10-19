package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.*;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.BaseAccountRepository;
import com.winllc.acme.common.repository.BaseServerEntryRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import com.winllc.pki.ra.beans.form.ValidForm;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ServerEntryDataTableService<T extends BaseServerEntryEntity, F extends ValidForm<T>>
        extends AbstractService {

    private final ServerEntryRepository serverEntryRepository;
    private final AccountRepository accountRepository;
    private final BaseServerEntryRepository<T> entityServerEntryRepository;

    public ServerEntryDataTableService(ApplicationContext context,
                                       ServerEntryRepository serverEntryRepository,
                                       AccountRepository accountRepository,
                                       BaseServerEntryRepository<T> entityServerEntryRepository) {
        super(context);
        this.serverEntryRepository = serverEntryRepository;
        this.accountRepository = accountRepository;
        this.entityServerEntryRepository = entityServerEntryRepository;
    }

    @GetMapping("/{type}/{id}/all")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<F> getAll(@PathVariable String type, @PathVariable Long id, Authentication authentication) throws RAObjectNotFoundException {
        UniqueEntity entity = getType(type, id);
        return all(entity, authentication);
    }



    @PostMapping("/{type}/{id}/add")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public F addRest(@PathVariable String type, @PathVariable Long id, @RequestBody F form, Authentication authentication) throws RAObjectNotFoundException {
        UniqueEntity entity = getType(type, id);
        return add(entity, form, authentication);
    }

    @PostMapping("/{type}/{id}/update")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public F updateRest(@PathVariable String type, @PathVariable Long id, @RequestBody F form, Authentication authentication) throws RAObjectNotFoundException {
        UniqueEntity entity = getType(type, id);
        return update(entity, form, authentication);
    }

    @DeleteMapping("/{type}/{id}/delete/{entityId}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public void deleteRest(@PathVariable String type, @PathVariable Long id, @PathVariable Long entityId,  Authentication authentication) throws RAObjectNotFoundException {
        UniqueEntity entity = getType(type, id);
        delete(entity, entityId, authentication);
    }

    private UniqueEntity getType(String type, Long id) throws RAObjectNotFoundException {
        UniqueEntity entity = switch (type) {
            case "server" ->
                    serverEntryRepository.findById(id).orElseThrow(() -> new RAObjectNotFoundException(ServerEntry.class, id));
            case "account" ->
                    accountRepository.findById(id).orElseThrow(() -> new RAObjectNotFoundException(Account.class, id));
            default -> null;
        };
        if(entity != null){
            return entity;
        }else{
            throw new RAObjectNotFoundException(Object.class, id);
        }
    }

    protected List<F> all(UniqueEntity obj, Authentication authentication){
        List<T> entries = new ArrayList<>();
        if(obj instanceof ServerEntry) {
            entries = entityServerEntryRepository.findAllByServerEntry((ServerEntry) obj);
        }else if(obj instanceof Account){
            entries = entityServerEntryRepository.findAllByAccount((Account) obj);
        }
        return entries.stream()
                .map(p -> entityToForm(p))
                .collect(Collectors.toList());
    }

    protected F add(UniqueEntity parent, F form, Authentication authentication) throws RAObjectNotFoundException {
        T entity = formToEntity(form, parent);

        if(parent instanceof ServerEntry) {
            entity.setServerEntry((ServerEntry) parent);
        }else if(parent instanceof Account){
            entity.setAccount((Account) parent);
        }

        entity = entityServerEntryRepository.save(entity);
        return entityToForm(entity);
    }

    protected F update(UniqueEntity parent, F form, Authentication authentication) throws RAObjectNotFoundException {
        T entity = entityServerEntryRepository.findById(form.getId()).orElseThrow(() -> new RAObjectNotFoundException(form));
        T newEntity = formToEntity(form, parent);
        newEntity = combine(entity, newEntity);
        entity = entityServerEntryRepository.save(newEntity);
        return entityToForm(entity);
    }

    protected void delete(UniqueEntity account, Long id, Authentication authentication) throws RAObjectNotFoundException{
        entityServerEntryRepository.deleteById(id);
    }

    protected abstract F entityToForm(T entity);
    protected abstract T formToEntity(F form, Object parent) throws RAObjectNotFoundException;
    protected abstract T combine(T original, T updated);
}
