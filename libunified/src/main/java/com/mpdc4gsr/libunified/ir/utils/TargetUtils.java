package com.mpdc4gsr.libunified.ir.utils;

import com.mpdc4gsr.libunified.R;
import com.mpdc4gsr.libunified.app.bean.ObserveBean;

public enum TargetUtils {
    ;

    public static int getSelectTargetDraw(int targetMeasureMode, int targetType, int targetColorType) {
        int currentSelectDraw = R.drawable.svg_ic_target_horizontal_person_green;
        if (ObserveBean.TYPE_TARGET_COLOR_GREEN == targetColorType) {
            if (ObserveBean.TYPE_MEASURE_PERSON == targetMeasureMode) {
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.svg_ic_target_horizontal_person_green;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_person_green;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_person_green;
                }
            } else if (ObserveBean.TYPE_MEASURE_SHEEP == targetMeasureMode) {
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_green;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_green;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_green;
                }
            } else if (ObserveBean.TYPE_MEASURE_DOG == targetMeasureMode) {
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_green;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_green;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_green;
                }
            } else if (ObserveBean.TYPE_MEASURE_BIRD == targetMeasureMode) {

                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_green;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_green;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_green;
                }
            }
        } else if (ObserveBean.TYPE_TARGET_COLOR_RED == targetColorType) {
            if (ObserveBean.TYPE_MEASURE_PERSON == targetMeasureMode) {
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_person_red;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_person_red;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_person_red;
                }
            } else if (ObserveBean.TYPE_MEASURE_SHEEP == targetMeasureMode) {
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_red;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_red;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_red;
                }
            } else if (ObserveBean.TYPE_MEASURE_DOG == targetMeasureMode) {
//                if(targetType == ObserveBean.TYPE_TARGET_HORIZONTAL){
//                    currentSelectDraw = R.drawable.ic_target_horizontal_dog_red;
//                } else if(targetType == ObserveBean.TYPE_TARGET_VERTICAL){
//                    currentSelectDraw = R.drawable.ic_target_vertical_dog_red;
//                } else if(targetType == ObserveBean.TYPE_TARGET_CIRCLE){
//                    currentSelectDraw = R.drawable.ic_target_circle_dog_red;
//                }
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_red;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_red;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_red;
                }
            } else if (ObserveBean.TYPE_MEASURE_BIRD == targetMeasureMode) {
//                if(targetType == ObserveBean.TYPE_TARGET_HORIZONTAL){
//                    currentSelectDraw = R.drawable.ic_target_horizontal_bird_red;
//                } else if(targetType == ObserveBean.TYPE_TARGET_VERTICAL){
//                    currentSelectDraw = R.drawable.ic_target_vertical_bird_red;
//                } else if(targetType == ObserveBean.TYPE_TARGET_CIRCLE){
//                    currentSelectDraw = R.drawable.ic_target_circle_bird_red;
//                }
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_red;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_red;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_red;
                }
            }
        } else if (ObserveBean.TYPE_TARGET_COLOR_BLUE == targetColorType) {
            if (ObserveBean.TYPE_MEASURE_PERSON == targetMeasureMode) {
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_person_blue;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_person_blue;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_person_blue;
                }
            } else if (ObserveBean.TYPE_MEASURE_SHEEP == targetMeasureMode) {
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_blue;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_blue;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_blue;
                }
            } else if (ObserveBean.TYPE_MEASURE_DOG == targetMeasureMode) {
//                if(targetType == ObserveBean.TYPE_TARGET_HORIZONTAL){
//                    currentSelectDraw = R.drawable.ic_target_horizontal_dog_blue;
//                } else if(targetType == ObserveBean.TYPE_TARGET_VERTICAL){
//                    currentSelectDraw = R.drawable.ic_target_vertical_dog_blue;
//                } else if(targetType == ObserveBean.TYPE_TARGET_CIRCLE){
//                    currentSelectDraw = R.drawable.ic_target_circle_dog_blue;
//                }
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_blue;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_blue;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_blue;
                }
            } else if (ObserveBean.TYPE_MEASURE_BIRD == targetMeasureMode) {
//                if(targetType == ObserveBean.TYPE_TARGET_HORIZONTAL){
//                    currentSelectDraw = R.drawable.ic_target_horizontal_bird_blue;
//                } else if(targetType == ObserveBean.TYPE_TARGET_VERTICAL){
//                    currentSelectDraw = R.drawable.ic_target_vertical_bird_blue;
//                } else if(targetType == ObserveBean.TYPE_TARGET_CIRCLE){
//                    currentSelectDraw = R.drawable.ic_target_circle_bird_blue;
//                }
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_blue;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_blue;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_blue;
                }
            }
        } else if (ObserveBean.TYPE_TARGET_COLOR_BLACK == targetColorType) {
            if (ObserveBean.TYPE_MEASURE_PERSON == targetMeasureMode) {
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_person_black;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_person_black;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_person_black;
                }
            } else if (ObserveBean.TYPE_MEASURE_SHEEP == targetMeasureMode) {
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_black;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_black;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_black;
                }
            } else if (ObserveBean.TYPE_MEASURE_DOG == targetMeasureMode) {
//                if(targetType == ObserveBean.TYPE_TARGET_HORIZONTAL){
//                    currentSelectDraw = R.drawable.ic_target_horizontal_dog_black;
//                } else if(targetType == ObserveBean.TYPE_TARGET_VERTICAL){
//                    currentSelectDraw = R.drawable.ic_target_vertical_dog_black;
//                } else if(targetType == ObserveBean.TYPE_TARGET_CIRCLE){
//                    currentSelectDraw = R.drawable.ic_target_circle_dog_black;
//                }
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_black;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_black;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_black;
                }
            } else if (ObserveBean.TYPE_MEASURE_BIRD == targetMeasureMode) {
//                if(targetType == ObserveBean.TYPE_TARGET_HORIZONTAL){
//                    currentSelectDraw = R.drawable.ic_target_horizontal_bird_black;
//                } else if(targetType == ObserveBean.TYPE_TARGET_VERTICAL){
//                    currentSelectDraw = R.drawable.ic_target_vertical_bird_black;
//                } else if(targetType == ObserveBean.TYPE_TARGET_CIRCLE){
//                    currentSelectDraw = R.drawable.ic_target_circle_bird_black;
//                }
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_black;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_black;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_black;
                }
            }
        } else if (ObserveBean.TYPE_TARGET_COLOR_WHITE == targetColorType) {
            if (ObserveBean.TYPE_MEASURE_PERSON == targetMeasureMode) {
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_person_white;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_person_white;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_person_white;
                }
            } else if (ObserveBean.TYPE_MEASURE_SHEEP == targetMeasureMode) {
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_white;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_white;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_white;
                }
            } else if (ObserveBean.TYPE_MEASURE_DOG == targetMeasureMode) {
//                if(targetType == ObserveBean.TYPE_TARGET_HORIZONTAL){
//                    currentSelectDraw = R.drawable.ic_target_horizontal_dog_white;
//                } else if(targetType == ObserveBean.TYPE_TARGET_VERTICAL){
//                    currentSelectDraw = R.drawable.ic_target_vertical_dog_white;
//                } else if(targetType == ObserveBean.TYPE_TARGET_CIRCLE){
//                    currentSelectDraw = R.drawable.ic_target_circle_dog_white;
//                }
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_white;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_white;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_white;
                }
            } else if (ObserveBean.TYPE_MEASURE_BIRD == targetMeasureMode) {
//                if(targetType == ObserveBean.TYPE_TARGET_HORIZONTAL){
//                    currentSelectDraw = R.drawable.ic_target_horizontal_bird_white;
//                } else if(targetType == ObserveBean.TYPE_TARGET_VERTICAL){
//                    currentSelectDraw = R.drawable.ic_target_vertical_bird_white;
//                } else if(targetType == ObserveBean.TYPE_TARGET_CIRCLE){
//                    currentSelectDraw = R.drawable.ic_target_circle_bird_white;
//                }
                if (ObserveBean.TYPE_TARGET_HORIZONTAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_horizontal_sheep_white;
                } else if (ObserveBean.TYPE_TARGET_VERTICAL == targetType) {
                    currentSelectDraw = R.drawable.ic_target_vertical_sheep_white;
                } else if (ObserveBean.TYPE_TARGET_CIRCLE == targetType) {
                    currentSelectDraw = R.drawable.ic_target_circle_sheep_white;
                }
            }
        }
        return currentSelectDraw;
    }

    public static float getMeasureSize(int targetMeasureMode) {
        float mMeasureSize = 180.0f;
        switch (targetMeasureMode) {
            case ObserveBean.TYPE_MEASURE_PERSON://人
                mMeasureSize = 180.0f;
                break;
            case ObserveBean.TYPE_MEASURE_SHEEP://羊
                mMeasureSize = 100.0f;
                break;
            case ObserveBean.TYPE_MEASURE_DOG://狗
                mMeasureSize = 50.0f;
                break;
            case ObserveBean.TYPE_MEASURE_BIRD://鸟
                mMeasureSize = 20.0f;
                break;
        }
        return mMeasureSize;
    }

    public static boolean isScaleMode(int targetMeasureMode) {
        final boolean isScaleFlag = ObserveBean.TYPE_MEASURE_DOG == targetMeasureMode ||
                ObserveBean.TYPE_MEASURE_BIRD == targetMeasureMode;
        return isScaleFlag;
    }
}
