package com.winllc.pki.ra.ca;

import java.util.List;

public class ConnectionProperty {
    private String name;
    private String friendlyName;
    private String description;
    private String type;
    private List<String> options;

    public static ConnectionProperty build(){
        return new ConnectionProperty();
    }

    public ConnectionProperty addName(String name){
        setName(name);
        return this;
    }

    public ConnectionProperty addFriendlyName(String name){
        setFriendlyName(name);
        return this;
    }

    public ConnectionProperty addDescription(String description){
        setDescription(description);
        return this;
    }

    public ConnectionProperty addOptions(List<String> options){
        setOptions(options);
        return this;
    }

    public ConnectionProperty addType(String type){
        setType(type);
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}
