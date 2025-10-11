package com.mpdc4gsr.component.shared.app.bean;

import com.mpdc4gsr.component.shared.app.utils.SharedTemperatureUtils;

public class CarDetectChildBean {
    public int type;
    public int pos;
    public String description;
    public String item;
    public String temperature;
    public boolean isSelected;

    public CarDetectChildBean(int type, int pos, String description, String item, String temperature) {
        this.type = type;
        this.pos = pos;
        this.description = description;
        this.item = item;
        this.temperature = temperature;
    }

    public String buildString() {
        String[] temperatures = temperature.split("~");
        String minTemp = SharedTemperatureUtils.INSTANCE.formatTemperature(Float.parseFloat(temperatures[0]), SharedTemperatureUtils.TemperatureUnit.CELSIUS);
        String maxTemp = SharedTemperatureUtils.INSTANCE.formatTemperature(Float.parseFloat(temperatures[1]), SharedTemperatureUtils.TemperatureUnit.CELSIUS);
        return item + "(" + minTemp + "~" + maxTemp + ")";
    }
}



