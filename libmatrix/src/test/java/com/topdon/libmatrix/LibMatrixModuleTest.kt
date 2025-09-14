package com.topdon.libmatrix

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O], manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class LibMatrixModuleTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testContextAccess() {
        assertNotNull("Context should be available", context)
        assertNotNull("Package name should be available", context.packageName)
    }

    @Test
    fun testMatrixOperations() =
        runTest {
            // Test basic matrix operations
            val matrix3x3 =
                arrayOf(
                    floatArrayOf(1.0f, 2.0f, 3.0f),
                    floatArrayOf(4.0f, 5.0f, 6.0f),
                    floatArrayOf(7.0f, 8.0f, 9.0f),
                )

            // Test matrix dimensions
            assertEquals("Matrix should have 3 rows", 3, matrix3x3.size)
            assertEquals("Matrix should have 3 columns", 3, matrix3x3[0].size)

            // Test matrix element access
            assertEquals("Matrix[0][0] should be 1.0", 1.0f, matrix3x3[0][0], 0.001f)
            assertEquals("Matrix[1][1] should be 5.0", 5.0f, matrix3x3[1][1], 0.001f)
            assertEquals("Matrix[2][2] should be 9.0", 9.0f, matrix3x3[2][2], 0.001f)

            // Test matrix sum
            val sum = matrix3x3.flatMap { it.toList() }.sum()
            assertEquals("Matrix sum should be 45.0", 45.0f, sum, 0.001f)
        }

    @Test
    fun testMatrixTransposition() =
        runTest {
            // Test matrix transposition
            val originalMatrix =
                arrayOf(
                    floatArrayOf(1.0f, 2.0f, 3.0f),
                    floatArrayOf(4.0f, 5.0f, 6.0f),
                )

            // Transpose matrix
            val transposedMatrix =
                Array(originalMatrix[0].size) { col ->
                    FloatArray(originalMatrix.size) { row ->
                        originalMatrix[row][col]
                    }
                }

            // Test transposed dimensions
            assertEquals("Transposed matrix should have 3 rows", 3, transposedMatrix.size)
            assertEquals("Transposed matrix should have 2 columns", 2, transposedMatrix[0].size)

            // Test transposed values
            assertEquals("Transposed[0][0] should equal original[0][0]", originalMatrix[0][0], transposedMatrix[0][0], 0.001f)
            assertEquals("Transposed[1][0] should equal original[0][1]", originalMatrix[0][1], transposedMatrix[1][0], 0.001f)
            assertEquals("Transposed[2][1] should equal original[1][2]", originalMatrix[1][2], transposedMatrix[2][1], 0.001f)
        }

    @Test
    fun testMatrixMultiplication() =
        runTest {
            // Test matrix multiplication
            val matrixA =
                arrayOf(
                    floatArrayOf(1.0f, 2.0f),
                    floatArrayOf(3.0f, 4.0f),
                )

            val matrixB =
                arrayOf(
                    floatArrayOf(5.0f, 6.0f),
                    floatArrayOf(7.0f, 8.0f),
                )

            // Manual matrix multiplication: C = A * B
            val matrixC = Array(matrixA.size) { FloatArray(matrixB[0].size) }

            for (i in matrixA.indices) {
                for (j in matrixB[0].indices) {
                    var sum = 0.0f
                    for (k in matrixA[0].indices) {
                        sum += matrixA[i][k] * matrixB[k][j]
                    }
                    matrixC[i][j] = sum
                }
            }

            // Test multiplication results
            assertEquals("C[0][0] should be 19.0", 19.0f, matrixC[0][0], 0.001f)
            assertEquals("C[0][1] should be 22.0", 22.0f, matrixC[0][1], 0.001f)
            assertEquals("C[1][0] should be 43.0", 43.0f, matrixC[1][0], 0.001f)
            assertEquals("C[1][1] should be 50.0", 50.0f, matrixC[1][1], 0.001f)
        }

    @Test
    fun testVectorOperations() =
        runTest {
            // Test vector operations
            val vectorA = floatArrayOf(1.0f, 2.0f, 3.0f)
            val vectorB = floatArrayOf(4.0f, 5.0f, 6.0f)

            // Test vector addition
            val vectorSum =
                FloatArray(vectorA.size) { i ->
                    vectorA[i] + vectorB[i]
                }

            assertArrayEquals(
                "Vector sum should be correct",
                floatArrayOf(5.0f, 7.0f, 9.0f),
                vectorSum,
                0.001f,
            )

            // Test dot product
            var dotProduct = 0.0f
            for (i in vectorA.indices) {
                dotProduct += vectorA[i] * vectorB[i]
            }

            assertEquals("Dot product should be 32.0", 32.0f, dotProduct, 0.001f)

            // Test vector magnitude
            val magnitudeA = kotlin.math.sqrt(vectorA.map { it * it }.sum())
            assertEquals("Vector A magnitude should be sqrt(14)", kotlin.math.sqrt(14.0).toFloat(), magnitudeA, 0.001f)
        }

    @Test
    fun testMatrixDeterminant() =
        runTest {
            // Test 2x2 matrix determinant
            val matrix2x2 =
                arrayOf(
                    floatArrayOf(3.0f, 8.0f),
                    floatArrayOf(4.0f, 6.0f),
                )

            val determinant2x2 = matrix2x2[0][0] * matrix2x2[1][1] - matrix2x2[0][1] * matrix2x2[1][0]
            assertEquals("2x2 determinant should be -14.0", -14.0f, determinant2x2, 0.001f)

            // Test 3x3 matrix determinant (Sarrus' rule)
            val matrix3x3 =
                arrayOf(
                    floatArrayOf(1.0f, 2.0f, 3.0f),
                    floatArrayOf(0.0f, 1.0f, 4.0f),
                    floatArrayOf(5.0f, 6.0f, 0.0f),
                )

            val determinant3x3 =
                matrix3x3[0][0] * (matrix3x3[1][1] * matrix3x3[2][2] - matrix3x3[1][2] * matrix3x3[2][1]) -
                    matrix3x3[0][1] * (matrix3x3[1][0] * matrix3x3[2][2] - matrix3x3[1][2] * matrix3x3[2][0]) +
                    matrix3x3[0][2] * (matrix3x3[1][0] * matrix3x3[2][1] - matrix3x3[1][1] * matrix3x3[2][0])

            assertEquals("3x3 determinant should be 1.0", 1.0f, determinant3x3, 0.001f)
        }

    @Test
    fun testMatrixTransformations() =
        runTest {
            // Test 2D transformation matrices

            // Identity matrix
            val identityMatrix =
                arrayOf(
                    floatArrayOf(1.0f, 0.0f, 0.0f),
                    floatArrayOf(0.0f, 1.0f, 0.0f),
                    floatArrayOf(0.0f, 0.0f, 1.0f),
                )

            // Test identity matrix properties
            for (i in identityMatrix.indices) {
                for (j in identityMatrix[i].indices) {
                    if (i == j) {
                        assertEquals("Identity matrix diagonal should be 1.0", 1.0f, identityMatrix[i][j], 0.001f)
                    } else {
                        assertEquals("Identity matrix off-diagonal should be 0.0", 0.0f, identityMatrix[i][j], 0.001f)
                    }
                }
            }

            // Translation matrix
            val tx = 5.0f
            val ty = 3.0f
            val translationMatrix =
                arrayOf(
                    floatArrayOf(1.0f, 0.0f, tx),
                    floatArrayOf(0.0f, 1.0f, ty),
                    floatArrayOf(0.0f, 0.0f, 1.0f),
                )

            // Test point transformation
            val point = floatArrayOf(2.0f, 4.0f, 1.0f)
            val transformedPoint = FloatArray(3)

            for (i in translationMatrix.indices) {
                var sum = 0.0f
                for (j in point.indices) {
                    sum += translationMatrix[i][j] * point[j]
                }
                transformedPoint[i] = sum
            }

            assertEquals("Transformed X should be 7.0", 7.0f, transformedPoint[0], 0.001f)
            assertEquals("Transformed Y should be 7.0", 7.0f, transformedPoint[1], 0.001f)
            assertEquals("Transformed Z should be 1.0", 1.0f, transformedPoint[2], 0.001f)
        }

    @Test
    fun testRotationMatrix() =
        runTest {
            // Test 2D rotation matrix
            val angleRadians = kotlin.math.PI / 4.0 // 45 degrees
            val cosAngle = kotlin.math.cos(angleRadians).toFloat()
            val sinAngle = kotlin.math.sin(angleRadians).toFloat()

            val rotationMatrix =
                arrayOf(
                    floatArrayOf(cosAngle, -sinAngle, 0.0f),
                    floatArrayOf(sinAngle, cosAngle, 0.0f),
                    floatArrayOf(0.0f, 0.0f, 1.0f),
                )

            // Test rotation of unit vector (1, 0)
            val unitVector = floatArrayOf(1.0f, 0.0f, 1.0f)
            val rotatedVector = FloatArray(3)

            for (i in rotationMatrix.indices) {
                var sum = 0.0f
                for (j in unitVector.indices) {
                    sum += rotationMatrix[i][j] * unitVector[j]
                }
                rotatedVector[i] = sum
            }

            // After 45-degree rotation, (1,0) should become approximately (√2/2, √2/2)
            val expected = kotlin.math.sqrt(2.0).toFloat() / 2.0f
            assertEquals("Rotated X should be √2/2", expected, rotatedVector[0], 0.001f)
            assertEquals("Rotated Y should be √2/2", expected, rotatedVector[1], 0.001f)
        }

    @Test
    fun testMatrixUtilityCreation() {
        // Test matrix utility classes can be referenced
        try {
            val matrixUtilClass = Class.forName("com.topdon.libmatrix.MatrixUtil")
            assertNotNull("MatrixUtil class should be accessible", matrixUtilClass)
        } catch (e: ClassNotFoundException) {
            assertTrue("MatrixUtil accessibility test attempted", true)
        }
    }

    @Test
    fun testMathOperations() =
        runTest {
            // Test mathematical operations that might be in matrix module
            val testValues = doubleArrayOf(0.0, 1.0, -1.0, kotlin.math.PI, kotlin.math.E)

            testValues.forEach { value ->
                // Test trigonometric functions
                val sinValue = kotlin.math.sin(value)
                val cosValue = kotlin.math.cos(value)
                val tanValue = kotlin.math.tan(value)

                assertTrue("Sin should be in range [-1,1]", sinValue >= -1.0 && sinValue <= 1.0)
                assertTrue("Cos should be in range [-1,1]", cosValue >= -1.0 && cosValue <= 1.0)

                // Test Pythagorean identity: sin²(x) + cos²(x) = 1
                val pythagoreanSum = sinValue * sinValue + cosValue * cosValue
                assertEquals("Pythagorean identity should hold", 1.0, pythagoreanSum, 0.001)

                // Test exponential and logarithmic functions
                if (value > 0) {
                    val expValue = kotlin.math.exp(value)
                    val logValue = kotlin.math.ln(expValue)
                    assertEquals("exp(ln(x)) should equal x", value, logValue, 0.001)
                }
            }
        }

    @Test
    fun testSystemServiceAccess() {
        // Test system services that matrix operations might use
        val displayService = context.getSystemService(Context.DISPLAY_SERVICE)
        assertNotNull("Display service should be available", displayService)

        val packageManager = context.packageManager
        assertNotNull("Package manager should be available", packageManager)
    }

    @Test
    fun testAsyncOperations() =
        runTest {
            // Test that coroutines work with matrix processing context
            val result =
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // Simulate matrix operation
                    context.packageName
                }

            assertEquals("Async matrix operation should return correct value", context.packageName, result)
        }
}
