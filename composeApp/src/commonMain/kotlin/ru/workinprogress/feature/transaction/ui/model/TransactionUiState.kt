package ru.workinprogress.feature.transaction.ui.model

import androidx.compose.ui.text.AnnotatedString
import ir.ehsannarmani.compose_charts.extensions.format
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import ru.workinprogress.feature.currency.Currency
import ru.workinprogress.feature.transaction.Transaction
import ru.workinprogress.mani.today

data class TransactionUiState(
    val id: String = "temp",
    val amount: String = "",
    val income: Boolean = true,
    val period: Transaction.Period = Transaction.Period.OneTime,
    val comment: String = "",
    val date: DateDataUiState = DateDataUiState(),
    val until: DateDataUiState = DateDataUiState(),

    val periods: ImmutableList<Transaction.Period> = defaultPeriods,

    val success: Boolean = false,
    val loading: Boolean = false,
    val edit: Boolean = false,

    val errorMessage: String? = null,

    val futureInformation: AnnotatedString = AnnotatedString(""),

    val currency: Currency = Currency("", "", ""),
) {
    val expanded get() = periods != defaultPeriods

    val valid get() = amount.toDoubleOrNull() != null

    val tempTransaction
        get() = Transaction(
            id = id,
            amount = amount.toDoubleOrNull() ?: 0.0,
            income = income,
            period = period,
            date = date.value ?: today(),
            until = until.value,
            comment = comment
        )

    companion object {
        operator fun invoke(transaction: Transaction?, currency: Currency) = transaction?.let {
            TransactionUiState(
                transaction.id,
                transaction.amount.format(0),
                transaction.income,
                transaction.period,
                periods = defaultPeriods,
                comment = transaction.comment,
                date = DateDataUiState(transaction.date),
                until = DateDataUiState(transaction.until),
                currency = currency
            )
        } ?: TransactionUiState()

        private val defaultPeriods = listOf(
            Transaction.Period.OneTime, Transaction.Period.TwoWeek, Transaction.Period.Month
        ).toImmutableList()
    }
}

@Serializable
data class DateDataUiState(val value: LocalDate? = null, val showDatePicker: Boolean = false)