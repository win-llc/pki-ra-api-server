package com.winllc.pki.ra.service.external.beans;

import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.pki.ra.service.AccountRequestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.util.CollectionUtils;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapName;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

public class DirectoryServerEntity {

    private static final Logger log = LogManager.getLogger(DirectoryServerEntity.class);

    private static String certificateAttribute = "userCertificate";
    //todo externalize
    private static int certMaxListSize = 10;
    
    private boolean exists = false;

    private final ServerEntry serverEntry;
    private final Name dn;
    private final String fqdn;
    private final LdapTemplate ldapTemplate;

    private Map<String, Attribute> currentMap;
    private List<X509Certificate> certificates;

    public DirectoryServerEntity(ServerEntry serverEntry, LdapTemplate ldapTemplate) throws InvalidNameException {
        this.serverEntry = serverEntry;
        this.dn = new LdapName(serverEntry.getDistinguishedName());
        this.fqdn = serverEntry.getFqdn();
        this.ldapTemplate = ldapTemplate;
    }

    public void sync(){
        createIfDoesNotExistAndUpdate();
    }


    public void saveAttribute(String name, Object value){
        createIfDoesNotExistAndUpdate();
        Attribute attribute = new BasicAttribute(name, value);
        saveAttributeInternal(attribute);
    }

    public void saveAttributes(Map<String, Object> attributes){
        createIfDoesNotExistAndUpdate();

        if(!CollectionUtils.isEmpty(attributes)){
            attributes.forEach((k,v) -> {
                Attribute attribute = new BasicAttribute(k, v);
                saveAttributeInternal(attribute);
            });
        }
    }

    public Map<String, Object> getCurrentAttributes(){
        return ldapTemplate.lookup(dn, new ServerEntryAttributeMapper());
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
        }
        saveAttributes(attributes);
    }

    private void saveAttributeInternal(Attribute attribute){

        ModificationItem modificationItem;
        if(currentMap.containsKey(attribute.getID())){
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

        Map<String, Attribute> lookup = ldapTemplate.lookup(dn, new AttributesMapper<Map<String, Attribute>>() {
                    @Override
                    public Map<String, Attribute> mapFromAttributes(Attributes attributes) throws NamingException {
                        Map<String, Attribute> map = new HashMap<>();
                        Iterator<String> keyIterator = attributes.getIDs().asIterator();
                        while (keyIterator.hasNext()) {
                            String key = keyIterator.next();
                            Attribute attribute = attributes.get(key);
                            map.put(key, attribute);
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

    public List<X509Certificate> getCertificates(){
        syncCertificates();
        return this.certificates;
    }

    public Optional<X509Certificate> getLatestCertificate(){
        List<X509Certificate> list = getCertificates();
        if(list.size() > 0){
            return Optional.of(list.get(0));
        }else{
            return Optional.empty();
        }
    }

    //todo test
    public void addCertificate(X509Certificate certificate){
        BasicAttribute certAttribute = new BasicAttribute(certificateAttribute);

        syncCertificates();

        List<X509Certificate> certsToAdd;
        if(this.certificates.size() >= certMaxListSize){
            List<X509Certificate> stubbedList = this.certificates.subList(0, certMaxListSize - 1);
            certsToAdd = new ArrayList<>(stubbedList);
        }else{
            certsToAdd = this.certificates;
        }
        certsToAdd.add(certificate);

        List<byte[]> encodedCerts = certsToAdd.stream()
                .map(cert -> {
                    try {
                        return cert.getEncoded();
                    } catch (CertificateEncodingException e) {
                        log.error("Could not encode cert", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for(byte[] encoded : encodedCerts){
            certAttribute.add(encoded);
        }

        saveAttributeInternal(certAttribute);
    }

    //todo finish this
    private void syncCertificates(){
        updateCurrent();

        Attribute certAttribute = this.currentMap.get(certificateAttribute);

        if(certAttribute != null){
            List<CertWrapper> certList = new ArrayList<>();
            for (int i = 0; i < certAttribute.size(); i++) {
                try {
                    byte[] cert = (byte[]) certAttribute.get(i);

                    X509Certificate x509Cert = (X509Certificate) CertificateFactory
                            .getInstance("X.509").generateCertificate(new ByteArrayInputStream(cert));
                    certList.add(new CertWrapper(x509Cert));
                }catch (Exception e){
                    log.error("Could not get cert", e);
                }
            }
            this.certificates = certList.stream()
                    .sorted()
                    .map(w -> w.getCertificate())
                    .collect(Collectors.toList());
        }else{
            log.info("No certificate attribute");
            this.certificates = new ArrayList<>();
        }
    }

    private static class CertWrapper implements Comparable<CertWrapper> {

        X509Certificate certificate;
        Date notAfter;
        Date notBefore;

        public CertWrapper(X509Certificate certificate) {
            this.certificate = certificate;
            this.notAfter = certificate.getNotAfter();
            this.notBefore = certificate.getNotBefore();
        }

        public X509Certificate getCertificate() {
            return certificate;
        }

        @Override
        public int compareTo(CertWrapper o) {
            return o.notBefore.compareTo(this.notBefore);
        }
    }

    private static class ServerEntryAttributeMapper implements AttributesMapper<Map<String, Object>> {

        @Override
        public Map<String, Object> mapFromAttributes(Attributes attributes) throws NamingException {
            Map<String, Object> map = new HashMap<>();
            NamingEnumeration<String> ids = attributes.getIDs();
            while(ids.hasMore()){
                String id = ids.next();
                map.put(id, attributes.get(id).get());
            }

            return map;
        }
    }

}
