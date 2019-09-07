package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertificateAuthoritySettings;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.ExternalAccountProviderSettings;
import com.winllc.pki.ra.acme.AcmeServerConnection;
import com.winllc.pki.ra.acme.AcmeServerService;
import com.winllc.pki.ra.acme.AcmeServerServiceImpl;
import com.winllc.pki.ra.domain.AcmeServerConnectionInfo;
import com.winllc.pki.ra.repository.AcmeServerConnectionInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/acmeServerManagement")
public class AcmeServerManagementService {

    @Autowired
    private AcmeServerConnectionInfoRepository connectionInfoRepository;

    private Map<String, AcmeServerServiceImpl> services;

    @PostConstruct
    private void postConstruct(){
        services = new HashMap<>();

        for(AcmeServerConnectionInfo info : connectionInfoRepository.findAll()){
            load(info);
        }
    }

    private void load(AcmeServerConnectionInfo connectionInfo){
        AcmeServerConnection connection = new AcmeServerConnection(connectionInfo);
        AcmeServerServiceImpl serverService = new AcmeServerServiceImpl(connection);
        services.put(serverService.getName(), serverService);
    }

    @PostMapping("/save")
    public void save(@RequestBody AcmeServerConnectionInfo connectionInfo){
        connectionInfo = connectionInfoRepository.save(connectionInfo);
        load(connectionInfo);
    }

    @PostMapping("/saveDirectorySettings/{connectionName}")
    public void saveDirectorySettings(@PathVariable String connectionName, @RequestBody DirectoryDataSettings directoryDataSettings) {
        AcmeServerService acmeServerService = services.get(connectionName);
        acmeServerService.saveDirectorySettings(directoryDataSettings);
    }

    public DirectoryDataSettings getDirectorySettingsByName(String name) {
        return null;
    }

    public List<DirectoryDataSettings> getAllDirectorySettings() {
        return null;
    }

    public void deleteDirectorySettings(String name) {

    }

    public void saveCertificateAuthoritySettings(CertificateAuthoritySettings directoryDataSettings) {

    }

    public CertificateAuthoritySettings getCertificateAuthoritySettingsByName(String name) {
        return null;
    }

    public List<CertificateAuthoritySettings> getAllCertificateAuthoritySettings() {
        return null;
    }

    public void deleteCertificateAuthoritySettings(String name) {

    }

    public void saveExternalAccountProviderSettings(ExternalAccountProviderSettings directoryDataSettings) {

    }

    public ExternalAccountProviderSettings getExternalAccountProviderSettingsByName(String name) {
        return null;
    }

    public List<ExternalAccountProviderSettings> getAllExternalAccountProviderSettings() {
        return null;
    }

    public void deleteExternalAccountProviderSettings(String name) {

    }
}
