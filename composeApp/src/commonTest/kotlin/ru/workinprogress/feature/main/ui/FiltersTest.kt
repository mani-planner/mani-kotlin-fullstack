@file:OptIn(ExperimentalTestApi::class)

package ru.workinprogress.feature.main.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.*
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import ru.workinprogress.feature.chart.ui.ChartComponent
import ru.workinprogress.feature.chart.ui.model.ChartUi
import ru.workinprogress.feature.transaction.Category
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FiltersTest {

    @Test
    fun filterChipsTest() = runComposeUiTest {
        val targetCategory = Category("1", "Test1")
        val stateFlow = MutableStateFlow(
            FiltersState(
                categories = persistentSetOf(Category("0", "Test0"), targetCategory),
                loading = true
            )
        )

        setContent {
            val state = stateFlow.collectAsState()

            FiltersChips(
                filtersState = state.value,
                onUpcomingToggle = {
                    stateFlow.update { state ->
                        state.copy(upcoming = it)
                    }
                }) {
                stateFlow.update { state ->
                    state.copy(category = it)
                }
            }
        }

        onNodeWithTag("filtersShimmer").assertIsDisplayed()

        stateFlow.update { state ->
            state.copy(loading = false)
        }
        onNodeWithText("Upcoming").assertIsDisplayed()
        onNodeWithText("All categories").assertIsDisplayed()

        onNodeWithText("Upcoming").performClick()
        onNodeWithText("Past").assertIsDisplayed()

        onNodeWithText("Past").performClick()
        assertFalse(stateFlow.value.upcoming)
        onNodeWithText("All categories").performClick()
        onNodeWithText(targetCategory.name).assertIsDisplayed()
        onNodeWithText(targetCategory.name).performClick()
        assertEquals(targetCategory, stateFlow.value.category)
    }
}