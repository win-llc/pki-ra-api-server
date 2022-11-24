package com.winllc.pki.ra.beans.search;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class GridFilterModel {

    private List<GridFilterItem> items;
    private GridLinkOperator linkOperator;
    private GridLinkOperator quickFilterLogicOperator;
    private List<Object> quickFilterValues = new ArrayList<>();

    public Optional<String> firstQuickFilter(){
        if(CollectionUtils.isNotEmpty(quickFilterValues)){
            String filter = quickFilterValues.get(0).toString();
            if(StringUtils.isNotBlank(filter)){
                return Optional.of(filter);
            }else {
                return Optional.empty();
            }
        }else{
            return Optional.empty();
        }
    }
}
