package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.AcmeClientDetails;
import com.winllc.pki.ra.beans.OIDCClientDetails;
import com.winllc.pki.ra.beans.ServerEntryDockerDeploymentFile;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.beans.info.ServerEntryInfo;
import com.winllc.acme.common.constants.AuditRecordType;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.AuthCredential;
import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.ServerEntryRepository;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakOIDCProviderConnection;
import com.winllc.pki.ra.service.transaction.SystemActionRunner;
import com.winllc.pki.ra.service.transaction.ThrowingSupplier;
import org.hibernate.Hibernate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/serverEntry/oidc")
public class OIDCManagementService implements ApplicationContextAware {

    //todo replace with OIDCProviderService
    private final KeycloakOIDCProviderConnection oidcProviderConnection;
    private final ServerEntryRepository serverEntryRepository;
    private final AccountRepository accountRepository;
    private final ApplicationContext applicationContext;

    public OIDCManagementService(KeycloakOIDCProviderConnection oidcProviderConnection,
                                 ServerEntryRepository serverEntryRepository, AccountRepository accountRepository, ApplicationContext applicationContext) {
        this.oidcProviderConnection = oidcProviderConnection;
        this.serverEntryRepository = serverEntryRepository;
        this.accountRepository = accountRepository;
        this.applicationContext = applicationContext;
        setApplicationContext(applicationContext);
    }

    @GetMapping("/getDetails/{serverId}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public OIDCClientDetails getDetailsForServer(@PathVariable Long serverId) throws RAObjectNotFoundException {
        Optional<ServerEntry> optionalServer = serverEntryRepository.findById(serverId);
        if(optionalServer.isPresent()){
            ServerEntry serverEntry = optionalServer.get();
            OIDCClientDetails oidcClientDetails = oidcProviderConnection.getClient(serverEntry);

            return oidcClientDetails;
        }else{
            throw new RAObjectNotFoundException(ServerEntry.class, serverId);
        }

    }

    @PostMapping("/enableForOIDConnect")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public ServerEntryInfo enableForOIDConnect(@RequestBody ServerEntryForm form) throws RAException {
        //todo

        Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findById(form.getId());
        if(serverEntryOptional.isPresent()){
            ServerEntry serverEntry = serverEntryOptional.get();

            try {
                //todo generify

                SystemActionRunner runner = SystemActionRunner.build(this.applicationContext)
                        .createAuditRecord(AuditRecordType.OPENID_ENABLED, serverEntry);

                ServerEntry finalServerEntry = serverEntry;
                ThrowingSupplier<ServerEntry, Exception> action = () -> oidcProviderConnection.createClient(finalServerEntry);

                serverEntry = runner.execute(action);

                if(serverEntry != null){
                    serverEntry.setOpenidClientRedirectUrl(form.getOpenidClientRedirectUrl());
                    Hibernate.initialize(serverEntry.getAlternateDnsValues());
                    serverEntry = serverEntryRepository.save(serverEntry);

                    return entryToInfo(serverEntry);
                }else{
                    throw new RAException("Could not create OIDC client");
                }
            } catch (Exception e) {
                throw new RAException(e.getMessage());
            }
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    @PostMapping("/disableForOIDConnect")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public ServerEntryInfo disableForOIDConnect(@RequestBody ServerEntryForm form) throws Exception {

        Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findById(form.getId());

        if(optionalServerEntry.isPresent()){
            ServerEntry serverEntry = optionalServerEntry.get();

            SystemActionRunner runner = SystemActionRunner.build(this.applicationContext)
                    .createAuditRecord(AuditRecordType.OPENID_DISABLED, serverEntry);

            ServerEntry finalServerEntry = serverEntry;
            ThrowingSupplier<ServerEntry, Exception> action = () -> oidcProviderConnection.deleteClient(finalServerEntry);

            serverEntry = runner.execute(action);

            if(serverEntry != null){
                return entryToInfo(serverEntry);
            }else {
                throw new RAException("Did not delete the OIDC client");
            }
        }else{
            throw new RAObjectNotFoundException(form);
        }

    }

    @PostMapping("/buildDeploymentPackage")
    @ResponseStatus(HttpStatus.OK)
    public List<String> buildDeploymentPackage(@RequestBody ServerEntryForm form) throws RAObjectNotFoundException {

        Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findById(form.getId());
        if(serverEntryOptional.isPresent()) {
            ServerEntry serverEntry = serverEntryOptional.get();
            Optional<Account> optionalAccount = accountRepository.findById(serverEntry.getAccount().getId());
            if(optionalAccount.isPresent()){
                ServerEntryDockerDeploymentFile deploymentFile = buildDeploymentFile(serverEntry);
                return deploymentFile.buildContent();
            }else{
                throw new RAObjectNotFoundException(Account.class, serverEntry.getAccount().getId());
            }
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    private ServerEntryDockerDeploymentFile buildDeploymentFile(ServerEntry serverEntry) throws RAObjectNotFoundException {
        Optional<AuthCredential> optionalAuthCredential = serverEntry.getLatestAuthCredential();

        if(optionalAuthCredential.isPresent()) {
            AuthCredential authCredential = optionalAuthCredential.get();

            AcmeClientDetails acmeClientDetails = new AcmeClientDetails();
            acmeClientDetails.setAcmeEabHmacKeyValue(authCredential.getMacKeyBase64());
            acmeClientDetails.setAcmeKidValue(authCredential.getKeyIdentifier());
            //todo make dynamic
            acmeClientDetails.setAcmeServerValue("http://192.168.1.202:8181/acme/directory");

            OIDCClientDetails oidcClientDetails = oidcProviderConnection.getClient(serverEntry);

            ServerEntryDockerDeploymentFile dockerDeploymentFile = new ServerEntryDockerDeploymentFile();
            dockerDeploymentFile.setAcmeClientDetails(acmeClientDetails);
            dockerDeploymentFile.setOidcClientDetails(oidcClientDetails);
            //todo make dynamic
            //dockerDeploymentFile.setProxyAddressValue("http://192.168.1.13:8282/test");
            dockerDeploymentFile.setProxyAddressValue(serverEntry.getOpenidClientRedirectUrl());
            dockerDeploymentFile.setServerNameValue(serverEntry.getFqdn());
            return dockerDeploymentFile;
        }else{
            throw new RAObjectNotFoundException(AuthCredential.class, "For Server Entry: "+serverEntry.getId());
        }
    }

    private ServerEntryInfo entryToInfo(ServerEntry entry){
        Hibernate.initialize(entry.getAlternateDnsValues());
        return new ServerEntryInfo(entry);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}
