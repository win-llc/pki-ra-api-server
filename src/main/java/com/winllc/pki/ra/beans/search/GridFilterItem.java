package com.winllc.pki.ra.beans.search;


import com.winllc.acme.common.domain.BaseEntity;
import com.winllc.pki.ra.util.DateUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Getter
@Setter
public class GridFilterItem<T extends BaseEntity> {

    private String columnField;
    private String id;
    private String operatorValue;
    private Object value;

    public Class findValueClass(){
        if(value.toString().equalsIgnoreCase("false") ||
                value.toString().equalsIgnoreCase("true")){
            return Boolean.class;
        }else{
            LocalDateTime ldt = DateUtil.dataTableTimestampToLocalDateTime(value.toString()).orElse(null);
            if(ldt != null){
                return LocalDateTime.class;
            }else{
                return String.class;
            }
        }
    }

    public Predicate toPredicate(Root<T> root,
                                            CriteriaBuilder cb){
        Predicate predicate = null;
        Object objValue = getValue();

        boolean dataObjectContainsField = root.getModel().getAttributes().stream()
                .map(a -> a.getName())
                .anyMatch(n -> n.equalsIgnoreCase(columnField));

        if(objValue != null && dataObjectContainsField) {
            Class valueClass = findValueClass();
            if (valueClass == Boolean.class) {
                Boolean value = Boolean.valueOf(objValue.toString());
                if (getOperatorValue().equalsIgnoreCase("is")) {
                    predicate = cb.equal(root.get(getColumnField()), value);
                }
            } else if (valueClass == String.class)  {
                String value = objValue.toString();

                List<String> vals = Stream.of(value.split(",")).toList();

                List<Predicate> searchPredicates = new ArrayList<>();
                for(String val : vals){
                    Predicate temp = null;
                    if(getOperatorValue().equalsIgnoreCase("equals")){
                        temp = cb.equal(root.get(getColumnField()), val);
                    } else if(getOperatorValue().equalsIgnoreCase("contains")){
                        String searchValue = "%" + val + "%";
                        temp = cb.like(root.get(getColumnField()), searchValue);
                    }else if(getOperatorValue().equalsIgnoreCase("startsWith")){
                        String searchValue = val + "%";
                        temp = cb.like(root.get(getColumnField()), searchValue);
                    }else if(getOperatorValue().equalsIgnoreCase("isAnyOf")){
                        //todo handle list
                    }else if(getOperatorValue().equalsIgnoreCase("endsWith")){
                        String searchValue = "%" + val;
                        temp = cb.like(root.get(getColumnField()), searchValue);
                    }else if(getOperatorValue().equalsIgnoreCase("isEmpty")){
                        temp = cb.isEmpty(root.get(getColumnField()));
                    }else if(getOperatorValue().equalsIgnoreCase("isNotEmpty")){
                        temp = cb.isNotEmpty(root.get(getColumnField()));
                    }

                    if(temp != null){
                        searchPredicates.add(temp);
                    }
                }

                if(CollectionUtils.isNotEmpty(searchPredicates)) {
                    predicate = cb.or(searchPredicates.toArray(new Predicate[0]));
                }

            } else if (valueClass == LocalDateTime.class){
                LocalDateTime value = DateUtil.dataTableTimestampToLocalDateTime(objValue.toString()).orElse(null);

                if(getOperatorValue().equalsIgnoreCase("is")){
                    predicate = cb.equal(root.get(getColumnField()), value);
                } else if(getOperatorValue().equalsIgnoreCase("is not")){
                    predicate = cb.notEqual(root.get(getColumnField()), value);
                } else if(getOperatorValue().equalsIgnoreCase("is after")){
                    predicate = cb.greaterThan(root.get(getColumnField()), value);
                } else if(getOperatorValue().equalsIgnoreCase("is on or after")){
                    predicate = cb.greaterThanOrEqualTo(root.get(getColumnField()), value);
                } else if(getOperatorValue().equalsIgnoreCase("is before")){
                    predicate = cb.lessThan(root.get(getColumnField()), value);
                } else if(getOperatorValue().equalsIgnoreCase("is on or before")){
                    predicate = cb.lessThanOrEqualTo(root.get(getColumnField()), value);
                } else if(getOperatorValue().equalsIgnoreCase("is empty")){
                    predicate = cb.isEmpty(root.get(getColumnField()));
                }else if(getOperatorValue().equalsIgnoreCase("is not empty")){
                    predicate = cb.isNotEmpty(root.get(getColumnField()));
                }
            }


        }

        return predicate;
    }
}
