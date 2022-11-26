package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.search.GridModel;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface UpdatedDataService<F, I> {

    Page<F> getPaged(Integer page,
                     Integer pageSize,
                     String order,
                     String sortBy,
                     Map<String, String> allRequestParams,
                     GridModel gridModel,
                     Authentication authentication);


    Page<F> getMyPaged(Integer page,
                       Integer pageSize,
                       String order,
                       String sortBy,
                       Map<String, String> allRequestParams,
                       GridModel gridModel,
                       Authentication authentication);

    List<F> getAll(Authentication authentication) throws Exception;

    F findRest(I id, Authentication authentication) throws Exception;


    F addRest(F entity, Map<String, String> allRequestParams, Authentication authentication) throws Exception;


    F updateRest(F entity, Map<String, String> allRequestParams, Authentication authentication) throws Exception;


    void deleteRest(I id, F form, Authentication authentication) throws Exception;
}
