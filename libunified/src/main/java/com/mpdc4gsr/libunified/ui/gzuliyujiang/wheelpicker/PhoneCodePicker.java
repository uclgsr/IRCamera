package com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.mpdc4gsr.libunified.ui.gzuliyujiang.wheelpicker.entity.PhoneCodeEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class PhoneCodePicker extends OptionPicker {
    public static String JSON = "[{\"prefix\":\"1\",\"en\":\"USA\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1\",\"en\":\"PuertoRico\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1\",\"en\":\"Canada\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"7\",\"en\":\"Russia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"7\",\"en\":\"Kazeakhstan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"20\",\"en\":\"Egypt\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"27\",\"en\":\"South Africa\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"30\",\"en\":\"Greece\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"31\",\"en\":\"Netherlands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"32\",\"en\":\"Belgium\",\"cn\":\"[CHINESE_TEXT]Hour\"},\n" +
            "{\"prefix\":\"33\",\"en\":\"France\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"34\",\"en\":\"Spain\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"36\",\"en\":\"Hungary\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"40\",\"en\":\"Romania\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"41\",\"en\":\"Switzerland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"43\",\"en\":\"Austria\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"44\",\"en\":\"United Kingdom\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"44\",\"en\":\"Jersey\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"44\",\"en\":\"Isle of Man\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"44\",\"en\":\"Guernsey\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"45\",\"en\":\"Denmark\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"46\",\"en\":\"Sweden\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"47\",\"en\":\"Norway\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"48\",\"en\":\"Poland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"51\",\"en\":\"Peru\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"52\",\"en\":\"Mexico\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"53\",\"en\":\"Cuba\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"54\",\"en\":\"Argentina\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"55\",\"en\":\"Brazill\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"56\",\"en\":\"Chile\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"57\",\"en\":\"Colombia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"58\",\"en\":\"Venezuela\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"60\",\"en\":\"Malaysia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"61\",\"en\":\"Australia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"62\",\"en\":\"Indonesia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"63\",\"en\":\"Philippines\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"64\",\"en\":\"NewZealand\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"65\",\"en\":\"Singapore\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"66\",\"en\":\"Thailand\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"81\",\"en\":\"Japan\",\"cn\":\"Day[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"82\",\"en\":\"Korea\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"84\",\"en\":\"Vietnam\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"86\",\"en\":\"China\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"90\",\"en\":\"Turkey\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"91\",\"en\":\"Indea\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"92\",\"en\":\"Pakistan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"93\",\"en\":\"Italy\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"93\",\"en\":\"Afghanistan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"94\",\"en\":\"SriLanka\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"94\",\"en\":\"Germany\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"95\",\"en\":\"Myanmar\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"98\",\"en\":\"Iran\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"212\",\"en\":\"Morocco\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"213\",\"en\":\"Algera\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"216\",\"en\":\"Tunisia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"218\",\"en\":\"Libya\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"220\",\"en\":\"Gambia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"221\",\"en\":\"Senegal\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"222\",\"en\":\"Mauritania\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"223\",\"en\":\"Mali\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"224\",\"en\":\"Guinea\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"225\",\"en\":\"Cote divoire\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"226\",\"en\":\"Burkina Faso\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"227\",\"en\":\"Niger\",\"cn\":\"[CHINESE_TEXT]Day[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"228\",\"en\":\"Togo\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"229\",\"en\":\"Benin\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"230\",\"en\":\"Mauritius\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"231\",\"en\":\"Liberia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"232\",\"en\":\"Sierra Leone\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"233\",\"en\":\"Ghana\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"234\",\"en\":\"Nigeria\",\"cn\":\"[CHINESE_TEXT]Day[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"235\",\"en\":\"Chad\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"236\",\"en\":\"Central African Republic\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"237\",\"en\":\"Cameroon\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"238\",\"en\":\"Cape Verde\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"239\",\"en\":\"Sao Tome and Principe\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"240\",\"en\":\"Guinea\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"241\",\"en\":\"Gabon\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"242\",\"en\":\"Republic of the Congo\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"243\",\"en\":\"Democratic Republic of the Congo\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"244\",\"en\":\"Angola\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"247\",\"en\":\"Ascension\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"248\",\"en\":\"Seychelles\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"249\",\"en\":\"Sudan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"250\",\"en\":\"Rwanda\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"251\",\"en\":\"Ethiopia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"253\",\"en\":\"Djibouti\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"254\",\"en\":\"Kenya\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"255\",\"en\":\"Tanzania\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"256\",\"en\":\"Uganda\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"257\",\"en\":\"Burundi\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"258\",\"en\":\"Mozambique\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"260\",\"en\":\"Zambia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"261\",\"en\":\"Madagascar\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"262\",\"en\":\"Reunion\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"262\",\"en\":\"Mayotte\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"263\",\"en\":\"Zimbabwe\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"264\",\"en\":\"Namibia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"265\",\"en\":\"Malawi\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"266\",\"en\":\"Lesotho\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"267\",\"en\":\"Botwana\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"268\",\"en\":\"Swaziland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"269\",\"en\":\"Comoros\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"297\",\"en\":\"Aruba\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"298\",\"en\":\"Faroe Islands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"299\",\"en\":\"Greenland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"350\",\"en\":\"Gibraltar\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"351\",\"en\":\"Portugal\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"352\",\"en\":\"Luxembourg\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"353\",\"en\":\"Ireland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"354\",\"en\":\"Iceland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"355\",\"en\":\"Albania\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"356\",\"en\":\"Malta\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"357\",\"en\":\"Cyprus\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"358\",\"en\":\"Finland\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"359\",\"en\":\"Bulgaria\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"370\",\"en\":\"Lithuania\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"371\",\"en\":\"Latvia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"372\",\"en\":\"Estonia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"373\",\"en\":\"Moldova\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"374\",\"en\":\"Armenia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"375\",\"en\":\"Belarus\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"376\",\"en\":\"Andorra\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"377\",\"en\":\"Monaco\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"378\",\"en\":\"San Marino\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"380\",\"en\":\"Ukraine\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"381\",\"en\":\"Serbia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"382\",\"en\":\"Montenegro\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"383\",\"en\":\"Kosovo\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"385\",\"en\":\"Croatia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"386\",\"en\":\"Slovenia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"387\",\"en\":\"Bosnia and Herzegovina\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"389\",\"en\":\"Macedonia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"420\",\"en\":\"Czech Republic\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"421\",\"en\":\"Slovakia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"423\",\"en\":\"Liechtenstein\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"501\",\"en\":\"Belize\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"502\",\"en\":\"Guatemala\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"503\",\"en\":\"EISalvador\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"504\",\"en\":\"Honduras\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"505\",\"en\":\"Nicaragua\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"506\",\"en\":\"Costa Rica\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"507\",\"en\":\"Panama\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"509\",\"en\":\"Haiti\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"590\",\"en\":\"Guadeloupe\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"591\",\"en\":\"Bolivia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"592\",\"en\":\"Guyana\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"593\",\"en\":\"Ecuador\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"594\",\"en\":\"French Guiana\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"595\",\"en\":\"Paraguay\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"596\",\"en\":\"Martinique\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"597\",\"en\":\"Suriname\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"598\",\"en\":\"Uruguay\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"599\",\"en\":\"Netherlands Antillse\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"670\",\"en\":\"Timor Leste\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"673\",\"en\":\"Brunei\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"675\",\"en\":\"Papua New Guinea\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"676\",\"en\":\"Tonga\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"678\",\"en\":\"Vanuatu\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"679\",\"en\":\"Fiji\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"682\",\"en\":\"Cook Islands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"684\",\"en\":\"Samoa Eastern\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"685\",\"en\":\"Samoa Western\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"687\",\"en\":\"New Caledonia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"689\",\"en\":\"French Polynesia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"852\",\"en\":\"Hong Kong\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"853\",\"en\":\"Macao\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"855\",\"en\":\"Cambodia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"856\",\"en\":\"Laos\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"880\",\"en\":\"Bangladesh\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"886\",\"en\":\"Taiwan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"960\",\"en\":\"Maldives\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"961\",\"en\":\"Lebanon\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"962\",\"en\":\"Jordan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"963\",\"en\":\"Syria\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"964\",\"en\":\"Iraq\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"965\",\"en\":\"Kuwait\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"966\",\"en\":\"Saudi Arabia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"967\",\"en\":\"Yemen\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"968\",\"en\":\"Oman\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"970\",\"en\":\"Palestinian\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"971\",\"en\":\"United Arab Emirates\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"972\",\"en\":\"Israel\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"973\",\"en\":\"Bahrain\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"974\",\"en\":\"Qotar\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"975\",\"en\":\"Bhutan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"976\",\"en\":\"Mongolia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"977\",\"en\":\"Nepal\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"992\",\"en\":\"Tajikistan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"993\",\"en\":\"Turkmenistan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"994\",\"en\":\"Azerbaijan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"995\",\"en\":\"Georgia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"996\",\"en\":\"Kyrgyzstan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"998\",\"en\":\"Uzbekistan\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1242\",\"en\":\"Bahamas\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1246\",\"en\":\"Barbados\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1264\",\"en\":\"Anguilla\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1268\",\"en\":\"Antigua and Barbuda\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1340\",\"en\":\"Virgin Islands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1345\",\"en\":\"Cayman Islands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1441\",\"en\":\"Bermuda\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1473\",\"en\":\"Grenada\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1649\",\"en\":\"Turks and Caicos Islands\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1664\",\"en\":\"Montserrat\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1671\",\"en\":\"Guam\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1758\",\"en\":\"St.Lucia\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1767\",\"en\":\"Dominica\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1784\",\"en\":\"St.Vincent\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1809\",\"en\":\"Dominican Republic\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1868\",\"en\":\"Trinidad and Tobago\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1869\",\"en\":\"St Kitts and Nevis\",\"cn\":\"[CHINESE_TEXT]\"},\n" +
            "{\"prefix\":\"1876\",\"en\":\"Jamaica\",\"cn\":\"[CHINESE_TEXT]\"}]";
    private boolean onlyChina = false;

    public PhoneCodePicker(@NonNull Activity activity) {
        super(activity);
    }

    public PhoneCodePicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    public void setOnlyChina(boolean onlyChina) {
        this.onlyChina = onlyChina;
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
        PhoneCodeEntity entity = new PhoneCodeEntity();
        entity.setCode(code);
        super.setDefaultValue(entity);
    }

    public void setDefaultValueByName(String name) {
        PhoneCodeEntity entity = new PhoneCodeEntity();
        entity.setName(name);
        super.setDefaultValue(entity);
    }

    public void setDefaultValueByEnglish(String english) {
        PhoneCodeEntity entity = new PhoneCodeEntity();
        entity.setEnglish(english);
        super.setDefaultValue(entity);
    }

    @Override
    protected List<?> provideData() {
        List<PhoneCodeEntity> data = new ArrayList<>();
        if (onlyChina) {
            PhoneCodeEntity china = new PhoneCodeEntity();
            china.setCode("+86");
            china.setName("[CHINESE_TEXT]+86");
            china.setEnglish("Chinese Mainland");
            data.add(china);
            PhoneCodeEntity hongKong = new PhoneCodeEntity();
            hongKong.setCode("+852");
            hongKong.setName("[CHINESE_TEXT]+852");
            hongKong.setEnglish("Hong Kong");
            data.add(hongKong);
            PhoneCodeEntity macao = new PhoneCodeEntity();
            macao.setCode("+853");
            macao.setName("[CHINESE_TEXT]+853");
            macao.setEnglish("Macao");
            data.add(macao);
            PhoneCodeEntity taiwan = new PhoneCodeEntity();
            taiwan.setCode("+886");
            taiwan.setName("[CHINESE_TEXT]+886");
            taiwan.setEnglish("Taiwan");
            data.add(taiwan);
        } else {
            try {
                JSONArray jsonArray = new JSONArray(JSON);
                for (int i = 0, n = jsonArray.length(); i < n; i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    PhoneCodeEntity entity = new PhoneCodeEntity();
                    entity.setCode("+" + jsonObject.getString("prefix"));
                    entity.setName(jsonObject.getString("cn"));
                    entity.setEnglish(jsonObject.getString("en"));
                    data.add(entity);
                }
            } catch (JSONException e) {
            }
        }
        return data;
    }

}
