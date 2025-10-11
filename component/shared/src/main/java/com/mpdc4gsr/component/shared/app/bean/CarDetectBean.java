package com.mpdc4gsr.component.shared.app.bean;

import java.util.List;

public class CarDetectBean {
    public String title;
    public List<CarDetectChildBean> detectChildBeans;

    public CarDetectBean(String title, List<CarDetectChildBean> detectChildBeans) {
        this.title = title;
        this.detectChildBeans = detectChildBeans;
    }
}


