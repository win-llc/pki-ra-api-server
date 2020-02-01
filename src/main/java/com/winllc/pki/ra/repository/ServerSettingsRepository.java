package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.ServerSettings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerSettingsRepository extends CrudRepository<ServerSettings, Long> {

    List<ServerSettings> findAll();
    Optional<ServerSettings> findDistinctByPropertyEquals(String property);
}
