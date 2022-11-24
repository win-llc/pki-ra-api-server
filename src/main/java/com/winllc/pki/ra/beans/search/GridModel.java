package com.winllc.pki.ra.beans.search;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class GridModel {
    private GridFilterModel filterModel;
    private List<GridSortItem> sortItems;

    public Optional<GridSortItem> firstSortItem(){
        if(CollectionUtils.isNotEmpty(sortItems)){
            return Optional.of(sortItems.get(0));
        }else{
            return Optional.empty();
        }
    }
}
