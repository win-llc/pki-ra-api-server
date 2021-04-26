package com.winllc.pki.ra.service;

import com.winllc.acme.common.SubjectAltName;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.beans.info.PocEntryInfo;
import com.winllc.pki.ra.beans.info.ServerEntryInfo;
import com.winllc.pki.ra.beans.validator.ValidationResponse;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.*;
import com.winllc.pki.ra.service.external.EntityDirectoryService;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakOIDCProviderConnection;
import com.winllc.pki.ra.service.transaction.SystemActionRunner;
import com.winllc.pki.ra.service.transaction.ThrowingSupplier;
import com.winllc.pki.ra.service.validators.ServerEntryValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.naming.InvalidNameException;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/serverEntry")
public class ServerEntryService extends AbstractService {

    private static final Logger log = LogManager.getLogger(ServerEntryService.class);

    private final ServerEntryRepository serverEntryRepository;
    private final AccountRepository accountRepository;
    //todo replace with OIDCProviderService
    private final KeycloakOIDCProviderConnection oidcProviderConnection;
    private final PocEntryRepository pocEntryRepository;
    private final EntityDirectoryService entityDirectoryService;
    private final ServerEntryValidator serverEntryValidator;
    private final AuthCredentialService authCredentialService;
    @Autowired
    private DomainRepository domainRepository;

    public ServerEntryService(ApplicationContext context,
                              ServerEntryRepository serverEntryRepository, AccountRepository accountRepository,
                              KeycloakOIDCProviderConnection oidcProviderConnection, PocEntryRepository pocEntryRepository,
                              EntityDirectoryService entityDirectoryService, ServerEntryValidator serverEntryValidator,
                              AuthCredentialService authCredentialService) {
        super(context);
        this.serverEntryRepository = serverEntryRepository;
        this.accountRepository = accountRepository;
        this.oidcProviderConnection = oidcProviderConnection;
        this.pocEntryRepository = pocEntryRepository;
        this.entityDirectoryService = entityDirectoryService;
        this.serverEntryValidator = serverEntryValidator;
        this.authCredentialService = authCredentialService;
    }

    @InitBinder("serverEntryForm")
    public void initBinder(WebDataBinder binder) {
        binder.setValidator(serverEntryValidator);
    }

    public ServerEntry getServerEntry(Long id) throws RAObjectNotFoundException {
        return serverEntryRepository.findById(id).orElseThrow(() -> new RAObjectNotFoundException(ServerEntry.class, id));
    }

    @GetMapping("/variableFields")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getAvailableVariableFields() {
        return Stream.of(ServerEntry.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(EntityVariableField.class))
                .map(field -> field.getName())
                .collect(Collectors.toList());
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Long createServerEntry(@Valid @RequestBody ServerEntryForm form) throws RAObjectNotFoundException {

        Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());

        if (optionalAccount.isPresent()) {
            Account account = optionalAccount.get();

            //Don't proceed if FQDN already exists under account
            Optional<ServerEntry> optionalExisting = serverEntryRepository.findDistinctByFqdnEqualsAndAccountEquals(form.getFqdn(), account);
            if(optionalExisting.isPresent()){
                return optionalExisting.get().getId();
            }

            String fqdn = form.getFqdn();

            if(form.getDomainId() != null) {
                Optional<Domain> optionalDomain = domainRepository.findById(form.getDomainId());
                if(optionalDomain.isPresent()){
                    Domain domain = optionalDomain.get();
                    String fullDomain = domain.getFullDomainName();
                    fqdn = fqdn + "." + fullDomain;
                }
            }

            //Domain domain = optionalDomain.get();
            ServerEntry entry = ServerEntry.buildNew();
            entry.setAccount(account);
            //entry.setDomainParent(domain);
            entry.setFqdn(fqdn);
            entry.setHostname(form.getFqdn());

            if (!CollectionUtils.isEmpty(form.getAlternateDnsValues())) {
                entry.setAlternateDnsValues(form.getAlternateDnsValues());
            }

            entry = serverEntryRepository.save(entry);
            entry = (ServerEntry) authCredentialService.addNewAuthCredentialToEntry(entry);
            entry = serverEntryRepository.save(entry);

            SystemActionRunner.build(context)
                    .createAuditRecord(AuditRecordType.SERVER_ENTRY_ADDED, entry)
                    .createNotificationForAccountPocs(Notification.buildNew()
                            .addMessage("Server Entry added: " + entry.getFqdn()), account)
                    .sendNotification()
                    .execute();

            //apply attributes to external directory

            ServerEntry finalEntry = entry;
            ThrowingSupplier<Boolean, Exception> action = () -> {
                entityDirectoryService.applyServerEntryToDirectory(finalEntry);
                return true;
            };

            SystemActionRunner.build(context)
                    .executeAsync(action);

            log.info("Created a Server Entry: " + entry);

            return entry.getId();
        } else {
            throw new RAObjectNotFoundException(Account.class, form.getAccountId());
        }

    }

    @PostMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public ServerEntryInfo updateServerEntry(@Valid @RequestBody ServerEntryForm form) throws RAException {
        //todo update attributes in directory

        log.info("Is account linked: " + form.isAccountLinkedForm());

        ServerEntry serverEntry = getServerEntry(form.getId());

        serverEntry.setAlternateDnsValues(form.getAlternateDnsValues());
        serverEntry.setOpenidClientRedirectUrl(form.getOpenidClientRedirectUrl());

        List<String> alternateDnsValues = new ArrayList<>(serverEntry.getAlternateDnsValues());
        serverEntry.setAlternateDnsValues(alternateDnsValues);

        serverEntry = serverEntryRepository.save(serverEntry);

        SystemActionRunner.build(context)
                .createAuditRecord(AuditRecordType.SERVER_ENTRY_UPDATED, serverEntry)
                .createNotificationForAccountPocs(Notification.buildNew()
                        .addMessage("Server Entry updated: " + serverEntry.getFqdn()), serverEntry.getAccount())
                .sendNotification()
                .execute();

        //apply attributes to external directory
        entityDirectoryService.applyServerEntryToDirectory(serverEntry);

        return entryToInfo(serverEntry);
    }

    @GetMapping("/getSans/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<SubjectAltName> getSans(@PathVariable Long id) throws RAObjectNotFoundException {

        ServerEntry serverEntry = getServerEntry(id);
        Hibernate.initialize(serverEntry.getAlternateDnsValues());

        return serverEntry.getAlternateDnsValues().stream()
                .map(a -> new SubjectAltName(a, "dns"))
                .collect(Collectors.toList());
    }

    @PostMapping("/updateSans/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<SubjectAltName> updateSans(@PathVariable Long id, @RequestBody List<SubjectAltName> sans) throws RAObjectNotFoundException {
        ServerEntry serverEntry = getServerEntry(id);

        if(!CollectionUtils.isEmpty(sans)){
            List<String> dns = sans.stream()
                    .map(s -> s.getValue())
                    .collect(Collectors.toList());

            serverEntry.setAlternateDnsValues(dns);
        }else{
            serverEntry.setAlternateDnsValues(new ArrayList<>());
        }

        serverEntryRepository.save(serverEntry);

        return getSans(id);
    }

    @GetMapping("/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public ServerEntryInfo getServerEntryInfo(@PathVariable Long id) throws RAObjectNotFoundException {

        ServerEntry entry = getServerEntry(id);

        return entryToInfo(entry);
    }

    @GetMapping("/latestAuthCredential/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public AuthCredential getLatestAuthCredential(@PathVariable Long id) throws RAObjectNotFoundException {

        ServerEntry entry = getServerEntry(id);

        Optional<AuthCredential> optionalAuthCredential = entry.getLatestAuthCredential();
        if (optionalAuthCredential.isPresent()) {
            return optionalAuthCredential.get();
        } else {
            log.error("No credential for server: " + id);
            throw new RAObjectNotFoundException(AuthCredential.class, id);
        }
    }


    @GetMapping("/allByAccountId/{accountId}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<ServerEntryInfo> getAllServerEntriesForAccount(@PathVariable Long accountId) {

        List<ServerEntry> allByAccountId = serverEntryRepository.findAllByAccountId(accountId);

        return allByAccountId.stream()
                .map(s -> new ServerEntryInfo(s))
                .collect(Collectors.toList());
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<ServerEntryInfo> getAll() {
        List<ServerEntryInfo> infoList = serverEntryRepository.findAll()
                .stream().map(i -> entryToInfo(i))
                .collect(Collectors.toList());
        return infoList;
    }

    @GetMapping("/allForUser")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<ServerEntryInfo> getAllServerEntriesForUser(@AuthenticationPrincipal UserDetails raUser) {
        List<PocEntry> pocEntries = pocEntryRepository.findAllByEmailEquals(raUser.getUsername());
        List<Account> allByAccountUsersContains = accountRepository.findAllByPocsIn(pocEntries);

        List<ServerEntryInfo> entries = new ArrayList<>();
        for (Account account : allByAccountUsersContains) {
            List<ServerEntryInfo> temp = serverEntryRepository.findAllByAccountId(account.getId())
                    .stream().map(i -> entryToInfo(i))
                    .collect(Collectors.toList());
            entries.addAll(temp);
        }

        return entries;
    }


    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteServerEntry(@PathVariable Long id) throws RAException {

        ServerEntry serverEntry = getServerEntry(id);

        //if server entry is deleted, remove the OIDC client if it exists
        if (StringUtils.isNotBlank(serverEntry.getOpenidClientId())) {
            try {
                oidcProviderConnection.deleteClient(serverEntry);
            } catch (Exception e) {
                log.error("Could not delete OID Client", e);
            }
        }

        serverEntryRepository.deleteById(id);

        SystemActionRunner.build(context)
                .createAuditRecord(AuditRecordType.SERVER_ENTRY_REMOVED, serverEntry)
                .execute();
    }

    @GetMapping("/calculateAttributes/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public Map<String, Object> getCalculatedAttributes(@PathVariable Long id) throws RAObjectNotFoundException {

        ServerEntry serverEntry = getServerEntry(id);

        Map<String, Object> attributeMap = entityDirectoryService.calculateAttributePolicyMapForServerEntry(serverEntry);

        return attributeMap;
    }

    @GetMapping("/currentAttributes/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public Map<String, Object> getCurrentAttributes(@PathVariable Long id) throws RAObjectNotFoundException, InvalidNameException {
        ServerEntry serverEntry = getServerEntry(id);

        Map<String, Object> attributeMap = entityDirectoryService.getCurrentAttributesForServer(serverEntry);

        return attributeMap;
    }

    @PostMapping("/syncAttributes/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public boolean syncAttributes(@PathVariable Long id) throws RAObjectNotFoundException {
        ServerEntry serverEntry = getServerEntry(id);

        entityDirectoryService.applyServerEntryToDirectory(serverEntry);

        return true;
    }

    @GetMapping("/getManagers/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<PocEntryInfo> getManagers(@PathVariable Long id) throws RAObjectNotFoundException {

        ServerEntry serverEntry = getServerEntry(id);
        List<PocEntry> pocs = pocEntryRepository.findAllByManagesContaining(serverEntry);

        if (!CollectionUtils.isEmpty(pocs)) {
            return pocs.stream()
                    .map(PocEntryInfo::new)
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @PostMapping("/updateManagers/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<PocEntryInfo> updateManagers(@PathVariable Long id, @RequestBody List<Long> pocs) throws RAObjectNotFoundException {

        ServerEntry serverEntry = getServerEntry(id);

        serverEntry.getManagedBy().clear();

        List<PocEntry> existing = pocEntryRepository.findAllByManagesContaining(serverEntry);

        List<PocEntry> existingNotINPocs = existing.stream()
                .filter(p -> !pocs.contains(p.getId()))
                .collect(Collectors.toList());

        existingNotINPocs.forEach(p -> {
            p.getManages().remove(serverEntry);
            pocEntryRepository.save(p);
        });

        for (Long poc : pocs) {

            Optional<PocEntry> optionalPocEntry = pocEntryRepository.findById(poc);
            if (optionalPocEntry.isPresent()) {
                PocEntry pocEntry = optionalPocEntry.get();
                pocEntry.getManages().add(serverEntry);
                pocEntry = pocEntryRepository.save(pocEntry);

                serverEntry.getManagedBy().add(pocEntry);
            }
        }

        serverEntryRepository.save(serverEntry);

        return getManagers(id);
    }

    private ServerEntryInfo entryToInfo(ServerEntry entry) {
        Hibernate.initialize(entry.getAlternateDnsValues());
        return new ServerEntryInfo(entry);
    }


}
