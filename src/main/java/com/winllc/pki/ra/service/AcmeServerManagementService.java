package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.pki.ra.acme.AcmeServerConnection;
import com.winllc.pki.ra.acme.AcmeServerService;
import com.winllc.pki.ra.acme.AcmeServerServiceImpl;
import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import com.winllc.pki.ra.repository.AcmeServerConnectionInfoRepository;
import com.winllc.pki.ra.security.RAUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void save(@RequestBody AcmeServerConnectionInfo connectionInfo){
        connectionInfo = connectionInfoRepository.save(connectionInfo);
        load(connectionInfo);
    }

    @GetMapping("/getAcmeServerConnectionInfoByName/{name}")
    public AcmeServerConnectionInfo getAcmeServerConnectionInfoByName(@PathVariable String name){
        return connectionInfoRepository.findByName(name);
    }

    @GetMapping("/getAllAcmeServerConnectionInfo")
    public List<AcmeServerConnectionInfo> getAllAcmeServerConnectionInfo(@AuthenticationPrincipal RAUser raUser){
        return connectionInfoRepository.findAll();
    }

    @PostMapping("{connectionName}/saveDirectorySettings")
    public void saveDirectorySettings(@PathVariable String connectionName, @RequestBody DirectoryDataSettings directoryDataSettings) {
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

        acmeServerService.saveDirectorySettings(directoryDataSettings);
    }

    @GetMapping("{connectionName}/getDirectorySettingsByName/{name}")
    public DirectoryDataSettings getDirectorySettingsByName(@PathVariable String connectionName, @PathVariable String name) {
        AcmeServerService acmeServerService = services.get(connectionName);
        return acmeServerService.getDirectorySettingsByName(name);
    }

    @GetMapping("{connectionName}/getAllDirectorySettings")
    public List<DirectoryDataSettings> getAllDirectorySettings(@PathVariable String connectionName, @AuthenticationPrincipal RAUser raUser) {
        log.info("RAUser: "+raUser.getUsername());
        AcmeServerService acmeServerService = services.get(connectionName);
        return acmeServerService.getAllDirectorySettings();
    }

    @DeleteMapping("{connectionName}/deleteDirectorySettings/{name}")
    public void deleteDirectorySettings(@PathVariable String connectionName, @PathVariable String name) {
        AcmeServerService acmeServerService = services.get(connectionName);
        acmeServerService.deleteDirectorySettings(name);
    }

    @PostMapping("{connectionName}/saveCertificateAuthoritySettings")
    public void saveCertificateAuthoritySettings(@PathVariable String connectionName, @RequestBody CertificateAuthoritySettings certificateAuthoritySettings) {
        AcmeServerService acmeServerService = services.get(connectionName);
        acmeServerService.saveCertificateAuthoritySettings(certificateAuthoritySettings);
    }

    @GetMapping("{connectionName}/getCertificateAuthoritySettingsByName/{name}")
    public CertificateAuthoritySettings getCertificateAuthoritySettingsByName(@PathVariable String connectionName, @PathVariable String name) {
        AcmeServerService acmeServerService = services.get(connectionName);
        return acmeServerService.getCertificateAuthoritySettingsByName(name);
    }

    @GetMapping("{connectionName}/getAllCertificateAuthoritySettings")
    public List<CertificateAuthoritySettings> getAllCertificateAuthoritySettings(@PathVariable String connectionName) {
        AcmeServerService acmeServerService = services.get(connectionName);
        return acmeServerService.getAllCertificateAuthoritySettings();
    }

    @DeleteMapping("{connectionName}/deleteCertificateAuthoritySettings/{name}")
    public void deleteCertificateAuthoritySettings(@PathVariable String connectionName, @PathVariable String name) {
        AcmeServerService acmeServerService = services.get(connectionName);
        acmeServerService.deleteCertificateAuthoritySettings(name);
    }

    @PostMapping("{connectionName}/saveExternalAccountProviderSettings")
    public void saveExternalAccountProviderSettings(@PathVariable String connectionName, @RequestBody ExternalAccountProviderSettings externalAccountProviderSettings) {
        AcmeServerService acmeServerService = services.get(connectionName);
        acmeServerService.saveExternalAccountProviderSettings(externalAccountProviderSettings);
    }

    @GetMapping("{connectionName}/getExternalAccountProviderSettingsByName/{name}")
    public ExternalAccountProviderSettings getExternalAccountProviderSettingsByName(@PathVariable String connectionName, @PathVariable String name) {
        AcmeServerService acmeServerService = services.get(connectionName);
        return acmeServerService.getExternalAccountProviderSettingsByName(name);
    }

    @GetMapping("{connectionName}/getAllExternalAccountProviderSettings")
    public List<ExternalAccountProviderSettings> getAllExternalAccountProviderSettings(@PathVariable String connectionName) {
        AcmeServerService acmeServerService = services.get(connectionName);
        return acmeServerService.getAllExternalAccountProviderSettings();
    }

    @DeleteMapping("{connectionName}/deleteExternalAccountProviderSettings/{name}")
    public void deleteExternalAccountProviderSettings(@PathVariable String connectionName, @PathVariable String name) {
        AcmeServerService acmeServerService = services.get(connectionName);
        acmeServerService.deleteExternalAccountProviderSettings(name);
    }


}
