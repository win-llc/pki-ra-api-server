package com.winllc.pki.ra.beans;

import com.winllc.pki.ra.domain.ServerSettings;

import java.util.HashSet;
import java.util.Set;

public class ServerSettingsGroup {

    private String settingsGroupName;
    private Set<ServerSettings> requiredSettings;

    public ServerSettingsGroup(String groupName){
        this.settingsGroupName = groupName;
    }

    public String getSettingsGroupName() {
        return settingsGroupName;
    }

    public void setSettingsGroupName(String settingsGroupName) {
        this.settingsGroupName = settingsGroupName;
    }

    public Set<ServerSettings> getRequiredSettings() {
        if(requiredSettings == null) requiredSettings = new HashSet<>();
        return requiredSettings;
    }

    public void setRequiredSettings(Set<ServerSettings> requiredSettings) {
        this.requiredSettings = requiredSettings;
    }
}
