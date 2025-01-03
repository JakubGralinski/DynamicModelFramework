package org.example;

import java.util.Map;

public interface ModelBase {
    void setData(Map<String, Object> data);
    void run();
    Map<String, Object> getResults();
}