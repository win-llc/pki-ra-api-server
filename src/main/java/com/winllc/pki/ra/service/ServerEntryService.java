package com.winllc.pki.ra.service;

import com.nimbusds.jose.util.Base64;
import com.winllc.pki.ra.beans.AcmeClientDetails;
import com.winllc.pki.ra.beans.OIDCClientDetails;
import com.winllc.pki.ra.beans.ServerEntryDockerDeploymentFile;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.beans.info.ServerEntryInfo;
import com.winllc.pki.ra.beans.validator.ServerEntryFormValidator;
import com.winllc.pki.ra.beans.validator.ValidationResponse;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import com.winllc.pki.ra.repository.PocEntryRepository;
import com.winllc.pki.ra.repository.ServerEntryRepository;
import com.winllc.pki.ra.service.external.EntityDirectoryService;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakOIDCProviderConnection;
import com.winllc.pki.ra.service.transaction.SystemActionRunner;
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
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.security.cert.X509Certificate;
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
    private final AuditRecordService auditRecordService;

    public ServerEntryService(ApplicationContext context,
                              ServerEntryRepository serverEntryRepository, AccountRepository accountRepository,
                              KeycloakOIDCProviderConnection oidcProviderConnection, PocEntryRepository pocEntryRepository,
                              EntityDirectoryService entityDirectoryService, AuditRecordService auditRecordService) {
        super(context);
        this.serverEntryRepository = serverEntryRepository;
        this.accountRepository = accountRepository;
        this.oidcProviderConnection = oidcProviderConnection;
        this.pocEntryRepository = pocEntryRepository;
        this.entityDirectoryService = entityDirectoryService;
        this.auditRecordService = auditRecordService;
    }


    @GetMapping("/variableFields")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getAvailableVariableFields(){
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

        if(optionalAccount.isPresent()){
            Account account = optionalAccount.get();

            Set<DomainPolicy> accountDomainPolicies = account.getAccountDomainPolicies();

            Optional<Domain> optionalDomain = accountDomainPolicies.stream()
                    .map(p -> p.getTargetDomain())
                    .filter(d -> form.getFqdn().endsWith(d.getBase()))
                    .findAny();

            if(optionalDomain.isPresent()){
                Domain domain = optionalDomain.get();
                ServerEntry entry = ServerEntry.buildNew();
                entry.setAccount(account);
                entry.setDomainParent(domain);
                entry.setFqdn(form.getFqdn());
                entry.setHostname(form.getFqdn());

                if(!CollectionUtils.isEmpty(form.getAlternateDnsValues())){
                    entry.setAlternateDnsValues(form.getAlternateDnsValues());
                }

                entry = serverEntryRepository.save(entry);

                SystemActionRunner.build(context)
                        .createAuditRecord(AuditRecordType.SERVER_ENTRY_ADDED, entry)
                        .createNotificationForAccountPocs(Notification.buildNew()
                                .addMessage("Server Entry added: "+entry.getFqdn()), account)
                        .sendNotification()
                        .execute();

                //apply attributes to external directory
                entityDirectoryService.applyServerEntryToDirectory(entry);

                log.info("Created a Server Entry: "+entry);

                return entry.getId();
            }else{
                throw new RAObjectNotFoundException(Domain.class, form.getFqdn());
            }
        }else{
            throw new RAObjectNotFoundException(Account.class, form.getAccountId());
        }

    }

    @PostMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public ServerEntryInfo updateServerEntry(@Valid @RequestBody ServerEntryForm form) throws RAException {
        //todo update attributes in directory

        log.info("Is account linked: "+form.isAccountLinkedForm());

        Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findById(form.getId());
        if(optionalServerEntry.isPresent()){
            ServerEntry serverEntry = optionalServerEntry.get();

            ValidationResponse validationResponse = new ServerEntryFormValidator().validate(form, true);
            if(validationResponse.isValid()){
                serverEntry.setAlternateDnsValues(form.getAlternateDnsValues());
                serverEntry.setOpenidClientRedirectUrl(form.getOpenidClientRedirectUrl());

                List<String> alternateDnsValues = new ArrayList<>(serverEntry.getAlternateDnsValues());
                serverEntry.setAlternateDnsValues(alternateDnsValues);

                serverEntry = serverEntryRepository.save(serverEntry);

                SystemActionRunner.build(context)
                        .createAuditRecord(AuditRecordType.SERVER_ENTRY_UPDATED, serverEntry)
                        .createNotificationForAccountPocs(Notification.buildNew()
                                .addMessage("Server Entry updated: "+serverEntry.getFqdn()), serverEntry.getAccount())
                        .sendNotification()
                        .execute();

                //apply attributes to external directory
                entityDirectoryService.applyServerEntryToDirectory(serverEntry);

                return entryToInfo(serverEntry);
            }else{
                throw new RAException("Invalid Server Entry form");
            }
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    @GetMapping("/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public ServerEntryInfo getServerEntry(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<ServerEntry> entryOptional = serverEntryRepository.findById(id);

        if(entryOptional.isPresent()){
            ServerEntry entry = entryOptional.get();

            return entryToInfo(entry);
        }else{
            throw new RAObjectNotFoundException(ServerEntry.class, id);
        }
    }

    @GetMapping("/allByAccountId/{accountId}")
    @ResponseStatus(HttpStatus.OK)
    public List<ServerEntry> getAllServerEntriesForAccount(@PathVariable Long accountId){

        List<ServerEntry> allByAccountId = serverEntryRepository.findAllByAccountId(accountId);

        return allByAccountId;
    }

    @GetMapping("/allForUser")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<ServerEntryInfo> getAllServerEntriesForUser(@AuthenticationPrincipal UserDetails raUser) {
        List<PocEntry> pocEntries = pocEntryRepository.findAllByEmailEquals(raUser.getUsername());
        List<Account> allByAccountUsersContains = accountRepository.findAllByPocsIn(pocEntries);

        List<ServerEntryInfo> entries = new ArrayList<>();
        for(Account account : allByAccountUsersContains) {
            List<ServerEntryInfo> temp = serverEntryRepository.findAllByAccountId(account.getId())
                    .stream().map(i -> {
                        return entryToInfo(i);
                    })
                    .collect(Collectors.toList());
            entries.addAll(temp);
        }

        return entries;
    }



    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteServerEntry(@PathVariable Long id) throws RAException {

        Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findById(id);
        if(serverEntryOptional.isPresent()){
            ServerEntry serverEntry = serverEntryOptional.get();

            //if server entry is deleted, remove the OIDC client if it exists
            if(StringUtils.isNotBlank(serverEntry.getOpenidClientId())){
                oidcProviderConnection.deleteClient(serverEntry);
            }

            serverEntryRepository.deleteById(id);
            auditRecordService.save(AuditRecord.buildNew(AuditRecordType.SERVER_ENTRY_REMOVED, serverEntry));
        }else{
            throw new RAObjectNotFoundException(ServerEntry.class, id);
        }
    }

    @GetMapping("/calculateAttributes/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public Map<String, Object> getCalculatedAttributes(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findById(id);
        if(serverEntryOptional.isPresent()) {
            ServerEntry serverEntry = serverEntryOptional.get();

            Map<String, Object> attributeMap = entityDirectoryService.calculateAttributePolicyMapForServerEntry(serverEntry);

            return attributeMap;
        }else{
            throw new RAObjectNotFoundException(ServerEntry.class, id);
        }
    }

    private ServerEntryInfo entryToInfo(ServerEntry entry){
        Hibernate.initialize(entry.getAlternateDnsValues());
        return new ServerEntryInfo(entry);
    }


}
