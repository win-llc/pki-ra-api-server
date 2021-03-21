package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.ServerSettingsGroup;
import com.winllc.pki.ra.constants.ServerSettingRequired;
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
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/settings")
public class ServerSettingsService {

    private static final Logger log = LogManager.getLogger(ServerEntryService.class);

    private final ServerSettingsRepository repository;

    public ServerSettingsService(ServerSettingsRepository repository) {
        this.repository = repository;
    }


    @PostConstruct
    private void postConstruct(){
        Stream.of(ServerSettingRequired.values())
                .forEach(s -> addSetting(new ServerSettings(s.getSettingName())));
    }

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<ServerSettingsGroup> findAll(){

        return buildGroupList();
    }

    @PostMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    public ServerSettings updateSettings(@RequestBody ServerSettings serverSettings){
        ServerSettings updated = updateSetting(serverSettings);
        return updated;
    }

    @PostMapping("/updateAll")
    @ResponseStatus(HttpStatus.OK)
    public List<ServerSettingsGroup> updateAllSettings(@RequestBody List<ServerSettings> settingsList){
        List<ServerSettings> updatedList = new ArrayList<>();
        for(ServerSettings settings : settingsList){
            updatedList.add(updateSetting(settings));
        }
        return buildGroupList();
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

    public Optional<String> getServerSettingValue(ServerSettingRequired settingRequired){
        Optional<ServerSettings> optionalServerSettings = repository.findDistinctByPropertyEquals(settingRequired.getSettingName());
        if(optionalServerSettings.isPresent()){
            return Optional.of(optionalServerSettings.get().getValue());
        }else{
            return Optional.empty();
        }
    }

    private List<ServerSettingsGroup> buildGroupList(){
        List<ServerSettingsGroup> groups = new ArrayList<>();
        for(String groupName : ServerSettingRequired.getGroupNames()){
            ServerSettingsGroup group = new ServerSettingsGroup(groupName);

            List<ServerSettingRequired> byGroupName = ServerSettingRequired.getByGroupName(groupName);
            for(ServerSettingRequired settingRequired : byGroupName){
                Optional<ServerSettings> optionalSetting = repository.findDistinctByPropertyEquals(settingRequired.getSettingName());
                if(optionalSetting.isPresent()){
                    ServerSettings setting = optionalSetting.get();
                    setting.setGroupName(settingRequired.getSettingGroupName());
                    group.getRequiredSettings().add(setting);
                }else{
                    ServerSettings settings = new ServerSettings(settingRequired.getSettingName());
                    settings.setGroupName(settingRequired.getSettingGroupName());
                }
            }
            groups.add(group);
        }
        return groups;
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
