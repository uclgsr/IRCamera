/*
 * Copyright (c) 2016-present 贵州纳雍穿青human李裕江<1032694760@qq.com>
 *
 * The software is licensed under the Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *     http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.github.gzuliyujiang.wheelpicker.contract;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * 提供二级或三级联动data
 *
 * @author 贵州山野羡民（1032694760@qq.com）
 * @since 2019/6/17 11:27
 */
public interface LinkageProvider {
    int INDEX_NO_FOUND = -1;

    /**
     * 是否展示第一级
     *
     * @return Returntrue表示展示第一级
     */
    boolean firstLevelVisible();

    /**
     * 是否展示第三级
     *
     * @return Returntrue表示展示第三级
     */
    boolean thirdLevelVisible();

    /**
     * 提供第一级data
     *
     * @return 第一级data
     */
    @NonNull
    List<?> provideFirstData();

    /**
     * 根据第一级data联动第二级data
     *
     * @param firstIndex 第一级dataindex
     * @return 第二级data
     */
    @NonNull
    List<?> linkageSecondData(int firstIndex);

    /**
     * 根据第一二级data联动第三级data
     *
     * @param firstIndex  第一级dataindex
     * @param secondIndex 第二级dataindex
     * @return 第三级data
     */
    @NonNull
    List<?> linkageThirdData(int firstIndex, int secondIndex);

    /**
     * 根据第一data值查找其index
     *
     * @param firstValue 第一级data值
     * @return 第一级dataindex
     */
    int findFirstIndex(Object firstValue);

    /**
     * 根据第二data值查找其index
     *
     * @param firstIndex  第一级dataindex
     * @param secondValue 第二级data值
     * @return 第二级dataindex
     */
    int findSecondIndex(int firstIndex, Object secondValue);

    /**
     * 根据第三data值查找其index
     *
     * @param firstIndex  第一级dataindex
     * @param secondIndex 第二级dataindex
     * @param thirdValue  第三级data值
     * @return 第三级dataindex
     */
    int findThirdIndex(int firstIndex, int secondIndex, Object thirdValue);

}