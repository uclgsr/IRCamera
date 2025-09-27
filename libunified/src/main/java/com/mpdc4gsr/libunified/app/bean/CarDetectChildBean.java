package com.mpdc4gsr.libunified.app.bean;

import com.mpdc4gsr.libunified.app.utils.UnifiedTemperatureUtils;

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
        String minTemp = UnifiedTemperatureUtils.INSTANCE.formatTemperature(Float.parseFloat(temperatures[0]), UnifiedTemperatureUtils.TemperatureUnit.CELSIUS);
        String maxTemp = UnifiedTemperatureUtils.INSTANCE.formatTemperature(Float.parseFloat(temperatures[1]), UnifiedTemperatureUtils.TemperatureUnit.CELSIUS);
        return item + "(" + minTemp + "~" + maxTemp + ")";
    }
}
