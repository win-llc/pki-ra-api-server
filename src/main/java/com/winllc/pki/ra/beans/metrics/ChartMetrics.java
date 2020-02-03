package com.winllc.pki.ra.beans.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ChartMetrics {

    private List<String> labels;
    private List<Dataset> datasets;

    @JsonIgnore
    private List<LocalDate> dateRangeList;

    private ChartMetrics(List<LocalDate> dateRangeList){
        this.dateRangeList = dateRangeList;
    }

    public static ChartMetrics buildForRange(LocalDate from, LocalDate to){
        List<LocalDate> baseDates = getLocalDatesInRange(from, to);
        ChartMetrics chart = new ChartMetrics(baseDates);

        List<String> labels = baseDates
                .stream().map(k -> k.toString())
                .collect(Collectors.toList());
        chart.setLabels(labels);

        return chart;
    }

    public void addDataset(Dataset dataset){
        if(datasets == null) datasets = new LinkedList<>();
        datasets.add(dataset);
    }

    public void addDataset(Dataset.DatasetType type, String label, Map<LocalDate, Integer> datasetMap){
        Dataset ds = Dataset.build(type, label);
        Map<LocalDate, Integer> filledInDataMap = dateRangeList.stream()
                .collect(Collectors.toMap(ld -> ld, ld -> {
                    Integer count = datasetMap.get(ld);
                    if(count == null) count = 0;
                    return count;
                }));

        TreeMap<LocalDate, Integer> sorted = new TreeMap<>(filledInDataMap);

        Integer[] dataPoints = sorted.values().toArray(new Integer[0]);

        ds.setData(dataPoints);
        this.addDataset(ds);
    }

    private static List<LocalDate> getLocalDatesInRange(LocalDate from, LocalDate to){
        if(from.isBefore(to)){
            List<LocalDate> list = new LinkedList<>();
            LocalDate temp = from;
            while(temp.isBefore(to) || temp.isEqual(to)){
                list.add(temp);
                temp = temp.plusDays(1);
            }
            return list;
        }else{
            throw new IllegalArgumentException("From must be before to date");
        }
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    public static class Dataset {

        public enum DatasetType {
            SUCCESS("--success"),
            DANGER("--danger");

            private String value;

            DatasetType(String value){
                this.value = value;
            }

            @Override
            public String toString() {
                return value;
            }
        }

        private String label;
        private String backgroundColor;
        private String borderColor;
        private String pointHoverBackgroundColor;
        private Integer borderWidth;
        private Integer[] data;

        public static Dataset build(DatasetType type, String label){
            Dataset dataset = new Dataset();
            dataset.label = label;
            dataset.backgroundColor = "transparent";
            dataset.borderColor = type.toString();
            dataset.pointHoverBackgroundColor = "#fff";
            dataset.borderWidth = 1;
            return dataset;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getBackgroundColor() {
            return backgroundColor;
        }

        public void setBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        public String getBorderColor() {
            return borderColor;
        }

        public void setBorderColor(String borderColor) {
            this.borderColor = borderColor;
        }

        public String getPointHoverBackgroundColor() {
            return pointHoverBackgroundColor;
        }

        public void setPointHoverBackgroundColor(String pointHoverBackgroundColor) {
            this.pointHoverBackgroundColor = pointHoverBackgroundColor;
        }

        public Integer getBorderWidth() {
            return borderWidth;
        }

        public void setBorderWidth(Integer borderWidth) {
            this.borderWidth = borderWidth;
        }

        public Integer[] getData() {
            return data;
        }

        public void setData(Integer[] data) {
            this.data = data;
        }
    }
}
