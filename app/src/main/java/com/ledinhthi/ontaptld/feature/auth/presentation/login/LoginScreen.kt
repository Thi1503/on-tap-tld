package com.ledinhthi.ontaptld.feature.auth.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onLoginClick = viewModel::onLoginClick,
        onForgotPasswordClick = viewModel::onForgotPasswordClick,
        onSignUpClick = viewModel::onSignUpClick,
    )
}

@Composable
private fun LoginContent(
    state: LoginState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
) {
    val colors = appColors()

    LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
        Scaffold(containerColor = colors.scaffoldBackground) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.padding24),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(AppDimens.paddingExtra))

                LoginLogo()

                Spacer(Modifier.height(AppDimens.paddingHuge))

                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(AppDimens.paddingSmallest))
                Text(
                    text = stringResource(R.string.login_subtitle, stringResource(R.string.app_name)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )

                Spacer(Modifier.height(AppDimens.paddingHuge))

                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.login_email_label)) },
                    singleLine = true,
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = MaterialTheme.shapes.medium,
                )

                Spacer(Modifier.height(AppDimens.paddingSmall))

                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.login_password_label)) },
                    singleLine = true,
                    isError = state.passwordError != null,
                    supportingText = state.passwordError?.let { msg -> { Text(msg) } },
                    visualTransformation = if (state.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = onTogglePasswordVisibility) {
                            Text(
                                stringResource(
                                    if (state.isPasswordVisible) {
                                        R.string.login_password_hide
                                    } else {
                                        R.string.login_password_show
                                    },
                                ),
                            )
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                )

                TextButton(
                    onClick = onForgotPasswordClick,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = AppDimens.paddingSmallest),
                ) { Text(stringResource(R.string.login_forgot_password)) }

                Spacer(Modifier.height(AppDimens.paddingSmall))

                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimens.btnLoginFigmaHeight),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(R.string.login_button), style = MaterialTheme.typography.titleSmall)
                }

                Spacer(Modifier.height(AppDimens.paddingHuge))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.login_no_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    TextButton(onClick = onSignUpClick) { Text(stringResource(R.string.login_sign_up)) }
                }

                Spacer(Modifier.height(AppDimens.paddingHuge))
            }
        }
    }
}

@Composable
private fun LoginLogo() {
    Box(
        modifier = Modifier
            .size(AppDimens.sizeImageMedium)
            .clip(RoundedCornerShape(AppDimens.radius20))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "TLD",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Preview(name = "Login - Light", showBackground = true)
@Composable
private fun LoginScreenLightPreview() {
    OnTapTldTheme {
        LoginContent(
            state = LoginState(),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onSignUpClick = {},
        )
    }
}

@Preview(name = "Login - Error", showBackground = true)
@Composable
private fun LoginScreenErrorPreview() {
    OnTapTldTheme {
        LoginContent(
            state = LoginState(
                email = "sai-email",
                emailError = "Email không hợp lệ",
                passwordError = "Mật khẩu tối thiểu 6 ký tự",
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onSignUpClick = {},
        )
    }
}
