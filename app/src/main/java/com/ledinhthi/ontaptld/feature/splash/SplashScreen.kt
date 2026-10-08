package com.ledinhthi.ontaptld.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.AppLogoTile

/** Màn chào (route `SplashRoute`): logo, tên app, khẩu hiệu — hiện một thoáng rồi vào Home. */
@Composable
fun SplashScreen(viewModel: SplashViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { viewModel.enter() }
    SplashContent()
}

@Composable
private fun SplashContent() {
    val colors = appColors()
    // Box cho phép đặt hai phần ở hai chỗ khác nhau của cùng một khung: cụm logo ở chính giữa,
    // dòng chú thích ở sát đáy (mỗi phần tự chọn vị trí bằng `Modifier.align`).
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scaffoldBackground),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingMedium),
        ) {
            AppLogoTile(
                size = 96.dp,
                shape = RoundedCornerShape(AppDimens.radius20),
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    letterSpacing = 1.sp,
                ),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimens.padding6),
            ) {
                Text(
                    text = stringResource(R.string.app_display_name),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.splash_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
        Text(
            text = stringResource(R.string.splash_footer),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = AppDimens.padding24)
                .padding(bottom = 36.dp),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Splash", widthDp = 390, heightDp = 844)
@Composable
private fun SplashPreview() = OnTapTldTheme(themeMode = ThemeMode.LIGHT) { SplashContent() }

@Preview(name = "Splash — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun SplashDarkPreview() = OnTapTldTheme(themeMode = ThemeMode.DARK) { SplashContent() }
