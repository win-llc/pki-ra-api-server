package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.LdapSchemaOverlay;
import com.winllc.pki.ra.domain.LdapSchemaOverlayAttribute;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
@Transactional
public interface LdapSchemaOverlayAttributeRepository extends BaseRepository<LdapSchemaOverlayAttribute> {
    List<LdapSchemaOverlayAttribute> findAllByLdapSchemaOverlay(LdapSchemaOverlay overlay);
}
