package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.ServerSettings;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.ServerSettingsRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/settings")
public class ServerSettingsService {

    private static final Logger log = LogManager.getLogger(ServerEntryService.class);

    @Autowired
    private ServerSettingsRepository repository;

    private String[] defaultProperties = new String[]{
            "emailServer", "emailServerPort", "emailFromAddress",
            "openIdConnectEnabled", "openIdConnectServerBaseUrl", "openIdConnectRealm", "openIdConnectClientId",
            "openIdConnectClientSecret", "openIdConnectClientUserName", "openIdConnectClientPassword", "openIdConnectClientScope"
    };

    @PostConstruct
    private void postConstruct(){
        for(String defaultProperty : defaultProperties){
            addSetting(new ServerSettings(defaultProperty));
        }
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<ServerSettings> findAll(){
        return repository.findAll();
    }

    @PostMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    public ServerSettings updateSettings(@RequestBody ServerSettings serverSettings){
        ServerSettings updated = updateSetting(serverSettings);
        return updated;
    }

    @PostMapping("/updateAll")
    @ResponseStatus(HttpStatus.OK)
    public List<ServerSettings> updateAllSettings(@RequestBody List<ServerSettings> settingsList){
        List<ServerSettings> updatedList = new ArrayList<>();
        for(ServerSettings settings : settingsList){
            updatedList.add(updateSetting(settings));
        }
        return updatedList;
    }

    @GetMapping("/getByName/{name}")
    @ResponseStatus(HttpStatus.OK)
    public ServerSettings getSetting(@PathVariable String name) throws RAObjectNotFoundException {
        Optional<ServerSettings> distinctByPropertyEquals = repository.findDistinctByPropertyEquals(name);
        if(distinctByPropertyEquals.isPresent()){
            return distinctByPropertyEquals.get();
        }else{
            throw new RAObjectNotFoundException(ServerSettings.class, name);
        }
    }

    private void addSetting(ServerSettings serverSettings){
        Optional<ServerSettings> optionalSetting = repository.findDistinctByPropertyEquals(serverSettings.getProperty());
        if(!optionalSetting.isPresent()){
            repository.save(serverSettings);
        }else{
            log.debug("Setting not added, already exists in DB");
        }
    }

    private ServerSettings updateSetting(ServerSettings serverSettings){
        Optional<ServerSettings> optionalSetting = repository.findDistinctByPropertyEquals(serverSettings.getProperty());
        if(optionalSetting.isPresent()){
            ServerSettings settings = optionalSetting.get();
            settings.setValue(serverSettings.getValue());
            serverSettings = repository.save(settings);
        }else{
            log.debug("Setting not updated, does not exist");
        }
        return serverSettings;
    }
}
