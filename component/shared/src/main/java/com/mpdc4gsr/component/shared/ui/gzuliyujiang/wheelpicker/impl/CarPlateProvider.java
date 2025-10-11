package com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.impl;

import androidx.annotation.NonNull;

import com.mpdc4gsr.component.shared.ui.gzuliyujiang.wheelpicker.contract.LinkageProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CarPlateProvider implements LinkageProvider {
    private static final String[] ABBREVIATIONS = {
            "Province1", "Province2", "Province3", "Province4", "Province5", "Province6", "Province7", "Province8", "Province9",
            "Province10", "Province11", "Province12", "Province13", "Province14", "Province15", "Province16", "Province17", "Province18",
            "Province19", "Province20", "Province21", "Province22", "Province23", "Province24", "Province25", "Province26", "Province27",
            "Province28", "Province29", "Province30", "Province31"};

    @Override
    public boolean firstLevelVisible() {
        return true;
    }

    @Override
    public boolean thirdLevelVisible() {
        return false;
    }

    @NonNull
    @Override
    public List<String> provideFirstData() {
        List<String> provinces = new ArrayList<>();
        Collections.addAll(provinces, ABBREVIATIONS);
        return provinces;
    }

    @NonNull
    @Override
    public List<String> linkageSecondData(int firstIndex) {
        List<String> letters = new ArrayList<>();
        if (firstIndex == INDEX_NO_FOUND) {
            firstIndex = 0;
        }
        String province = provideFirstData().get(firstIndex);
        switch (province) {
            case "Province1":
                for (char i = 'A'; i <= 'M'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.add("Y");
                break;
            case "Province2":
            case "Province3":
                for (char i = 'A'; i <= 'H'; i++) {
                    letters.add(String.valueOf(i));
                }
                break;
            case "Province4":
                for (char i = 'A'; i <= 'H'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.add("J");
                letters.add("R");
                letters.add("S");
                letters.add("T");
                break;
            case "Province5":
                for (char i = 'A'; i <= 'M'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("G");
                letters.remove("I");
                break;
            case "Province6":
            case "Province7":
                for (char i = 'A'; i <= 'M'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                break;
            case "Province8":
            case "Province9":
                for (char i = 'A'; i <= 'P'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province10":
            case "Province11":
                for (char i = 'A'; i <= 'K'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                break;
            case "Province12":
            case "Province13":
                for (char i = 'A'; i <= 'R'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province14":
                for (char i = 'A'; i <= 'D'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.add("R");
                break;
            case "Province15":
                for (char i = 'A'; i <= 'N'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                break;
            case "Province16":
                for (char i = 'A'; i <= 'L'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                break;
            case "Province17":
            case "Province18":
                for (char i = 'A'; i <= 'S'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province19":
                for (char i = 'A'; i <= 'V'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                letters.add("Y");
                break;
            case "Province20":
                for (char i = 'A'; i <= 'U'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province21":
                for (char i = 'A'; i <= 'N'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                letters.add("U");
                break;
            case "Province22":
                for (char i = 'A'; i <= 'Z'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province23":
                for (char i = 'A'; i <= 'P'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.remove("O");
                letters.add("R");
                break;
            case "Province24":
            case "Province25":
                for (char i = 'A'; i <= 'E'; i++) {
                    letters.add(String.valueOf(i));
                }
                break;
            case "Province26":
                for (char i = 'A'; i <= 'D'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("D");
                letters.remove("E");
                break;
            case "Province27":
                for (char i = 'A'; i <= 'Z'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("G");
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province28":
            case "Province29":
                for (char i = 'A'; i <= 'J'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                break;
            case "Province30":

                letters.add("A-V");
                for (char i = 'A'; i <= 'S'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("B");
                letters.remove("I");
                letters.remove("O");
                break;
            case "Province31":
                for (char i = 'A'; i <= 'K'; i++) {
                    letters.add(String.valueOf(i));
                }
                letters.remove("I");
                letters.add("V");
                break;
        }
        return letters;
    }

    @NonNull
    @Override
    public List<?> linkageThirdData(int firstIndex, int secondIndex) {
        return new ArrayList<>();
    }

    @Override
    public int findFirstIndex(Object firstValue) {
        if (firstValue == null) {
            return INDEX_NO_FOUND;
        }
        for (int i = 0, n = ABBREVIATIONS.length; i < n; i++) {
            String abbreviation = ABBREVIATIONS[i];
            if (abbreviation.equals(firstValue.toString())) {
                return i;
            }
        }
        return INDEX_NO_FOUND;
    }

    @Override
    public int findSecondIndex(int firstIndex, Object secondValue) {
        if (secondValue == null) {
            return INDEX_NO_FOUND;
        }
        List<String> letters = linkageSecondData(firstIndex);
        for (int i = 0, n = letters.size(); i < n; i++) {
            String letter = letters.get(i);
            if (letter.equals(secondValue.toString())) {
                return i;
            }
        }
        return INDEX_NO_FOUND;
    }

    @Override
    public int findThirdIndex(int firstIndex, int secondIndex, Object thirdValue) {
        return 0;
    }

}


