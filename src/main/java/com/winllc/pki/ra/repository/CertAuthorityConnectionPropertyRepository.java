package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.domain.CertAuthorityConnectionProperty;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertAuthorityConnectionPropertyRepository extends CrudRepository<CertAuthorityConnectionProperty, Long> {

}
