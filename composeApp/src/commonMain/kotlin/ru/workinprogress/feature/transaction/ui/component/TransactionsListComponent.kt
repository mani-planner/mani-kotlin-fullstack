package ru.workinprogress.feature.transaction.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valentinilk.shimmer.shimmer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.workinprogress.feature.main.ui.TransactionDeleteDialog
import ru.workinprogress.feature.main.ui.connectToAppBarState
import ru.workinprogress.feature.transaction.ui.TransactionsViewModel
import ru.workinprogress.feature.transaction.ui.model.TransactionUiItem
import ru.workinprogress.mani.components.MainAppBarState


@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun TransactionsListComponent(
    modifier: Modifier = Modifier,
    appBarState: MainAppBarState = remember { MainAppBarState() },
    onTransactionClicked: (String) -> Unit = {},
) {
    rememberKoinModules {
        listOf(module {
            viewModelOf(::TransactionsViewModel)
        })
    }

    val viewModel = koinViewModel<TransactionsViewModel>()
    val state by viewModel.observe.collectAsStateWithLifecycle()

    TransactionDeleteDialog(
        state.showDeleteDialog,
        viewModel::onDeleteClicked,
        viewModel::onDismissDeleteDialog
    )

    connectToAppBarState(
        state.selectedTransactions,
        appBarState,
        viewModel::onShowDeleteDialogClicked,
        viewModel::onContextMenuClosed
    )
    BoxWithConstraints(contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = modifier.widthIn(max = 640.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            state.data.forEach { day ->
                val (date, list) = day

                TransactionsDay(
                    date = date,
                    list = list,
                    selectedTransactions = state.selectedTransactions,
                    contextMode = appBarState.contextMode,
                    loadingMode = state.loading,
                    onSelected = viewModel::onTransactionSelected,
                    onClick = { onTransactionClicked(it.id) }
                )
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.TransactionsDay(
    date: LocalDate,
    list: ImmutableList<TransactionUiItem>,
    selectedTransactions: ImmutableList<TransactionUiItem>,
    contextMode: Boolean,
    loadingMode: Boolean,
    onSelected: (TransactionUiItem) -> Unit,
    onClick: (TransactionUiItem) -> Unit
) {
    stickyHeader(date.toString()) {
        val loadingModifier = if (loadingMode) {
            Modifier.shimmer()
        } else {
            Modifier
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).then(loadingModifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(vertical = 4.dp, horizontal = 6.dp)
            ) {
                Text(
                    date.format(localDateFormat).takeIf { !loadingMode } ?: "           ",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
    transactionsListItems(
        list,
        selectedTransactions,
        contextMode,
        loadingMode,
        onSelected,
        onClick
    )
}

private fun LazyListScope.transactionsListItems(
    list: ImmutableList<TransactionUiItem>,
    selectedTransactions: ImmutableList<TransactionUiItem>,
    contextMode: Boolean,
    loadingMode: Boolean,
    onSelected: (TransactionUiItem) -> Unit,
    onClick: (TransactionUiItem) -> Unit
) {
    itemsIndexed(list) { index, transaction ->
        TransactionItem(
            Modifier.testTag(if (index == 0) "transaction" else ""),
            transaction,
            transaction in selectedTransactions,
            contextMode,
            loadingMode,
            onSelected,
            onClick,
        )
    }
}

private val localDateFormat = LocalDate.Format {
    dayOfMonth()
    char(' ')
    monthName(MonthNames.ENGLISH_FULL)
    char(' ')
    year()
}

