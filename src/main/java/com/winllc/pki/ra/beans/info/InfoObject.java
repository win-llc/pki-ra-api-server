package com.winllc.pki.ra.beans.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.pki.ra.domain.UniqueEntity;
import org.springframework.data.jpa.domain.AbstractPersistable;

import java.lang.reflect.ParameterizedType;
import java.util.UUID;

public abstract class InfoObject<T extends AbstractPersistable<Long>> {

    private Class clazz;
    private Long id;
    private String objectUuid;
    private String objectClass;

    protected InfoObject(T entity){
        this.id = entity.getId();
        clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

        objectClass = clazz.getCanonicalName();

        if(isUniqueEntityForm()){
            UniqueEntity ue = ((UniqueEntity) entity);
            if(ue.getUuid() != null) {
                objectUuid = ((UniqueEntity) entity).getUuid().toString();
            }
        }
    }

    public Class getFormObjectType(){
        return clazz;
    }

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
}
