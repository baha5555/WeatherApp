package com.example.weatherapp

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    class MyBusinessLogic {
        fun addNumbers(a: Int, b: Int): Int {
            return a + b
        }
    }

    @Test
    fun testAddNumbers() {
        val myBusinessLogic = MyBusinessLogic()
        val result = myBusinessLogic.addNumbers(2, 3)
    }
}