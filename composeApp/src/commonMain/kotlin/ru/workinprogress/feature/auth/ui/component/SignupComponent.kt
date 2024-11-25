package ru.workinprogress.feature.auth.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.workinprogress.feature.auth.domain.AuthUseCase
import ru.workinprogress.feature.auth.domain.SignupUseCase
import ru.workinprogress.feature.auth.ui.AuthViewModel
import ru.workinprogress.feature.auth.ui.model.AuthComponentUiState


@Composable
fun SignupComponent(onNavigateBack: () -> Unit, onSuccess: () -> Unit) {
    rememberKoinModules {
        listOf(module {
            singleOf(::SignupUseCase).bind<AuthUseCase>()

            viewModelOf(::AuthViewModel)
        })
    }

    val viewModel = koinViewModel<AuthViewModel>()
    val state = viewModel.observe.collectAsStateWithLifecycle()

    LaunchedEffect(state.value.success) {
        if (state.value.success) {
            onSuccess()
        }
    }

    Column(
        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(
            onNavigateBack, modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp)
        ) {
            Icon(Icons.Default.Close, "Close")
        }
        AuthComponentImpl(
            Modifier.align(Alignment.CenterHorizontally), AuthComponentUiState(
                "Sign up",
                state.value.username,
                state.value.password,
                "Create",
                state.value.errorMessage,
                state.value.loading
            ), viewModel::onUsernameChanged, viewModel::onPasswordChanged, viewModel::onLoginClicked
        )
        Spacer(Modifier.height(128.dp))
    }
}