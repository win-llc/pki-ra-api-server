package com.winllc.pki.ra.beans;

import com.winllc.pki.ra.domain.ServerSettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServerSettingsGroup {

    private String settingsGroupName;
    private List<ServerSettings> requiredSettings;

    public ServerSettingsGroup(String groupName){
        this.settingsGroupName = groupName;
    }

    public String getSettingsGroupName() {
        return settingsGroupName;
    }

    public void setSettingsGroupName(String settingsGroupName) {
        this.settingsGroupName = settingsGroupName;
    }

    public List<ServerSettings> getRequiredSettings() {
        if(requiredSettings == null) requiredSettings = new ArrayList<>();
        return requiredSettings;
    }

    public void setRequiredSettings(List<ServerSettings> requiredSettings) {
        this.requiredSettings = requiredSettings;
    }
}
