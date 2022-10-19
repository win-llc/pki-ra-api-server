package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.BaseAccountEntity;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.BaseAccountRepository;
import com.winllc.pki.ra.beans.form.ValidForm;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AccountDataTableService<T extends BaseAccountEntity, F extends ValidForm<T>>
        extends AbstractService {

    private final AccountRepository accountRepository;
    private final BaseAccountRepository<T> entityAccountRepository;

    public AccountDataTableService(ApplicationContext context,
                                   AccountRepository accountRepository,
                                   BaseAccountRepository<T> entityAccountRepository) {
        super(context);
        this.accountRepository = accountRepository;
        this.entityAccountRepository = entityAccountRepository;
    }

    @GetMapping("/account/{id}/all")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<F> getAll(@PathVariable Long id, Authentication authentication) throws RAObjectNotFoundException {
        Account account = accountRepository.findById(id).orElseThrow(() -> new RAObjectNotFoundException(Account.class, id));

        return all(account, authentication);
    }

    @PostMapping("/account/{id}/add")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public F addRest(@PathVariable Long id, @RequestBody F entity, Authentication authentication) throws RAObjectNotFoundException {
        Account account = accountRepository.findById(id).orElseThrow(() -> new RAObjectNotFoundException(Account.class, id));

        return add(account, entity, authentication);
    }

    @PostMapping("/account/{id}/update")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public F updateRest(@PathVariable Long id, @RequestBody F entity, Authentication authentication) throws RAObjectNotFoundException {
        Account account = accountRepository.findById(id).orElseThrow(() -> new RAObjectNotFoundException(Account.class, id));

        return update(account, entity, authentication);
    }

    @DeleteMapping("/account/{id}/delete/{entityId}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public void deleteRest(@PathVariable Long id, @PathVariable Long entityId,  Authentication authentication) throws RAObjectNotFoundException {
        Account account = accountRepository.findById(id).orElseThrow(() -> new RAObjectNotFoundException(Account.class, id));

        delete(account, entityId, authentication);
    }

    protected List<F> all(Account account, Authentication authentication){
        List<T> entries = entityAccountRepository.findAllByAccount(account);
        return entries.stream()
                .map(p -> entityToForm(p))
                .collect(Collectors.toList());
    }

    protected F add(Account account, F form, Authentication authentication) throws RAObjectNotFoundException {
        T entity = formToEntity(form);
        entity.setAccount(account);
        entity = entityAccountRepository.save(entity);
        return entityToForm(entity);
    }

    protected F update(Account account, F form, Authentication authentication) throws RAObjectNotFoundException {
        T entity = entityAccountRepository.findById(form.getId()).orElseThrow(() -> new RAObjectNotFoundException(form));
        T newEntity = formToEntity(form);
        newEntity = combine(entity, newEntity);
        entity = entityAccountRepository.save(newEntity);
        return entityToForm(entity);
    }

    protected void delete(Account account, Long id, Authentication authentication) throws RAObjectNotFoundException{
        entityAccountRepository.deleteById(id);
    }

    protected abstract F entityToForm(T entity);
    protected abstract T formToEntity(F form) throws RAObjectNotFoundException;
    protected abstract T combine(T original, T updated);
}
