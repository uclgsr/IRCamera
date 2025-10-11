package com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.annotation.EthnicSpec;
import com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.entity.EthnicEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public class EthnicPicker extends OptionPicker {
    public static String JSON = "[{\"code\":\"01\",\"name\":\"Han\",\"spelling\":\"Han\"}," +
            "{\"code\":\"02\",\"name\":\"Mongol\",\"spelling\":\"Mongol\"}," +
            "{\"code\":\"03\",\"name\":\"Hui\",\"spelling\":\"Hui\"}," +
            "{\"code\":\"04\",\"name\":\"Zang\",\"spelling\":\"Zang\"}," +
            "{\"code\":\"05\",\"name\":\"Uygur\",\"spelling\":\"Uygur\"}," +
            "{\"code\":\"06\",\"name\":\"Miao\",\"spelling\":\"Miao\"}," +
            "{\"code\":\"07\",\"name\":\"Yi\",\"spelling\":\"Yi\"}," +
            "{\"code\":\"08\",\"name\":\"Zhuang\",\"spelling\":\"Zhuang\"}," +
            "{\"code\":\"09\",\"name\":\"Buyei\",\"spelling\":\"Buyei\"}," +
            "{\"code\":\"10\",\"name\":\"Chosen\",\"spelling\":\"Chosen\"}," +
            "{\"code\":\"11\",\"name\":\"Man\",\"spelling\":\"Man\"}," +
            "{\"code\":\"12\",\"name\":\"Dong\",\"spelling\":\"Dong\"}," +
            "{\"code\":\"13\",\"name\":\"Yao\",\"spelling\":\"Yao\"}," +
            "{\"code\":\"14\",\"name\":\"Bai\",\"spelling\":\"Bai\"}," +
            "{\"code\":\"15\",\"name\":\"Tujia\",\"spelling\":\"Tujia\"}," +
            "{\"code\":\"16\",\"name\":\"Hani\",\"spelling\":\"Hani\"}," +
            "{\"code\":\"17\",\"name\":\"Kazak\",\"spelling\":\"Kazak\"}," +
            "{\"code\":\"18\",\"name\":\"Dai\",\"spelling\":\"Dai\"}," +
            "{\"code\":\"19\",\"name\":\"Li\",\"spelling\":\"Li\"}," +
            "{\"code\":\"20\",\"name\":\"Lisu\",\"spelling\":\"Lisu\"}," +
            "{\"code\":\"21\",\"name\":\"Va\",\"spelling\":\"Va\"}," +
            "{\"code\":\"22\",\"name\":\"She\",\"spelling\":\"She\"}," +
            "{\"code\":\"23\",\"name\":\"Gaoshan\",\"spelling\":\"Gaoshan\"}," +
            "{\"code\":\"24\",\"name\":\"Lahu\",\"spelling\":\"Lahu\"}," +
            "{\"code\":\"25\",\"name\":\"Sui\",\"spelling\":\"Sui\"}," +
            "{\"code\":\"26\",\"name\":\"Dongxiang\",\"spelling\":\"Dongxiang\"}," +
            "{\"code\":\"27\",\"name\":\"Naxi\",\"spelling\":\"Naxi\"}," +
            "{\"code\":\"28\",\"name\":\"Jingpo\",\"spelling\":\"Jingpo\"}," +
            "{\"code\":\"29\",\"name\":\"Kirgiz\",\"spelling\":\"Kirgiz\"}," +
            "{\"code\":\"30\",\"name\":\"Tu\",\"spelling\":\"Tu\"}," +
            "{\"code\":\"31\",\"name\":\"Daur\",\"spelling\":\"Daur\"}," +
            "{\"code\":\"32\",\"name\":\"Mulao\",\"spelling\":\"Mulao\"}," +
            "{\"code\":\"33\",\"name\":\"Qiang\",\"spelling\":\"Qiang\"}," +
            "{\"code\":\"34\",\"name\":\"Blang\",\"spelling\":\"Blang\"}," +
            "{\"code\":\"35\",\"name\":\"Salar\",\"spelling\":\"Salar\"}," +
            "{\"code\":\"36\",\"name\":\"Maonan\",\"spelling\":\"Maonan\"}," +
            "{\"code\":\"37\",\"name\":\"Gelao\",\"spelling\":\"Gelao\"}," +
            "{\"code\":\"38\",\"name\":\"Xibe\",\"spelling\":\"Xibe\"}," +
            "{\"code\":\"39\",\"name\":\"Achang\",\"spelling\":\"Achang\"}," +
            "{\"code\":\"40\",\"name\":\"Pumi\",\"spelling\":\"Pumi\"}," +
            "{\"code\":\"41\",\"name\":\"Tajik\",\"spelling\":\"Tajik\"}," +
            "{\"code\":\"42\",\"name\":\"Nu\",\"spelling\":\"Nu\"}," +
            "{\"code\":\"43\",\"name\":\"Uzbek\",\"spelling\":\"Uzbek\"}," +
            "{\"code\":\"44\",\"name\":\"Russ\",\"spelling\":\"Russ\"}," +
            "{\"code\":\"45\",\"name\":\"Ewenki\",\"spelling\":\"Ewenki\"}," +
            "{\"code\":\"46\",\"name\":\"Deang\",\"spelling\":\"Deang\"}," +
            "{\"code\":\"47\",\"name\":\"Bonan\",\"spelling\":\"Bonan\"}," +
            "{\"code\":\"48\",\"name\":\"Yugur\",\"spelling\":\"Yugur\"}," +
            "{\"code\":\"49\",\"name\":\"Gin\",\"spelling\":\"Gin\"}," +
            "{\"code\":\"50\",\"name\":\"Tatar\",\"spelling\":\"Tatar\"}," +
            "{\"code\":\"51\",\"name\":\"Derung\",\"spelling\":\"Derung\"}," +
            "{\"code\":\"52\",\"name\":\"Oroqen\",\"spelling\":\"Oroqen\"}," +
            "{\"code\":\"53\",\"name\":\"Hezhen\",\"spelling\":\"Hezhen\"}," +
            "{\"code\":\"54\",\"name\":\"Monba\",\"spelling\":\"Monba\"}," +
            "{\"code\":\"55\",\"name\":\"Lhoba\",\"spelling\":\"Lhoba\"}," +
            "{\"code\":\"56\",\"name\":\"Jino\",\"spelling\":\"Jino\"}]";
    private int ethnicSpec = EthnicSpec.DEFAULT;

    public EthnicPicker(@NonNull Activity activity) {
        super(activity);
    }

    public EthnicPicker(@NonNull Activity activity, int themeResId) {
        super(activity, themeResId);
    }

    public void setEthnicSpec(@EthnicSpec int ethnicSpec) {
        this.ethnicSpec = ethnicSpec;
        setData(provideData());
    }

    @Override
    public void setDefaultValue(Object item) {
        if (item instanceof String) {
            setDefaultValueByName(item.toString());
        } else {
            super.setDefaultValue(item);
        }
    }

    public void setDefaultValueByCode(String code) {
        EthnicEntity entity = new EthnicEntity();
        entity.setCode(code);
        super.setDefaultValue(entity);
    }

    public void setDefaultValueByName(String name) {
        EthnicEntity entity = new EthnicEntity();
        entity.setName(name);
        super.setDefaultValue(entity);
    }

    public void setDefaultValueBySpelling(String spelling) {
        EthnicEntity entity = new EthnicEntity();
        entity.setSpelling(spelling);
        super.setDefaultValue(entity);
    }

    @Override
    protected List<EthnicEntity> provideData() {
        ArrayList<EthnicEntity> data = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(JSON);
            for (int i = 0, n = jsonArray.length(); i < n; i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                EthnicEntity entity = new EthnicEntity();
                entity.setCode(jsonObject.getString("code"));
                entity.setName(jsonObject.getString("name"));
                entity.setSpelling(jsonObject.getString("spelling"));
                data.add(entity);
            }
        } catch (JSONException e) {
        }
        switch (ethnicSpec) {
            case EthnicSpec.DEFAULT:
                EthnicEntity other = new EthnicEntity();
                other.setCode("97");
                other.setName("Other");
                other.setSpelling("Other");
                data.add(other);
                EthnicEntity foreign = new EthnicEntity();
                foreign.setCode("98");
                foreign.setName("Foreign Ancestry");
                foreign.setSpelling("Foreign");
                data.add(foreign);
                break;
            case EthnicSpec.SEVENTH_NATIONAL_CENSUS:
                EthnicEntity unrecognized = new EthnicEntity();
                unrecognized.setCode("97");
                unrecognized.setName("Undetermined EthnicityhumanPopulation");
                unrecognized.setSpelling("Unrecognized");
                data.add(unrecognized);
                EthnicEntity naturalization = new EthnicEntity();
                naturalization.setCode("98");
                naturalization.setName("Naturalized");
                naturalization.setSpelling("Naturalization");
                data.add(naturalization);
                break;
            default:
                break;
        }
        return data;
    }

}


