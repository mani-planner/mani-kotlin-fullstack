package ru.workinprogress.feature.transaction.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.workinprogress.feature.currency.Currency
import ru.workinprogress.feature.currency.GetCurrentCurrencyUseCase
import ru.workinprogress.feature.transaction.Transaction
import ru.workinprogress.feature.transaction.domain.DeleteTransactionsUseCase
import ru.workinprogress.feature.transaction.domain.GetTransactionsUseCase
import ru.workinprogress.feature.transaction.simulate
import ru.workinprogress.feature.transaction.ui.model.TransactionListUiState
import ru.workinprogress.feature.transaction.ui.model.TransactionUiItem
import ru.workinprogress.mani.emptyImmutableMap
import ru.workinprogress.mani.today
import ru.workinprogress.useCase.UseCase

class TransactionsViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCurrentCurrencyUseCase: GetCurrentCurrencyUseCase,
    private val deleteTransactionsUseCase: DeleteTransactionsUseCase,
) : ViewModel() {

    private val loadingItems by lazy {
        mapOf(
            today() to (0..5).map {
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

    private val state = MutableStateFlow(TransactionListUiState(loading = true, data = loadingItems))
    val observe = state.asStateFlow()

    init {
        load()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun load() {
        viewModelScope.launch {
            state.value = TransactionListUiState(loading = true, data = loadingItems)

            val currency = getCurrentCurrencyUseCase.get()
            when (val result = getTransactionsUseCase()) {
                is UseCase.Result.Error -> {
                    state.value = TransactionListUiState(errorMessage = result.throwable.message.orEmpty())
                }

                is UseCase.Result.Success -> {
                    state.update { state -> state.copy(loading = false, data = emptyImmutableMap()) }

                    result.data.mapLatest { transactions ->
                        transactions.simulate()
                            .filterValues { transactions -> transactions.isNotEmpty() }
                            .filterKeys {
                                today() > it
                            }
                            .mapValues { entry ->
                                entry.value.map { transaction ->
                                    TransactionUiItem(transaction, currency)
                                }.toImmutableList()
                            }
                            .entries
                            .sortedByDescending { it.key }
                            .associate {
                                it.key to it.value
                            }.toImmutableMap()
                    }.flowOn(Dispatchers.Default).collectLatest {
                        state.value = TransactionListUiState(data = it)
                    }
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

    fun onShowDeleteDialogClicked() {
        state.update {
            it.copy(showDeleteDialog = true)
        }
    }

    fun onContextMenuClosed() {
        state.update { state ->
            state.copy(
                selectedTransactions = emptyList<TransactionUiItem>().toImmutableList()
            )
        }
    }

    fun onDismissDeleteDialog() {
        state.update {
            it.copy(showDeleteDialog = false)
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
}