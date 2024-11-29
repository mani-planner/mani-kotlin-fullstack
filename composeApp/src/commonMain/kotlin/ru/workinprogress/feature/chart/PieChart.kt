package ru.workinprogress.feature.chart

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.workinprogress.feature.categories.domain.ObserveCategoriesUseCase
import ru.workinprogress.feature.transaction.Category
import ru.workinprogress.feature.transaction.defaultPeriodAppend
import ru.workinprogress.feature.transaction.domain.ObserveTransactionsUseCase
import ru.workinprogress.feature.transaction.simulate
import ru.workinprogress.feature.transaction.sumByPeriod
import ru.workinprogress.mani.today

data class CategoryPieData(val category: Category, val value: Double, val color: Color)
data class CategoriesChartUiState(val data: ImmutableList<Pie> = persistentListOf(), val loading: Boolean)

class CategoriesChartViewModel(
    private val observeTransactionsUseCase: ObserveTransactionsUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
) : ViewModel() {

    private val colors = (0..2).map { mul ->
        listOf(
            Color(0xFFFF5722),
            Color(0xFFFF9800),
            Color(0xFFFBC02D),
            Color(0xFF7CB342),
            Color(0xFF00ACC1),
            Color(0xFF1976D2),
            Color(0xFF9C27B0),
        ).map { color ->
            color.copy(red = color.red - (mul * 2), green = color.green - (mul * 1))
        }
    }.flatten().shuffled()

    private val state = MutableStateFlow(CategoriesChartUiState(loading = true))
    val observe = state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observeCategoriesUseCase.observe,
                observeTransactionsUseCase.observe
            ) { categories, transactions ->
                categories.mapIndexed { index, category ->
                    Pie(
                        category.name,
                        transactions.filter { transaction -> transaction.category == category }
                            .simulate()
                            .sumByPeriod(today(), defaultPeriodAppend(today())),
                        colors[index]
                    )
                }.toImmutableList()
            }.filter { data ->
                data.isNotEmpty()
            }.flowOn(Dispatchers.Default)
                .collectLatest { value ->
                    state.value = CategoriesChartUiState(data = value, loading = false)
                }
        }
    }
}

@Composable
fun PieChart() {
    rememberKoinModules {
        listOf(module {
            viewModelOf(::CategoriesChartViewModel)
        })
    }

    val viewModel = koinViewModel<CategoriesChartViewModel>()
    val uiState by viewModel.observe.collectAsStateWithLifecycle()

    Crossfade(uiState.loading.not()) {
        if (it)
            PieChart(
                modifier = Modifier.size(200.dp),
                data = uiState.data,
                onPieClick = {
                    println("${it.label} Clicked")
//            val pieIndex = data.indexOf(it)
//            data = data.mapIndexed { mapIndex, pie -> pie.copy(selected = pieIndex == mapIndex) }
                },
                selectedScale = 1.2f,
                scaleAnimEnterSpec = spring<Float>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                selectedPaddingDegree = 4f,
                style = Pie.Style.Stroke(width = 20.dp),
                colorAnimEnterSpec = tween(300),
                colorAnimExitSpec = tween(300),
                scaleAnimExitSpec = tween(300),
                spaceDegreeAnimExitSpec = tween(300),
            )
    }

}