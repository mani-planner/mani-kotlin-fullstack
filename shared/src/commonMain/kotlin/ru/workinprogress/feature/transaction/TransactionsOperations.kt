package ru.workinprogress.feature.transaction

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import ru.workinprogress.feature.chart.ChartResponse
import ru.workinprogress.mani.today
import kotlin.math.sign

const val DEFAULT_PERIOD_VALUE = 3
val DEFAULT_PERIOD_UNIT = DateTimeUnit.MONTH

const val LARGE_PERIOD_VALUE = 1
val LARGE_PERIOD_UNIT = DateTimeUnit.YEAR


fun createDates(from: LocalDate, to: LocalDate): List<LocalDate> {
    return buildList {
        var currentDate = from
        while (currentDate < to) {
            add(currentDate)
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }
    }
}

fun List<Transaction>.defaultPeriod(): Pair<LocalDate, LocalDate> {
    val from = this.minOfOrNull { transaction -> transaction.date } ?: today()
    val to = defaultPeriodAppend(today())
    return from to to
}

fun defaultPeriodAppend(date: LocalDate) = date.plus(DEFAULT_PERIOD_VALUE, DEFAULT_PERIOD_UNIT)
fun largePeriodAppend(date: LocalDate) = date.plus(LARGE_PERIOD_VALUE, LARGE_PERIOD_UNIT)

fun Map<LocalDate, List<Transaction>>.sumByMonth(monthDate: LocalDate): Double =
    this
        .filter {
            it.key.monthNumber == monthDate.monthNumber &&
                    it.key.year == monthDate.year
        }.flatMap { it.value }.sumOf { it.amountSigned }

fun Map<LocalDate, List<Transaction>>.sumByPeriod(from: LocalDate, to: LocalDate): Double =
    this
        .filter {
            it.key >= from && it.key < to
        }.flatMap { it.value }.sumOf { it.amount }

fun List<Transaction>.toChartInternal(): ChartResponse {
    if (isEmpty()) return ChartResponse.Empty

    val (from, to) = defaultPeriod()
    val simulated = simulate(from, to)
    val chartData = simulated.entries.runningFold(from to 0.toDouble()) { acc, list ->
        list.key to acc.second + list.value.sumOf { transaction ->
            transaction.amountSigned
        }
    }.toMap()

    return ChartResponse(days = chartData, from, to)
}

fun List<Transaction>.simulate(
    dates: Pair<LocalDate, LocalDate> = defaultPeriod()
): Map<LocalDate, List<Transaction>> {
    val (from, to) = dates
    return simulate(from, to)
}

fun List<Transaction>.simulate(from: LocalDate, to: LocalDate): Map<LocalDate, List<Transaction>> {
    val scheduled = mutableMapOf<LocalDate, List<Transaction>>()

    createDates(from, to).forEach { currentDate ->
        val scheduledForDate = mutableListOf<Transaction>()

        val currentTransactions = this.filter { transaction ->
            transaction.date == currentDate
        }

        val nextTransactions = scheduled[currentDate].orEmpty()

        (currentTransactions + nextTransactions).forEach { transaction ->
            scheduledForDate.add(transaction)

            scheduled.scheduleTransaction(
                transaction, to,
                when (transaction.period) {
                    Transaction.Period.Day -> currentDate.plus(1, DateTimeUnit.DAY)
                    Transaction.Period.OneTime -> currentDate
                    Transaction.Period.Week -> currentDate.plus(1, DateTimeUnit.WEEK)
                    Transaction.Period.TwoWeek -> currentDate.plus(2, DateTimeUnit.WEEK)
                    Transaction.Period.Month -> currentDate.plus(1, DateTimeUnit.MONTH)
                    Transaction.Period.ThreeMonth -> currentDate.plus(1, DateTimeUnit.QUARTER)
                    Transaction.Period.HalfYear -> currentDate.plus(2, DateTimeUnit.QUARTER)
                    Transaction.Period.Year -> currentDate.plus(1, DateTimeUnit.YEAR)
                }
            )
        }

        scheduled[currentDate] = scheduledForDate
    }

    return scheduled.toList().sortedBy { it.first }.toMap()
}

//searches for dates on which the balance sign changes
fun Map<LocalDate, List<Transaction>>.findZeroEvents(): Pair<LocalDate?, LocalDate?> {
    var positiveDate: LocalDate? = null
    var negativeDate: LocalDate? = null

    entries.runningFoldIndexed(
        0.toDouble(),
        { index, acc, item ->
            val nextValue = acc + item.value.sumOf { transaction ->
                transaction.amountSigned
            }

            if (index == 0) return@runningFoldIndexed nextValue

            val alreadyChangeSign = positiveDate != null || negativeDate != null
            if (nextValue.sign != acc.sign && (acc != 0.0 || alreadyChangeSign)) {
                if (nextValue.sign > acc.sign) {
                    positiveDate = item.value.first().date
                } else {
                    negativeDate = item.value.first().date
                }
            }

            if (positiveDate != null && negativeDate != null) {
                return positiveDate to negativeDate
            }

            nextValue
        })

    return positiveDate to negativeDate
}

private fun MutableMap<LocalDate, List<Transaction>>.scheduleTransaction(
    transaction: Transaction,
    to: LocalDate,
    nextDate: LocalDate
) {
    if ((transaction.until == null || transaction.until >= nextDate) && to > nextDate) {
        this[nextDate] = (this[nextDate]?.toMutableList() ?: mutableListOf()).apply {
            this.add(transaction)
        }
    }
}
