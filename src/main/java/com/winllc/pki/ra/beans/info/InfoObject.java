package com.winllc.pki.ra.beans.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.acme.common.domain.BaseEntity;
import com.winllc.acme.common.domain.UniqueEntity;

import java.lang.reflect.ParameterizedType;
import java.util.UUID;

public abstract class InfoObject<T extends BaseEntity> {

    private Class clazz;
    private Long id;
    private String objectUuid;
    private String objectClass;
    private String createdOn;

    protected InfoObject(T entity){
        this.id = entity.getId();
        clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

        objectClass = clazz.getCanonicalName();

        if(entity.getCreationDate() != null){
            //todo convert to common format
            createdOn = entity.getCreationDate().toString();
        }

        if(isUniqueEntityForm()){
            UniqueEntity ue = ((UniqueEntity) entity);
            if(ue.getUuid() != null) {
                objectUuid = ((UniqueEntity) entity).getUuid().toString();
            }
        }
    }

    @JsonIgnore
    public Class getFormObjectType(){
        return clazz;
    }

    @JsonIgnore
    public boolean isUniqueEntityForm() { return UniqueEntity.class.isAssignableFrom(getFormObjectType()); }

    protected InfoObject(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore
    public UUID convertUuid(){
        return UUID.fromString(objectUuid);
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getObjectUuid() {
        return objectUuid;
    }

    public void setObjectUuid(String objectUuid) {
        this.objectUuid = objectUuid;
    }

    protected void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }
}
