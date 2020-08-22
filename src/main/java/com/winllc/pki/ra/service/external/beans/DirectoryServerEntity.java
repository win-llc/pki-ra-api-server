package com.winllc.pki.ra.service.external.beans;

import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.service.AccountRequestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.util.CollectionUtils;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;

public class DirectoryServerEntity {

    private static final Logger log = LogManager.getLogger(DirectoryServerEntity.class);

    private boolean exists = false;

    private ServerEntry serverEntry;
    private final Name dn;
    private final String fqdn;
    private final LdapTemplate ldapTemplate;

    private Map<String, Object> currentMap;

    public DirectoryServerEntity(ServerEntry serverEntry, LdapTemplate ldapTemplate) {
        this.serverEntry = serverEntry;
        this.dn = serverEntry.getDn();
        this.fqdn = serverEntry.getFqdn();
        this.ldapTemplate = ldapTemplate;
    }

    //todo get schema

    public void saveAttribute(String name, Object value){
        createIfDoesNotExistAndUpdate();
        saveAttributeInternal(name, value);
    }

    public void saveAttributes(Map<String, Object> attributes){
        createIfDoesNotExistAndUpdate();

        if(!CollectionUtils.isEmpty(attributes)){
            attributes.forEach((k,v) -> {
                saveAttributeInternal(k, v);
            });
        }
    }

    public void overwriteAttributes(Map<String, Object> attributes){
        if(exists()){
            updateCurrent();

            List<ModificationItem> deleteItems = new ArrayList<>();
            for(String key : currentMap.keySet()){
                if(!key.equalsIgnoreCase("cn") && !key.equalsIgnoreCase("objectclass")) {
                    deleteItems.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(key)));
                }
            }
            ldapTemplate.modifyAttributes(dn, deleteItems.toArray(new ModificationItem[0]));
        }
        saveAttributes(attributes);
    }

    private void saveAttributeInternal(String name, Object value){

        Attribute attribute = new BasicAttribute(name, value);
        ModificationItem modificationItem;
        if(currentMap.containsKey(name)){
            modificationItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attribute);
        }else{
            modificationItem = new ModificationItem(DirContext.ADD_ATTRIBUTE, attribute);
        }
        try {
            ldapTemplate.modifyAttributes(dn, new ModificationItem[]{modificationItem});
        }catch (Exception e){
            log.error("Could not modify attribute", e);
        }finally {
            updateCurrent();
        }
    }

    public boolean exists(){
        try {
            Object obj = ldapTemplate.lookup(dn);
            exists = obj != null;
        }catch (NameNotFoundException e){
            exists = false;
        }
        return exists;
    }

    public void delete(){
        if(exists()){
            ldapTemplate.unbind(dn);
        }
    }

    private void createIfDoesNotExistAndUpdate(){
        if(!exists()){
            //todo bind basic attributes
            ldapTemplate.create(serverEntry);
            //ldapTemplate.bind(dn, null, buildBasicAttributes());
        }
        updateCurrent();
    }

    private void updateCurrent(){
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        searchControls.setCountLimit(1);

        Map<String, Object> lookup = ldapTemplate.lookup(dn, new AttributesMapper<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> mapFromAttributes(Attributes attributes) throws NamingException {
                        Map<String, Object> map = new HashMap<>();
                        Iterator<String> keyIterator = attributes.getIDs().asIterator();
                        while (keyIterator.hasNext()) {
                            String key = keyIterator.next();
                            Attribute attribute = attributes.get(key);
                            Object value = attribute.get();
                            map.put(key, value);
                        }
                        return map;
                    }
                }
        );

        if(!CollectionUtils.isEmpty(lookup)){
            this.currentMap = lookup;
        }
    }

    private Attributes buildBasicAttributes(){
        Attributes attrs = new BasicAttributes();
        BasicAttribute ocAttr = new BasicAttribute("objectclass");
        ocAttr.add("top");
        ocAttr.add("untypedObject");
        attrs.put(ocAttr);
        attrs.put(new BasicAttribute("cn", fqdn));
        return attrs;
    }

}
