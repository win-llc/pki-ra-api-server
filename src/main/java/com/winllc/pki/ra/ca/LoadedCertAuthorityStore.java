package com.winllc.pki.ra.ca;

import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.keystore.ApplicationKeystore;
import com.winllc.pki.ra.repository.CertAuthorityConnectionInfoRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;
import java.lang.reflect.Constructor;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LoadedCertAuthorityStore implements InitializingBean {


    private static final Logger log = LogManager.getLogger(LoadedCertAuthorityStore.class);

    private Map<String, CertAuthority> loadedCertAuthorities = new ConcurrentHashMap<>();

    private final CertAuthorityConnectionInfoRepository repository;
    private final ApplicationKeystore applicationKeystore;

    public LoadedCertAuthorityStore(ApplicationContext applicationContext){
        this.repository = applicationContext.getBean(CertAuthorityConnectionInfoRepository.class);
        this.applicationKeystore = applicationContext.getBean(ApplicationKeystore.class);
    }

    public void reload() {
        loadedCertAuthorities = new ConcurrentHashMap<>();
        for (String connectionInfoName : repository.findAllNames()) {
            loadCertAuthority(connectionInfoName);
        }
    }

    //Build CA object for use
    private void loadCertAuthority(String name) {
        Optional<CertAuthority> optionalCertAuthority = buildCertAuthority(name);
        if (optionalCertAuthority.isPresent()) {
            CertAuthority ca = optionalCertAuthority.get();

            loadedCertAuthorities.put(ca.getName(), ca);
        }
    }

    public CertAuthority getLoadedCertAuthority(String name){
        return loadedCertAuthorities.get(name);
    }

    public void addLoadedCertAuthority(CertAuthority ca){
        loadedCertAuthorities.put(ca.getName(), ca);
    }

    @Transactional
    public Optional<CertAuthority> buildCertAuthority(String connectionName) {
        Optional<CertAuthorityConnectionInfo> infoOptional = repository.findByName(connectionName);

        if (infoOptional.isPresent()) {
            CertAuthorityConnectionInfo info = infoOptional.get();

            try {
                Hibernate.initialize(info.getProperties());

                String certAuthorityClassName = info.getCertAuthorityClassName();
                Class<?> clazz = Class.forName(certAuthorityClassName);
                Constructor<?> ctor = clazz.getConstructor(CertAuthorityConnectionInfo.class, KeyStore.class, String.class);
                Object object = ctor.newInstance(new Object[] { info, applicationKeystore.getKeyStore(),
                        applicationKeystore.getKeystorePassword() });

                if(object instanceof CertAuthority){
                    CertAuthority ca = (CertAuthority) object;
                    return Optional.of(ca);
                }else{
                    log.error("Could not load cert authority, not a CertAuthority" + info.getName());
                }
            }catch (Exception e){
                log.error(e);
            }
        }
        return Optional.empty();
    }

    public List<CertAuthority> getAllCertAuthorities(){
        return new ArrayList<>(loadedCertAuthorities.values());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        reload();
    }
}
