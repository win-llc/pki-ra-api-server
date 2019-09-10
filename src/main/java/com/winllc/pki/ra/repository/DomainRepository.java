package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Domain;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DomainRepository extends CrudRepository<Domain, Long> {
    List<Domain> findAll();
    List<Domain> findAllByBaseContains(String search);
}
