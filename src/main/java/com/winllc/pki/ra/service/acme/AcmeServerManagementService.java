package com.winllc.pki.ra.service.acme;

import com.winllc.acme.common.domain.AcmeServerConnectionInfo;
import com.winllc.acme.common.repository.AcmeServerConnectionInfoRepository;
import com.winllc.pki.ra.endpoint.acme.AcmeServerConnection;
import com.winllc.pki.ra.endpoint.acme.AcmeServerService;
import com.winllc.pki.ra.endpoint.acme.AcmeServerServiceImpl;
import com.winllc.pki.ra.service.DataService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class  AcmeServerManagementService<F> implements DataService<F> {

    protected final String defaultConnectionName = "winllc";
    private final String winraAcmeServerUrl;
    private final String winraAcmeServerName;
    private final AcmeServerConnectionInfoRepository connectionInfoRepository;

    protected static Map<String, AcmeServerServiceImpl> services;

    public AcmeServerManagementService(String winraAcmeServerUrl, String winraAcmeServerName,
                                       AcmeServerConnectionInfoRepository connectionInfoRepository) {
        this.winraAcmeServerUrl = winraAcmeServerUrl;
        this.winraAcmeServerName = winraAcmeServerName;
        this.connectionInfoRepository = connectionInfoRepository;
    }

    @PostConstruct
    private void postConstruct(){
        services = new ConcurrentHashMap<>();

        for(AcmeServerConnectionInfo info : connectionInfoRepository.findAll()){
            load(info, false);
        }

        //load default, if exists
        if(StringUtils.isNoneBlank(winraAcmeServerName, winraAcmeServerUrl)) {
            AcmeServerConnectionInfo info = new AcmeServerConnectionInfo(winraAcmeServerName, winraAcmeServerUrl);
            load(info,true);
        }
    }

    private void load(AcmeServerConnectionInfo connectionInfo, boolean create){
        if(create){
            AcmeServerConnectionInfo info = connectionInfoRepository.findByName(connectionInfo.getName());
            if(info == null){
                connectionInfo = connectionInfoRepository.save(connectionInfo);
            }else{
                connectionInfo = info;
            }
        }
        AcmeServerConnection connection = new AcmeServerConnection(connectionInfo);
        AcmeServerServiceImpl serverService = new AcmeServerServiceImpl(connection);
        services.put(serverService.getName(), serverService);
    }

    public Optional<AcmeServerService> getAcmeServerServiceByName(String name){
        if(services.containsKey(name)){
            return Optional.of(services.get(name));
        }else{
            return Optional.empty();
        }
    }
}
