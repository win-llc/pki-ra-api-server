package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.BaseEntity;
import com.winllc.acme.common.domain.UniqueEntity;
import com.winllc.acme.common.repository.PagingRepository;
import com.winllc.pki.ra.beans.form.ValidForm;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

public interface DataService<F> {

    Page<F> getPaged(Integer page,
                     Integer pageSize,
                     String order,
                     String sortBy,
                     Map<String, String> allRequestParams);


    Page<F> getMyPaged(Integer page,
                       Integer pageSize,
                       String order,
                       String sortBy,
                       Map<String, String> allRequestParams,
                       Authentication authentication);

    List<F> getAll(Long id, Authentication authentication) throws Exception;

    F findRest(Long id, Authentication authentication) throws Exception;


    F addRest(F entity, Authentication authentication) throws Exception;


    F updateRest(F entity, Authentication authentication) throws Exception;


    void deleteRest(Long id, Authentication authentication) throws Exception;
}
