package ru.workinprogress.feature.transaction.ui.model

import androidx.compose.ui.text.AnnotatedString
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
    val periods: ImmutableList<Transaction.Period> = defaultPeriods,
    val comment: String = "",
    val date: DateDataUiState = DateDataUiState(),
    val until: DateDataUiState = DateDataUiState(),
    val success: Boolean = false,
    val futureInformation: AnnotatedString = AnnotatedString(""),
    val currency: Currency = Currency("", "", ""),
    val edit: Boolean = false
) {

    val expanded
        get() = periods != defaultPeriods

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
                transaction.amount.toString(),
                transaction.income,
                transaction.period,
                defaultPeriods,
                transaction.comment,
                DateDataUiState(transaction.date),
                DateDataUiState(transaction.until),
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