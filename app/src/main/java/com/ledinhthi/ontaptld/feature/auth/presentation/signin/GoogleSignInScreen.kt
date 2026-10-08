package com.ledinhthi.ontaptld.feature.auth.presentation.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.R
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.components.AppCard
import com.ledinhthi.ontaptld.core.presentation.components.AppIconButton
import com.ledinhthi.ontaptld.core.presentation.components.AppTextButton
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.components.SecondaryButton
import com.ledinhthi.ontaptld.core.presentation.theme.AppDimens
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.core.presentation.theme.appColors
import com.ledinhthi.ontaptld.feature.deck.presentation.decklist.components.AppLogoTile
import kotlinx.coroutines.launch

// Số đo lấy từ bản design (LoginGoogle.dc.html).
private val LogoSize = 70.dp
private val ContentGap = 28.dp
private val PointGap = 14.dp
private val CheckBadgeSize = 24.dp
private val CheckIconSize = 14.dp
private val GoogleLogoSize = 22.dp
private val LaterButtonHeight = 48.dp

/**
 * Màn Đăng nhập Google (route `GoogleSignInRoute`), mở từ dòng "Đăng nhập Google" ở màn Cài đặt.
 * Đăng nhập là tuỳ chọn: bấm "Để sau" hay nút ← đều quay về Cài đặt, app vẫn dùng bình thường.
 */
@Composable
fun GoogleSignInScreen(viewModel: GoogleSignInViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Trong Compose, `LocalContext` ở đây chính là Activity đang hiện — thứ hộp chọn tài khoản cần.
    val context = LocalContext.current
    // Phạm vi coroutine gắn với màn này: rời màn thì việc đang chạy trong nó tự bị huỷ.
    val scope = rememberCoroutineScope()
    // Đang mở hộp chọn tài khoản thì bỏ qua lần bấm nút thứ hai.
    var choosingAccount by remember { mutableStateOf(false) }

    LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
        GoogleSignInContent(
            onBack = viewModel::onBack,
            onContinueWithGoogle = {
                if (!choosingAccount) {
                    choosingAccount = true
                    scope.launch {
                        try {
                            viewModel.onGoogleIdTokenResult(requestGoogleIdToken(context))
                        } finally {
                            // `finally` chạy cả khi coroutine bị huỷ giữa chừng (vd xoay máy).
                            choosingAccount = false
                        }
                    }
                }
            },
            onLater = viewModel::onBack,
        )
    }
}

@Composable
private fun GoogleSignInContent(onBack: () -> Unit, onContinueWithGoogle: () -> Unit, onLater: () -> Unit) {
    val colors = appColors()
    Column(Modifier.fillMaxSize().background(colors.scaffoldBackground)) {
        // Màn này không có thanh tiêu đề, chỉ một nút ← nằm thẳng trên nền.
        Box(Modifier.padding(start = AppDimens.paddingVerySmall, top = AppDimens.paddingSmall)) {
            AppIconButton(
                icon = R.drawable.ic_arrow_back,
                contentDescription = stringResource(R.string.common_back),
                onClick = onBack,
            )
        }

        // Phần giới thiệu chiếm hết chỗ còn lại và cuộn được (máy thấp, cỡ chữ lớn); cụm nút
        // bên dưới luôn nằm ở đáy màn.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = AppDimens.padding24, end = AppDimens.padding24, top = AppDimens.padding24),
            verticalArrangement = Arrangement.spacedBy(ContentGap),
        ) {
            AppLogoTile(
                size = LogoSize,
                shape = RoundedCornerShape(AppDimens.radius20),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.5.sp,
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall)) {
                Text(
                    text = stringResource(R.string.auth_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.auth_subtitle),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = colors.textSecondary,
                )
            }
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(AppDimens.defaultPadding),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(PointGap)) {
                    CheckedPoint(stringResource(R.string.auth_point_local))
                    CheckedPoint(stringResource(R.string.auth_point_photos))
                    CheckedPoint(stringResource(R.string.auth_point_sign_out))
                }
            }
        }

        Column(
            modifier = Modifier.padding(
                start = AppDimens.defaultPadding,
                end = AppDimens.defaultPadding,
                top = AppDimens.paddingSmall,
                bottom = AppDimens.padding24,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingVerySmall),
        ) {
            SecondaryButton(
                text = stringResource(R.string.auth_continue_google),
                onClick = onContinueWithGoogle,
                modifier = Modifier.fillMaxWidth(),
                leadingContent = {
                    // `Image` (không phải `Icon`) để logo giữ nguyên bốn màu của Google.
                    Image(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = null, // chữ trên nút đã nói đủ
                        modifier = Modifier.size(GoogleLogoSize),
                    )
                },
            )
            AppTextButton(
                text = stringResource(R.string.auth_later),
                onClick = onLater,
                modifier = Modifier.fillMaxWidth().height(LaterButtonHeight),
                textStyle = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.auth_footer),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Một dòng trong thẻ giới thiệu: dấu tích xanh trong ô tròn, bên phải là câu mô tả. */
@Composable
private fun CheckedPoint(text: String) {
    val colors = appColors()
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)) {
        Box(
            modifier = Modifier
                .size(CheckBadgeSize)
                .background(colors.statusGreenBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(CheckIconSize),
                tint = colors.statusGreenText,
            )
        }
        Text(
            text = text,
            // Dòng chữ cao 21 nên hàng chữ đầu nằm gần giữa ô tròn 24 mà không cần căn riêng.
            modifier = Modifier.padding(top = 1.dp),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
            color = colors.textPrimary,
        )
    }
}

// ---------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------

@Preview(name = "Đăng nhập Google", widthDp = 390, heightDp = 844)
@Composable
private fun GoogleSignInPreview() = OnTapTldTheme(themeMode = ThemeMode.LIGHT) {
    GoogleSignInContent(onBack = {}, onContinueWithGoogle = {}, onLater = {})
}

@Preview(name = "Đăng nhập Google — Dark", widthDp = 390, heightDp = 844)
@Composable
private fun GoogleSignInDarkPreview() = OnTapTldTheme(themeMode = ThemeMode.DARK) {
    GoogleSignInContent(onBack = {}, onContinueWithGoogle = {}, onLater = {})
}
