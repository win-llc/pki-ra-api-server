package com.winllc.pki.ra.constants;

public enum  AccountStatusType {
    APPROVED("approved"),
    REJECTED("rejected");

    private String value;

    AccountStatusType(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AccountStatusType valueToType(String value){
        for(AccountStatusType type : values()){
            if(type.getValue().equalsIgnoreCase(value)) return type;
        }
        throw new RuntimeException("No matching type for: "+value);
    }
}
