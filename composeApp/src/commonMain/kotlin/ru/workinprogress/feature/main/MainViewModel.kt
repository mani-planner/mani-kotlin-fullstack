package ru.workinprogress.feature.main

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.plus
import ru.workinprogress.feature.auth.domain.LogoutUseCase
import ru.workinprogress.feature.categories.domain.GetCategoriesUseCase
import ru.workinprogress.feature.currency.Currency
import ru.workinprogress.feature.currency.GetCurrentCurrencyUseCase
import ru.workinprogress.feature.main.ui.FiltersState
import ru.workinprogress.feature.main.ui.MainUiState
import ru.workinprogress.feature.transaction.*
import ru.workinprogress.feature.transaction.domain.DeleteTransactionsUseCase
import ru.workinprogress.feature.transaction.domain.GetTransactionsUseCase
import ru.workinprogress.feature.transaction.ui.model.NegativeColor
import ru.workinprogress.feature.transaction.ui.model.PositiveColor
import ru.workinprogress.feature.transaction.ui.model.TransactionUiItem
import ru.workinprogress.feature.transaction.ui.model.buildColoredAmount
import ru.workinprogress.mani.emptyImmutableMap
import ru.workinprogress.mani.today
import ru.workinprogress.useCase.UseCase


class MainViewModel(
    private val transactionsUseCase: GetTransactionsUseCase,
    private val deleteTransactionsUseCase: DeleteTransactionsUseCase,
    private val getCurrencyUseCase: GetCurrentCurrencyUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val loadingItems by lazy {
        mapOf(
            today() to (0..2).map {
                TransactionUiItem(
                    it.toString(),
                    0.0,
                    false,
                    date = today(),
                    until = null,
                    period = Transaction.Period.OneTime,
                    comment = "Loading",
                    currency = Currency.Usd
                )
            }.toImmutableList()
        ).toImmutableMap()
    }

    private val state = MutableStateFlow(MainUiState(loading = true, transactions = loadingItems))

    private val filterUpcoming = MutableStateFlow(true)
    private val filterCategory = MutableStateFlow<Category?>(null)

    val observe = state.asStateFlow()

    private lateinit var currency: Currency

    init {
        viewModelScope.launch { load() }
        viewModelScope.launch {
            currency = getCurrencyUseCase.get()
        }

    }

    private suspend fun load() {
        state.value = MainUiState(loading = true, transactions = loadingItems)
        when (val result = transactionsUseCase()) {
            is UseCase.Result.Error -> {
                state.value = MainUiState(errorMessage = result.throwable.message)
            }

            is UseCase.Result.Success -> {
                state.update { state -> state.copy(loading = true, transactions = emptyImmutableMap()) }

                combine(
                    result.data,
                    getCategoriesUseCase.get(),
                    filterUpcoming,
                    filterCategory
                ) { transactions, categories, upcoming, category ->
                    val simulationResult = transactions.simulate()

                    MainUiState(
                        loading = false,
                        filtersState = FiltersState(
                            categories = (categories + Category.default).toImmutableSet(),
                            upcoming = upcoming,
                            category = category,
                            loading = false,
                        ),
                        transactions = simulationResult
                            .filterKeys {
                                if (upcoming) {
                                    today() <= it
                                } else {
                                    today() > it
                                }
                            }
                            .mapValues {
                                it.value.filter {
                                    category == null || category == it.category
                                }
                            }
                            .filterValues { transactions -> transactions.isNotEmpty() }
                            .mapValues { entry ->
                                entry.value.map { transaction ->
                                    TransactionUiItem(transaction, currency)
                                }.toImmutableList()
                            }
                            .entries
                            .run {
                                if (upcoming) {
                                    sortedBy { it.key }
                                } else {
                                    sortedByDescending { it.key }
                                }
                            }
                            .associate { it.key to it.value }.toImmutableMap(),
                        futureInformation = buildFutureInformation(simulationResult)
                    )
                }.flowOn(Dispatchers.Default)
                    .collectLatest { result: MainUiState ->
                        state.update { result }
                    }
            }
        }
    }

    fun onTransactionSelected(transactionUiItem: TransactionUiItem) {
        if (transactionUiItem in state.value.selectedTransactions) {
            state.update { state ->
                state.copy(
                    selectedTransactions = (state.selectedTransactions - transactionUiItem).toImmutableList()
                )
            }
        } else {
            state.update { state ->
                state.copy(
                    selectedTransactions = (state.selectedTransactions + transactionUiItem).toImmutableList()
                )
            }
        }
    }

    fun onDeleteClicked() {
        viewModelScope.launch {
            val selected = state.value.selectedTransactions.map { it.id }
            state.update { state ->
                state.copy(
                    showDeleteDialog = false,
                    selectedTransactions = emptyList<TransactionUiItem>().toImmutableList()
                )
            }

            deleteTransactionsUseCase(selected)
        }
    }

    fun onContextMenuClosed() {
        state.update { state ->
            state.copy(
                selectedTransactions = emptyList<TransactionUiItem>().toImmutableList()
            )
        }
    }

    fun onShowDeleteDialogClicked() {
        state.update {
            it.copy(showDeleteDialog = true)
        }
    }

    fun onDismissDeleteDialog() {
        state.update {
            it.copy(showDeleteDialog = false)
        }
    }


    private fun buildFutureInformation(simulationResult: Map<LocalDate, List<Transaction>>) =
        buildAnnotatedString {

            val localDateFormat = LocalDate.Format {
                dayOfMonth()
                char(' ')
                monthName(MonthNames.ENGLISH_ABBREVIATED)
                char(' ')
                year()
            }
            val filteredTransactions =
                simulationResult.filterValues { transactions -> transactions.isNotEmpty() }
                    .filterKeys { today() <= it }

            val todayAmount = simulationResult.entries
                .runningFold(0.0) { acc, entry ->
                    if (entry.key > today()) acc
                    else acc + entry.value.sumOf { it.amountSigned }
                }.last()

            val firstOfTheFirst = filteredTransactions.entries.firstOrNull()?.value?.firstOrNull()

            append("balance: ")
            append(buildColoredAmount(todayAmount, currency))
            append("\n")
            append(
                "today balance change: "
            )
            append(buildColoredAmount(filteredTransactions.filter { it.key == today() }.entries.flatMap { it.value }
                .sumOf { it.amountSigned }, currency))
            append("\n")

            firstOfTheFirst?.let {
                append("next transaction ${firstOfTheFirst.date.format(localDateFormat)}: ")
                append(buildColoredAmount(firstOfTheFirst.amountSigned, currency))
                append("\n")
            }

            append(
                "in month: "
            )
            append(
                buildColoredAmount(
                    simulationResult.sumByMonth(today()),
                    currency
                )
            )
            append(
                ", in next month: "
            )
            append(
                buildColoredAmount(
                    simulationResult.sumByMonth(today().plus(1, DateTimeUnit.MONTH)), currency
                )
            )
            append("\n")

            val (positiveDate, negativeDate) = simulationResult.findZeroEvents()

            when {
                (todayAmount > 0 && negativeDate != null) -> {
                    append("balance will become ")

                    withStyle(style = SpanStyle(color = NegativeColor)) {
                        append("negative: ")
                    }

                    append(negativeDate.format(localDateFormat))
                }

                (todayAmount < 0 && positiveDate != null) -> {
                    append("balance will become ")

                    withStyle(style = SpanStyle(color = PositiveColor)) {
                        append("positive: ")
                    }

                    append(positiveDate.format(localDateFormat))
                }

                else -> {
                    append("no zero events")
                }
            }
        }

    fun onProfileClicked() {
        state.update { state ->
            state.copy(showProfile = true)
        }
    }

    fun onProfileDismiss() {
        state.update { state ->
            state.copy(showProfile = false)
        }
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            logoutUseCase()
            state.value = MainUiState()
        }
    }

    fun onUpcomingToggle(bool: Boolean) {
        filterUpcoming.value = bool

    }

    fun onCategorySelected(category: Category?) {
        filterCategory.value = category
    }

}

