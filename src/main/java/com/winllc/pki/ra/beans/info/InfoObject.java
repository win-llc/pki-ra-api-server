package com.winllc.pki.ra.beans.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.winllc.acme.common.domain.BaseEntity;
import com.winllc.acme.common.domain.UniqueEntity;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.ParameterizedType;
import java.util.UUID;

@Getter
@Setter
public abstract class InfoObject<T extends BaseEntity> {

    private Class<T> clazz;
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
    public Class<T> getFormObjectType(){
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


}
