package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.AccountRequest;
import com.winllc.acme.common.domain.PocEntry;
import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.PocEntryRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.pki.ra.beans.form.PocEntryForm;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.ws.rs.PathParam;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/poc")
public class PocEntryService extends ServerEntryDataTableService<PocEntry, PocEntryForm> {

    private final PocEntryRepository pocEntryRepository;
    private final AccountRepository accountRepository;

    public PocEntryService(ApplicationContext context,
            PocEntryRepository pocEntryRepository, ServerEntryRepository serverEntryRepository,
                           AccountRepository accountRepository) {
        super(context, serverEntryRepository, accountRepository, pocEntryRepository);
        this.pocEntryRepository = pocEntryRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/account/{id}/all")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<PocFormEntry> getAccountPocs(@PathVariable Long id, Authentication authentication) throws RAObjectNotFoundException {
        Account account = accountRepository.findById(id).orElseThrow(() -> new RAObjectNotFoundException(Account.class, id));

        List<PocEntry> pocEntries = pocEntryRepository.findAllByAccount(account);

        return pocEntries.stream()
                .map(p -> new PocFormEntry(p))
                .collect(Collectors.toList());
    }

    @PostMapping("/account/{id}/add")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public PocFormEntry addPocToAccount(@PathVariable Long id, @RequestBody PocFormEntry form)
            throws RAObjectNotFoundException {
        Optional<Account> optionalAccount = accountRepository.findById(id);

        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();

            PocEntry pocEntry = new PocEntry();
            pocEntry.setAccount(account);
            pocEntry.setEmail(form.getEmail());
            pocEntry.setEnabled(true);
            pocEntry.setOwner(form.isOwner());
            pocEntry.setCanManageAllServers(form.isCanManageAllServers());
            pocEntry = pocEntryRepository.save(pocEntry);
            return new PocFormEntry(pocEntry);
        } else {
            throw new RAObjectNotFoundException(Account.class, id);
        }
    }

    @PostMapping("/account/{id}/update")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public PocFormEntry updatePocOnAccount(@PathVariable Long id, @RequestBody PocFormEntry form) throws RAObjectNotFoundException {
        Optional<Account> optionalAccount = accountRepository.findById(id);

        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();
            Optional<PocEntry> optionalPoc = pocEntryRepository.findById(form.getId());
            if(optionalPoc.isPresent()){
                PocEntry pocEntry = optionalPoc.get();
                pocEntry.setCanManageAllServers(form.isCanManageAllServers());
                pocEntry.setOwner(form.isOwner());
                pocEntry = pocEntryRepository.save(pocEntry);
                return new PocFormEntry(pocEntry);
            }else{
                throw new RAObjectNotFoundException(PocEntry.class, form.getId());
            }
        } else {
            throw new RAObjectNotFoundException(Account.class, id);
        }
    }

    @DeleteMapping("/account/{id}/delete/{pocId}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public void removePocFromAccount(@PathVariable Long id, @PathVariable Long pocId) throws RAObjectNotFoundException {
        Optional<Account> optionalAccount = accountRepository.findById(id);

        if (optionalAccount.isPresent()) {
            //Account account = optionalAccount.get();
            pocEntryRepository.deleteById(pocId);
        } else {
            throw new RAObjectNotFoundException(Account.class, id);
        }
    }

    @Override
    protected PocEntryForm entityToForm(PocEntry entity) {
        return new PocEntryForm(entity);
    }

    @Override
    protected PocEntry formToEntity(PocEntryForm form, Object parent) throws RAObjectNotFoundException {
        PocEntry pocEntry = new PocEntry();
        pocEntry.setEmail(form.getEmail());
        if(parent instanceof ServerEntry){
            pocEntry.setServerEntry((ServerEntry) parent);
        }else if(parent instanceof Account){
            pocEntry.setAccount((Account) parent);
        }
        return pocEntry;
    }

    @Override
    protected PocEntry combine(PocEntry original, PocEntry updated) {
        return null;
    }
}
