package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.IssuedCertificate;
import com.winllc.pki.ra.domain.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssuedCertificateRepository extends BaseRepository<IssuedCertificate> {

}
