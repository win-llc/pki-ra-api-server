package com.winllc.pki.ra.ca;

import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.keystore.ApplicationKeystore;
import com.winllc.pki.ra.repository.CertAuthorityConnectionInfoRepository;
import com.winllc.pki.ra.service.external.beans.DirectoryServerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class LoadedCertAuthorityStore implements InitializingBean {

    private static final Logger log = LogManager.getLogger(LoadedCertAuthorityStore.class);

    private final Map<String, CertAuthority> loadedCertAuthorities = new ConcurrentHashMap<>();

    private final CertAuthorityConnectionInfoRepository repository;
    private final ApplicationKeystore applicationKeystore;
    private final EntityManagerFactory entityManagerFactory;

    public LoadedCertAuthorityStore(ApplicationContext applicationContext){
        this.repository = applicationContext.getBean(CertAuthorityConnectionInfoRepository.class);
        this.applicationKeystore = applicationContext.getBean(ApplicationKeystore.class);
        this.entityManagerFactory = applicationContext.getBean(EntityManagerFactory.class);
    }

    public void reload() {
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
        CertAuthority certAuthority = null;
        Optional<CertAuthorityConnectionInfo> infoOptional = repository.findByName(connectionName);

        if (infoOptional.isPresent()) {
            CertAuthorityConnectionInfo info = infoOptional.get();

            try {
                Hibernate.initialize(info.getProperties());
                switch (info.getType()) {
                    case INTERNAL:
                        certAuthority = new InternalCertAuthority(info, entityManagerFactory);
                        break;
                    case DOGTAG:
                        certAuthority = new DogTagCertAuthority(info, applicationKeystore);
                        break;
                }
                return Optional.of(certAuthority);
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
