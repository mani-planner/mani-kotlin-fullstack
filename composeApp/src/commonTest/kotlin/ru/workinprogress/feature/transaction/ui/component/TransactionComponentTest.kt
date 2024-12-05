package ru.workinprogress.feature.transaction.ui.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import ru.workinprogress.feature.transaction.ui.model.TransactionUiState
import kotlin.test.Test

class TransactionComponentTest : LifecycleOwner {
    override val lifecycle = LifecycleRegistry(this)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun transactionComponentSimpleTest() {
        val stateFlow = MutableStateFlow(TransactionUiState())
        runComposeUiTest {
            runOnUiThread {
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
                lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
            setContent {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides this@TransactionComponentTest
                ) {
                    val state = stateFlow.collectAsStateWithLifecycle()

                    TransactionComponentImpl(
                        state = state.value,
                        onAction = { action ->
                            {
                                when (action) {
                                    else -> {

                                    }
                                }
                            }
                        }) {
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                    }
                }
            }

            onNodeWithTag("amount").assertIsDisplayed().assertIsFocused()
            onNodeWithTag("income").assertIsDisplayed()
            onNodeWithTag("categoryContainer").assertIsDisplayed()
            onNodeWithTag("divider").assertIsDisplayed()
            onNodeWithTag("date").assertIsDisplayed()
            onNodeWithTag("comment").assertIsDisplayed()
            onNodeWithTag("submit").assertIsDisplayed().assertIsNotEnabled()
        }
    }
}