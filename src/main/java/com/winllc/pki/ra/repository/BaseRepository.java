package com.winllc.pki.ra.repository;

import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface BaseRepository<T extends AbstractPersistable<Long>> extends CrudRepository<T, Long> {
    List<T> findAll();
}
