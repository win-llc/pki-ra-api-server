package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.pki.ra.acme.AcmeServerConnection;
import com.winllc.pki.ra.acme.AcmeServerService;
import com.winllc.pki.ra.acme.AcmeServerServiceImpl;
import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.repository.AcmeServerConnectionInfoRepository;
import com.winllc.pki.ra.security.RAUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/acmeServerManagement")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AcmeServerManagementService {

    private static final Logger log = LogManager.getLogger(AcmeServerManagementService.class);

    @Autowired
    private AcmeServerConnectionInfoRepository connectionInfoRepository;

    private Map<String, AcmeServerServiceImpl> services;

    @PostConstruct
    private void postConstruct(){
        services = new HashMap<>();

        for(AcmeServerConnectionInfo info : connectionInfoRepository.findAll()){
            load(info);
        }

        //todo remove this
        AcmeServerConnectionInfo info = new AcmeServerConnectionInfo("winllc", "http://localhost:8181");
        load(info);
    }

    private void load(AcmeServerConnectionInfo connectionInfo){
        AcmeServerConnection connection = new AcmeServerConnection(connectionInfo);
        AcmeServerServiceImpl serverService = new AcmeServerServiceImpl(connection);
        services.put(serverService.getName(), serverService);
    }

    @PostMapping("/saveAcmeServerConnection")
    public ResponseEntity<?> save(@RequestBody AcmeServerConnectionInfo connectionInfo){
        connectionInfo = connectionInfoRepository.save(connectionInfo);
        load(connectionInfo);

        return ResponseEntity.ok(connectionInfo);
    }

    @GetMapping("/getAcmeServerConnectionInfoByName/{name}")
    public ResponseEntity<?> getAcmeServerConnectionInfoByName(@PathVariable String name){
        return ResponseEntity.ok(connectionInfoRepository.findByName(name));
    }

    @GetMapping("/getAcmeServerConnectionInfoById/{id}")
    public ResponseEntity<?> getAcmeServerConnectionInfoById(@PathVariable Long id){
        Optional<AcmeServerConnectionInfo> connectionInfoOptional = connectionInfoRepository.findById(id);
        if(connectionInfoOptional.isPresent()){
            return ResponseEntity.ok(connectionInfoOptional.get());
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getAllAcmeServerConnectionInfo")
    public ResponseEntity<?> getAllAcmeServerConnectionInfo(@AuthenticationPrincipal RAUser raUser){
        return ResponseEntity.ok(connectionInfoRepository.findAll());
    }

    @PostMapping("{connectionName}/saveDirectorySettings")
    public ResponseEntity<?> saveDirectorySettingsRest(@PathVariable String connectionName,
                                                   @RequestBody DirectoryDataSettings directoryDataSettings) throws AcmeConnectionException, IOException {
        directoryDataSettings = saveDirectorySettings(connectionName, directoryDataSettings);
        return ResponseEntity.ok(directoryDataSettings);
    }

    public DirectoryDataSettings saveDirectorySettings(String connectionName,
                                                       DirectoryDataSettings directoryDataSettings) throws AcmeConnectionException, IOException {
        AcmeServerService acmeServerService = services.get(connectionName);

        DirectoryDataSettings existingSettings = acmeServerService.getDirectorySettingsByName(directoryDataSettings.getName());
        if(existingSettings != null) {
            //Check if Terms of Service have been updated
            if ((StringUtils.isNotBlank(existingSettings.getMetaTermsOfService()) && StringUtils.isNotBlank(directoryDataSettings.getMetaTermsOfService())) &&
                    (!existingSettings.getMetaTermsOfService().equalsIgnoreCase(directoryDataSettings.getMetaTermsOfService()))){
                directoryDataSettings.updateTermsOfService(directoryDataSettings.getMetaTermsOfService());
            }else{
                //If new, set terms of service updated if terms of service included
                directoryDataSettings.updateTermsOfService(existingSettings.getMetaTermsOfService());
            }
        }

        DirectoryDataSettings settings = acmeServerService.saveDirectorySettings(directoryDataSettings);
        return settings;
    }

    @GetMapping("{connectionName}/getDirectorySettingsByName/{name}")
    public ResponseEntity<?> getDirectorySettingsByNameRest(@PathVariable String connectionName, @PathVariable String name) throws AcmeConnectionException {
        return ResponseEntity.ok(getDirectorySettingsByName(connectionName, name));
    }

    public DirectoryDataSettings getDirectorySettingsByName(String connectionName, String name) throws AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(connectionName);
        DirectoryDataSettings directorySettings = acmeServerService.getDirectorySettingsByName(name);
        return directorySettings;
    }

    @GetMapping("{connectionName}/getDirectorySettingsById/{id}")
    public ResponseEntity<?> getDirectorySettingsById(@PathVariable String connectionName, @PathVariable String id) throws AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(connectionName);
        DirectoryDataSettings directorySettings = acmeServerService.getDirectorySettingsById(id);
        return ResponseEntity.ok(directorySettings);
    }

    @GetMapping("{connectionName}/getAllDirectorySettings")
    public ResponseEntity<?> getAllDirectorySettings(@PathVariable String connectionName, @AuthenticationPrincipal RAUser raUser) throws AcmeConnectionException {
        log.info("RAUser: "+raUser.getUsername());
        AcmeServerService acmeServerService = services.get(connectionName);
        List<DirectoryDataSettings> allDirectorySettings = acmeServerService.getAllDirectorySettings();
        return ResponseEntity.ok(allDirectorySettings);
    }

    @DeleteMapping("{connectionName}/deleteDirectorySettings/{name}")
    public ResponseEntity<?> deleteDirectorySettings(@PathVariable String connectionName, @PathVariable String name) throws AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(connectionName);
        acmeServerService.deleteDirectorySettings(name);
        return ResponseEntity.ok().build();
    }

    @PostMapping("{connectionName}/saveCertificateAuthoritySettings")
    public ResponseEntity<?> saveCertificateAuthoritySettings(@PathVariable String connectionName,
                                                              @RequestBody CertificateAuthoritySettings certificateAuthoritySettings)
            throws AcmeConnectionException, IOException {
        AcmeServerService acmeServerService = services.get(connectionName);
        CertificateAuthoritySettings settings = acmeServerService.saveCertificateAuthoritySettings(certificateAuthoritySettings);
        return ResponseEntity.ok(settings);
    }

    @GetMapping("{connectionName}/getCertificateAuthoritySettingsByName/{name}")
    public ResponseEntity<?> getCertificateAuthoritySettingsByName(@PathVariable String connectionName, @PathVariable String name) throws AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(connectionName);
        CertificateAuthoritySettings authoritySettings = acmeServerService.getCertificateAuthoritySettingsByName(name);
        return ResponseEntity.ok(authoritySettings);
    }

    @GetMapping("{connectionName}/getCertificateAuthoritySettingsById/{id}")
    public ResponseEntity<?> getCertificateAuthoritySettingsById(@PathVariable String connectionName, @PathVariable String id) throws AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(connectionName);
        CertificateAuthoritySettings authoritySettings = acmeServerService.getCertificateAuthoritySettingsById(id);
        return ResponseEntity.ok(authoritySettings);
    }

    @GetMapping("{connectionName}/getAllCertificateAuthoritySettings")
    public ResponseEntity<?> getAllCertificateAuthoritySettings(@PathVariable String connectionName) throws AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(connectionName);
        List<CertificateAuthoritySettings> allCertificateAuthoritySettings = acmeServerService.getAllCertificateAuthoritySettings();
        return ResponseEntity.ok(allCertificateAuthoritySettings);
    }

    @DeleteMapping("{connectionName}/deleteCertificateAuthoritySettings/{name}")
    public ResponseEntity<?> deleteCertificateAuthoritySettings(@PathVariable String connectionName, @PathVariable String name) throws AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(connectionName);
        acmeServerService.deleteCertificateAuthoritySettings(name);
        return ResponseEntity.ok().build();
    }

    @PostMapping("{connectionName}/saveExternalAccountProviderSettings")
    public ResponseEntity<?> saveExternalAccountProviderSettings(@PathVariable String connectionName, @RequestBody ExternalAccountProviderSettings externalAccountProviderSettings) throws AcmeConnectionException, IOException {
        AcmeServerService acmeServerService = services.get(connectionName);
        ExternalAccountProviderSettings providerSettings = acmeServerService.saveExternalAccountProviderSettings(externalAccountProviderSettings);
        return ResponseEntity.ok(providerSettings);
    }

    @GetMapping("{connectionName}/getExternalAccountProviderSettingsByName/{name}")
    public ResponseEntity<?> getExternalAccountProviderSettingsByName(@PathVariable String connectionName, @PathVariable String name) throws AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(connectionName);
        ExternalAccountProviderSettings externalAccountProviderSettingsByName = acmeServerService.getExternalAccountProviderSettingsByName(name);
        return ResponseEntity.ok(externalAccountProviderSettingsByName);
    }

    @GetMapping("{connectionName}/getExternalAccountProviderSettingsById/{id}")
    public ResponseEntity<?> getExternalAccountProviderSettingsById(@PathVariable String connectionName, @PathVariable String id) throws AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(connectionName);
        ExternalAccountProviderSettings externalAccountProviderSettingsByName = acmeServerService.getExternalAccountProviderSettingsById(id);
        return ResponseEntity.ok(externalAccountProviderSettingsByName);
    }

    @GetMapping("{connectionName}/getAllExternalAccountProviderSettings")
    public ResponseEntity<?> getAllExternalAccountProviderSettings(@PathVariable String connectionName) throws AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(connectionName);
        List<ExternalAccountProviderSettings> allExternalAccountProviderSettings = acmeServerService.getAllExternalAccountProviderSettings();
        return ResponseEntity.ok(allExternalAccountProviderSettings);
    }

    @DeleteMapping("{connectionName}/deleteExternalAccountProviderSettings/{name}")
    public ResponseEntity<?> deleteExternalAccountProviderSettings(@PathVariable String connectionName, @PathVariable String name) throws AcmeConnectionException {
        AcmeServerService acmeServerService = services.get(connectionName);
        acmeServerService.deleteExternalAccountProviderSettings(name);
        return ResponseEntity.ok().build();
    }


}
