package com.topdon.module.thermal.utils

/**
 * Array utility class for thermal imaging operations.
 * Provides helper functions and common functionality.
 */
object ArrayUtils {
    /**
getmatrixdata里的最大值的序列(在选取region内)-rotationmatrix
@param rotateType 1:rotation90 2:rotation180  3:rotation270
     */
    fun getMaxIndex(
        data: FloatArray,
        rotateType: Int = 0,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        val index =
            when (rotateType) {
                1, 2, 3 -> getRotateMaxIndex(data, rotateType, selectIndexList)
                else -> getMaxIndex(data, selectIndexList)
            }
        return index
    }

    /**
getmatrixdata里的最小值的序列(在选取region内)-rotationmatrix
@param rotateType 1:rotation90 2:rotation180  3:rotation270
     */
    fun getMinIndex(
        data: FloatArray,
        rotateType: Int = 0,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        val index =
            when (rotateType) {
                1, 2, 3 -> getRotateMinIndex(data, rotateType, selectIndexList)
                else -> getMinIndex(data, selectIndexList)
            }
        return index
    }

    /**
rotationmatrix
@param rotateType 1:rotation90 2:rotation180  3:rotation270
     */
    fun matrixRotate(
        srcData: FloatArray,
        rotateType: Int = 0,
    ): FloatArray {
        return when (rotateType) {
            1 -> matrixRotate90(srcData)
            2 -> matrixRotate180(srcData)
            3 -> matrixRotate270(srcData)
            else -> srcData
        }
    }

    /**
getmatrixdata里的最大值的序列(在选取region内)-原matrix
     */
    private fun getMaxIndex(
        data: FloatArray,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        if (selectIndexList.size == 0) {
无指定region
            var maxIndex = 0
            for (i in 1 until data.size - 1) {
                if (data[i] > data[maxIndex]) {
                    maxIndex = i
                }
            }
            return maxIndex
        } else {
            val selectPoint = FloatArray(selectIndexList.size)
            for (i in 0 until selectIndexList.size) {
                selectPoint[i] = data[selectIndexList[i]]
            }
            var maxIndex = 0
            for (i in 1 until selectPoint.size - 1) {
                if (selectPoint[i] > selectPoint[maxIndex]) {
                    maxIndex = i
                }
            }
            return selectIndexList[maxIndex]
        }
    }

    /**
getmatrixdata里的最小值的序列(在选取region内)-原matrix
     */
    private fun getMinIndex(
        data: FloatArray,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        if (selectIndexList.size == 0) {
            var minIndex = 0
            for (i in 1 until data.size - 1) {
                if (data[i] == 0f) {
                    continue
                }
                if (data[i] < data[minIndex]) {
                    minIndex = i
                }
            }
            return minIndex
        } else {
            val selectPoint = FloatArray(selectIndexList.size)
            for (i in 0 until selectIndexList.size) {
                selectPoint[i] = data[selectIndexList[i]]
            }
            var minIndex = 0
            for (i in 1 until selectPoint.size - 1) {
                if (selectPoint[i] == 0f) {
                    continue
                }
                if (selectPoint[i] < selectPoint[minIndex]) {
                    minIndex = i
                }
            }
            return selectIndexList[minIndex]
        }
    }

    /**
getmatrixdata里的最大值的序列(在选取region内)-rotationmatrix
@param rotateType 1:rotation90 2:rotation180  3:rotation270
     */
    private fun getRotateMaxIndex(
        data: FloatArray,
        rotateType: Int = 0,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        if (selectIndexList.size == 0) {
            val destData = matrixRotate(data, rotateType)
            var maxIndex = 0
            for (i in 1 until destData.size - 1) {
                if (destData[i] > destData[maxIndex]) {
                    maxIndex = i
                }
            }
            return maxIndex
        } else {
            val destData = matrixRotate(data, rotateType)
            val selectPoint = FloatArray(selectIndexList.size)
            for (i in 0 until selectIndexList.size) {
                selectPoint[i] = destData[selectIndexList[i]]
            }
            var maxIndex = 0
            for (i in 1 until selectPoint.size - 1) {
                if (selectPoint[i] > selectPoint[maxIndex]) {
                    maxIndex = i
                }
            }
            return selectIndexList[maxIndex]
        }
    }

    /**
getmatrixdata里的最小值的序列(在选取region内)-rotationmatrix
@param rotateType 1:rotation90 2:rotation180  3:rotation270
     */
    private fun getRotateMinIndex(
        data: FloatArray,
        rotateType: Int = 0,
        selectIndexList: ArrayList<Int> = arrayListOf(),
    ): Int {
        if (selectIndexList.size == 0) {
            val destData = matrixRotate(data, rotateType)
            var minIndex = 0
            for (i in 1 until destData.size - 1) {
                if (destData[i] == 0f) {
                    continue
                }
                if (destData[i] < destData[minIndex]) {
                    minIndex = i
                }
            }
            return minIndex
        } else {
            val destData = matrixRotate(data, rotateType)
            val selectPoint = FloatArray(selectIndexList.size)
            for (i in 0 until selectIndexList.size) {
                selectPoint[i] = destData[selectIndexList[i]]
            }
            var minIndex = 0
            for (i in 1 until selectPoint.size - 1) {
                if (selectPoint[i] == 0f) {
                    continue
                }
                if (selectPoint[i] < selectPoint[minIndex]) {
                    minIndex = i
                }
            }
            return selectIndexList[minIndex]
        }
    }

    /**
matrix顺时针rotation90°
     */
    private fun matrixRotate90(srcData: FloatArray): FloatArray {
        val row = 192
        val column = 256
        val srcMatrix = Array(row) { FloatArray(column) }
        for (i in 0 until row) {
            for (j in 0 until column) {
                srcMatrix[i][j] = srcData[i * column + j]
            }
        }
        val destMatrix = Array(column) { FloatArray(row) }
        for (x in 0 until column) {
            for (y in 0 until row) {
                destMatrix[x][y] = srcMatrix[row - 1 - y][x] // 矩阵旋转90度
            }
        }
        val data = FloatArray(srcData.size)
        for (i in destMatrix.indices) {
            for (j in destMatrix[i].indices) {
                data[destMatrix[0].size * i + j] = destMatrix[i][j]
            }
        }
        return data
    }

    /**
matrix顺时针rotation180°
     */
    private fun matrixRotate180(srcData: FloatArray): FloatArray {
        val row = 192
        val column = 256
        val srcMatrix = Array(row) { FloatArray(column) }
        for (i in 0 until row) {
            for (j in 0 until column) {
                srcMatrix[i][j] = srcData[i * column + j]
            }
        }
        val destMatrix = Array(row) { FloatArray(column) }
        for (x in 0 until row) {
            for (y in 0 until column) {
                destMatrix[x][y] = srcMatrix[row - 1 - x][column - 1 - y] // 矩阵旋转180度
            }
        }
        val data = FloatArray(srcData.size)
        for (i in destMatrix.indices) {
            for (j in destMatrix[i].indices) {
                data[destMatrix[0].size * i + j] = destMatrix[i][j]
            }
        }
        return data
    }

    /**
matrix顺时针rotation270°
getrotation后的arraydata
     */
    private fun matrixRotate270(srcData: FloatArray): FloatArray {
        val row = 192
        val column = 256
        val srcMatrix = Array(row) { FloatArray(column) } // 源矩阵
        for (i in 0 until row) {
            for (j in 0 until column) {
                srcMatrix[i][j] = srcData[i * column + j]
            }
        }
        val destMatrix = Array(column) { FloatArray(row) } // 目标矩阵
        for (x in 0 until column) {
            for (y in 0 until row) {
                destMatrix[x][y] = srcMatrix[y][column - 1 - x] // 矩阵旋转270度
            }
        }
        val data = FloatArray(srcData.size)
        for (i in destMatrix.indices) {
            for (j in destMatrix[i].indices) {
                data[destMatrix[0].size * i + j] = destMatrix[i][j]
            }
        }
        return data
    }
}
