package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.beans.info.PocEntryInfo;
import com.winllc.pki.ra.beans.info.ServerEntryInfo;
import com.winllc.acme.common.constants.AuditRecordType;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.*;
import com.winllc.pki.ra.service.external.EntityDirectoryService;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakOIDCProviderConnection;
import com.winllc.pki.ra.service.transaction.SystemActionRunner;
import com.winllc.pki.ra.service.transaction.ThrowingSupplier;
import com.winllc.pki.ra.service.validators.ServerEntryValidator;
import com.winllc.ra.integration.ca.SubjectAltName;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.naming.InvalidNameException;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.winllc.pki.ra.constants.ServerSettingRequired.ENTITY_DIRECTORY_LDAP_SERVERBASEDN;

@RestController
@RequestMapping("/api/serverEntry")
public class ServerEntryService extends DataPagedService<ServerEntry, ServerEntryForm, ServerEntryRepository> {

    private static final Logger log = LogManager.getLogger(ServerEntryService.class);

    private final ServerEntryRepository serverEntryRepository;
    private final AccountRepository accountRepository;
    //todo replace with OIDCProviderService
    private final KeycloakOIDCProviderConnection oidcProviderConnection;
    private final PocEntryRepository pocEntryRepository;
    private final EntityDirectoryService entityDirectoryService;
    private final ServerEntryValidator serverEntryValidator;
    private final AuthCredentialService authCredentialService;
    private final DomainRepository domainRepository;
    private final ServerSettingsService serverSettingsService;

    public ServerEntryService(ApplicationContext context,
                              ServerEntryRepository serverEntryRepository, AccountRepository accountRepository,
                              KeycloakOIDCProviderConnection oidcProviderConnection, PocEntryRepository pocEntryRepository,
                              EntityDirectoryService entityDirectoryService, ServerEntryValidator serverEntryValidator,
                              AuthCredentialService authCredentialService, DomainRepository domainRepository, ServerSettingsService serverSettingsService) {
        super(context, ServerEntry.class, serverEntryRepository);
        this.serverEntryRepository = serverEntryRepository;
        this.accountRepository = accountRepository;
        this.oidcProviderConnection = oidcProviderConnection;
        this.pocEntryRepository = pocEntryRepository;
        this.entityDirectoryService = entityDirectoryService;
        this.serverEntryValidator = serverEntryValidator;
        this.authCredentialService = authCredentialService;
        this.domainRepository = domainRepository;
        this.serverSettingsService = serverSettingsService;
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
    public Long createServerEntry(@Valid @RequestBody ServerEntryForm form, Authentication authentication)
            throws Exception {
        ServerEntryForm add = add(form, authentication);
        return add.getId();
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

        if (!CollectionUtils.isEmpty(sans)) {
            List<String> dns = sans.stream()
                    .map(s -> s.getValue())
                    .collect(Collectors.toList());

            serverEntry.setAlternateDnsValues(dns);
        } else {
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
        List<PocEntry> pocs = pocEntryRepository.findAllByServerEntry(serverEntry);

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

        List<PocEntry> existing = pocEntryRepository.findAllByServerEntry(serverEntry);

        List<PocEntry> existingNotINPocs = existing.stream()
                .filter(p -> !pocs.contains(p.getId())).toList();

        for (Long poc : pocs) {

            Optional<PocEntry> optionalPocEntry = pocEntryRepository.findById(poc);
            if (optionalPocEntry.isPresent()) {
                PocEntry pocEntry = optionalPocEntry.get();
                pocEntry.setServerEntry(serverEntry);
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




    @Override
    protected void delete(Long id, Authentication authentication) throws RAObjectNotFoundException {
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

    @Override
    public ServerEntryForm entityToForm(ServerEntry entity) {
        Hibernate.initialize(entity.getAlternateDnsValues());
        return new ServerEntryForm(entity);
    }

    @Override
    protected ServerEntry formToEntity(ServerEntryForm form, Authentication authentication) throws RAObjectNotFoundException {
        Account account = accountRepository.findById(form.getAccountId()).orElseThrow(() -> new RAObjectNotFoundException(form));

        //Don't proceed if FQDN already exists under account
        Optional<ServerEntry> optionalExisting = serverEntryRepository.findDistinctByFqdnEqualsAndAccountEquals(form.getFqdn(), account);
        if (optionalExisting.isPresent()) {
            return optionalExisting.get();
        }

        String fqdn = form.getFqdn();

        if (form.getDomainId() != null) {
            Optional<Domain> optionalDomain = domainRepository.findById(form.getDomainId());
            if (optionalDomain.isPresent()) {
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

        Optional<String> serverBaseDnOptional = serverSettingsService.getServerSettingValue(ENTITY_DIRECTORY_LDAP_SERVERBASEDN);
        String baseDn = null;
        if (serverBaseDnOptional.isPresent()) {
            baseDn = serverBaseDnOptional.get();
        }

        entry.buildDn(baseDn);

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

        return finalEntry;
    }

    @Override
    protected ServerEntry combine(ServerEntry original, ServerEntry updated, Authentication authentication) {

        original.setAlternateDnsValues(updated.getAlternateDnsValues());
        original.setOpenidClientRedirectUrl(updated.getOpenidClientRedirectUrl());

        List<String> alternateDnsValues = new ArrayList<>(original.getAlternateDnsValues());
        original.setAlternateDnsValues(alternateDnsValues);

        original = serverEntryRepository.save(original);

        SystemActionRunner.build(context)
                .createAuditRecord(AuditRecordType.SERVER_ENTRY_UPDATED, original)
                .createNotificationForAccountPocs(Notification.buildNew()
                        .addMessage("Server Entry updated: " + original.getFqdn()), original.getAccount())
                .sendNotification()
                .execute();

        //apply attributes to external directory
        entityDirectoryService.applyServerEntryToDirectory(original);
        return original;
    }

    @Override
    public List<Predicate> buildFilter(Map<String, String> allRequestParams, Root<ServerEntry> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        String search = allRequestParams.get("search");
        if (StringUtils.isNotBlank(search)) {
            String finalText = search;
            if (!search.contains("%")) {
                finalText = "%" + search + "%";
            }
            Predicate fqdnLike = cb.like(root.get("fqdn"), finalText);
            return Collections.singletonList(fqdnLike);
        }
        return null;
    }


    public Specification<ServerEntry> buildSearch(Map<String, String> allRequestParams) {
        /*
         Specification<ServerEntry> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

                Join<ServerEntry, Account> pocs = root.join("pocs");
                Expression<String> exp = pocs.get("email");

                //Predicate joinPredicate = exp.in(onlyPocs);

                //predicates.add(joinPredicate);


        };

         */
        return (root, query, cb) -> {
            List<Predicate> list = new ArrayList<Predicate>();
            query.distinct(true);
            Root<ServerEntry> fromUpdates = query.from(ServerEntry.class);
            Join<ServerEntry, Account> details = fromUpdates.join("account");
            Join<Account, PocEntry> associate = details.join("pocs");

            list.add(cb.equal(associate.get("email"), "test3@test.com"));

            Predicate[] p = new Predicate[list.size()];
            return cb.and(list.toArray(p));
        };
    }
}
