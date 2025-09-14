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

public interface LinkageProvider {
    int INDEX_NO_FOUND = -1;

    boolean firstLevelVisible();

    boolean thirdLevelVisible();

    @NonNull
    List<?> provideFirstData();

    @NonNull
    List<?> linkageSecondData(int firstIndex);

    @NonNull
    List<?> linkageThirdData(int firstIndex, int secondIndex);

    int findFirstIndex(Object firstValue);

    int findSecondIndex(int firstIndex, Object secondValue);

    int findThirdIndex(int firstIndex, int secondIndex, Object thirdValue);

}
