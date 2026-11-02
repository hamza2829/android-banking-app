package dev.pocketdemo

import dev.pocketdemo.domain.TransferValidation as V
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class IbanTest(private val input: String, private val expected: Boolean) {
    @Test fun countryLengthCharactersAndChecksum() { assertEquals(expected, V.validIban(input)) }
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{index}: {0} -> {1}") fun cases() = listOf(
            arrayOf<Any?>("DE89 3704 0044 0532 0130 00", true),
            arrayOf<Any?>("gb82 west 1234 5698 7654 32", true),
            arrayOf<Any?>("FR1420041010050500013M02606", true),
            arrayOf<Any?>("NL91ABNA0417164300", true),
            arrayOf<Any?>("ES9121000418450200051332", true),
            arrayOf<Any?>("IT60X0542811101000000123456", true),
            arrayOf<Any?>("BE68539007547034", true),
            arrayOf<Any?>("AT611904300234573201", true),
            arrayOf<Any?>("CH9300762011623852957", true),
            arrayOf<Any?>("", false), arrayOf<Any?>("DE88370400440532013000", false),
            arrayOf<Any?>("DE8937040044053201300", false), arrayOf<Any?>("DE893704004405320130000", false),
            arrayOf<Any?>("DE89-370400440532013000", false), arrayOf<Any?>("ZZ89370400440532013000", false),
            arrayOf<Any?>("DEAB370400440532013000", false), arrayOf<Any?>("DE89３70400440532013000", false),
        )
    }
}
@RunWith(Parameterized::class)
class AmountTest(private val input: String, private val expected: Long?) {
    @Test fun parseExactCentsWithinLimits() { assertEquals(expected, V.amountCents(input)) }
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{index}: {0} -> {1}") fun cases() = listOf(
            arrayOf<Any?>("0.01", 1L), arrayOf<Any?>("1", 100L), arrayOf<Any?>("12.3", 1230L),
            arrayOf<Any?>("9999.99", 999999L), arrayOf<Any?>("10000.00", 1000000L),
            arrayOf<Any?>("", null), arrayOf<Any?>("0", null), arrayOf<Any?>("0.00", null),
            arrayOf<Any?>("-1", null), arrayOf<Any?>("10000.01", null), arrayOf<Any?>("100000", null),
            arrayOf<Any?>("1.001", null), arrayOf<Any?>("1,20", null), arrayOf<Any?>("1e2", null),
            arrayOf<Any?>("NaN", null), arrayOf<Any?>(" 1.00", null), arrayOf<Any?>(".50", null), arrayOf<Any?>("1.", null),
        )
    }
}
