package com.example.testcamera.second;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FilterItem {
    private String titleFilter = "";
    private int idFilter = 0;
    public static final Map<String, Integer> FILTER_MAP = createMap();

    public static Map<String, Integer> createMap() {
        Map<String, Integer> result = new HashMap<>();
        result.put("origin", 1);

        return Collections.unmodifiableMap(result);
    }

    public FilterItem(String titleFilter, int idFilter) {
        this.titleFilter = titleFilter;
        this.idFilter = idFilter;
    }

    public String getTitleFilter() {
        return titleFilter;
    }

    public void setTitleFilter(String titleFilter) {
        this.titleFilter = titleFilter;
    }

    public int getIdFilter() {
        return idFilter;
    }

    public void setIdFilter(int idFilter) {
        this.idFilter = idFilter;
    }
}
