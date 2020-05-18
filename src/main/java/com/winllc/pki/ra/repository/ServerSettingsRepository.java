package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.ServerSettings;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
@Transactional
public interface ServerSettingsRepository extends BaseRepository<ServerSettings> {

    Optional<ServerSettings> findDistinctByPropertyEquals(String property);
}
