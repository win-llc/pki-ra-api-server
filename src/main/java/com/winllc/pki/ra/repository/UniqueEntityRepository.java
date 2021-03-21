package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.domain.UniqueEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface UniqueEntityRepository<T extends UniqueEntity> extends PagingAndSortingRepository<T, Long>, JpaSpecificationExecutor<T> {

    List<T> findAll();
    Optional<T> findDistinctByUuidEquals(String uuid);
}
