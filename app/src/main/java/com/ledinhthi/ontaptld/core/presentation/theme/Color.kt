package com.ledinhthi.ontaptld.core.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Ported từ `lib/const/colors.dart` (class `AppColors`) của app Flutter TLD Tracker.
 * Bảng màu THÔ (hex gốc) — giữ nguyên toàn bộ giá trị hex để không mất dữ liệu màu.
 * Lớp ngữ nghĩa (theme-aware) dùng các giá trị này nằm ở [AppExtendedColors] phía dưới.
 */
object AppPalette {

    // ---- Brand / mainColors ----
    val MainOrange = Color(0xFFF26724)

    // ---- Figma light colors ----
    val PrimaryLight2 = Color(0xFFF24E1E)
    val GrayLight1 = Color(0xFF151618)
    val GrayLight2 = Color(0xFF33414E)
    val GrayLight3 = Color(0xFF5C6771)
    val GrayLight4 = Color(0xFF768088)
    val GrayLight5 = Color(0xFFA1A8AE)
    val GrayLight6 = Color(0xFFC0C4C8)
    val GrayLight7 = Color(0xFFEBECED)
    val PrimaryLight7 = Color(0xFFFEF1E8)

    // ---- Figma dark colors ----
    val PrimaryDark2 = Color(0xFFF24E1E)
    val GrayDark1 = Color(0xFFFFFFFF)
    val GrayDark2 = Color(0xFFB8B9BB)
    val GrayDark3 = Color(0xFF919396)
    val GrayDark4 = Color(0xFF6D6E71)
    val GrayDark5 = Color(0xFF4B4C4E)
    val GrayDark6 = Color(0xFF24262B)
    val GrayDark7 = Color(0xFF111214)

    // ---- Figma status colors (badge/tag — dùng cho badge "Thủ công"/"AI") ----
    val Color33059669 = Color(0x33059669)
    val ColorD4F8E2 = Color(0xFFD4F8E2)
    val Color33E11D48 = Color(0x33E11D48)
    val ColorFFE8ED = Color(0xFFFFE8ED)
    val Color334F46E5 = Color(0x334F46E5)
    val ColorF1F0FF = Color(0xFFF1F0FF)
    val Color330891B2 = Color(0x330891B2)
    val ColorD2DFFC = Color(0xFFD2DFFC)
    val Color33D97706 = Color(0x33D97706)
    val ColorFFEAD2 = Color(0xFFFFEAD2)
    val Color059669 = Color(0xFF059669)
    val Color0B8E3F = Color(0xFF0B8E3F)

    // ---- Figma general colors ----
    val Color3DA000 = Color(0xFF3DA000)
    val ColorECB800 = Color(0xFFECB800)
    val ColorFE0000 = Color(0xFFFE0000)
    val ColorFF0000 = Color(0xFFFF0000)
    val ColorD97706 = Color(0xFFD97706)
    val ColorEA580C = Color(0xFFEA580C)
    val ColorFFD4BE = Color(0xFFFFD4BE)
    val ColorBFFAE8 = Color(0xFFBFFAE8)
    val Color0D9488 = Color(0xFF0D9488)
    val ColorCFFFFB = Color(0xFFCFFFFB)
    val Color6B7280 = Color(0xFF6B7280)
    val ColorE3E4E8 = Color(0xFFE3E4E8)
    val Color0284C7 = Color(0xFF0284C7)
    val ColorD4F1FF = Color(0xFFD4F1FF)
    val ColorDC2626 = Color(0xC4DC2626)
    val ColorFED0D0 = Color(0xFFFED0D0)
    val Color0891B2 = Color(0xFF0891B2)
    val ColorDDF8FF = Color(0xFFDDF8FF)
    val ColorE11D48 = Color(0xFFE11D48)
    val Color4F46E5 = Color(0xFF4F46E5)
    val ColorDDDBFF = Color(0xFFDDDBFF)
    val Color8B5CF6 = Color(0xFF8B5CF6)
    val Color2563EB = Color(0xFF2563EB)
    val ColorDB2777 = Color(0xFFDB2777)
    val ColorFFD2E6 = Color(0xFFFFD2E6)
    val Color37D100 = Color(0xFF37D100)
    val ColorFF3B30 = Color(0xFFFF3B30)

    // ---- Light theme base ----
    val LightPrimaryColor = Color(0xFFEFF6FF)
    val LightAccentColor = Color(0xFFF0EFF2)
    val StickyHeadLight = Color(0xFFFFFFFF)

    // ---- Dark theme base ----
    val DarkPrimaryColor = Color(0xFF3E4161)
    val DarkAccentColor = Color(0xFF25273F)

    // ---- Legacy / misc ----
    val ColorLoading = Color(0xFF58A0FF)
    val ColorBackgroundLight = Color(0xFFF7F7F7)
    val ChipDisable = Color(0xFFEFEFEF)
    val Orange = Color(0xFFE2530C)
    val OrangeShade = Color(0xFFFEE0D6)
    val BackgroundSearchColor = Color(0xFF596AFE)
    val BgSlideColorLight = Color(0xFFF9F9F9)
    val CardColor = Color(0xFF414465)
    val SystemIconColor = Color(0xFF77EDFE)
    val CalendarIconColor = Color(0xFF464E88)
    val CalendarIconColord = Color(0xFF281F1C)
    val AppBarColor1 = Color(0xFF333754)
    val ButtonColor = Color(0xFF25273F)
    val ButtonColor2 = Color(0x0FF3FFFF)
    val BackgroundColor = Color(0xFF333753)
    val BgInputTextColor = Color(0xFF3E4161)
    val BgHighLight = Color(0x60FFFFFF)
    val BgKeyBoard = Color(0xFF1F212D)
    val BgKeyBoardbtn = Color(0xFF65686F)
    val ErrorTextColor = Color(0xFFFFD54F)
    val HintTextSolidColor = Color(0xCCFFFFFF)
    val PrefixIconColor = Color(0xFF9E9E9E)
    val BgItemColor = Color(0xFF9CA4BC)
    val TextColorDefault = Color(0xFF111111)
    val ChipColor = Color(0xFFF36F21)
    val ColorBlue67ff = Color(0xFF5967FF)
    val ColorBlueAccent = Color(0xFF448AFF)
    val ColorShowCaseIcon = Color(0xFFFFFFFF)
    val ColorBackgroundShowCase = Color(0x4DFFFFFF)
    val ColorError = Color(0xFFFF5F6D)
    val TextColorIncrease = Color(0xFF06BEB6)
    val TextColorDecrease = Color(0xFFD66D75)
    val ColorInvoicesReplaced = Color(0xFFECBB00)
    val OrangeSelected = Color(0xFFFF7E5F)
    val ColorInvoicesAdjust = Color(0xFFFF7E5F)
    val ColorGreenLight = Color(0xFF82C341)
    val ColorRed444 = Color(0xCCEF4444)
    val ColorBlueB1FF = Color(0xE682B1FF)
    val ColorCancel = Color(0xFFF8F8F8)
    val ColorConfirm = Color(0xFFE95626)
    val ColorTextCancel = Color(0xFF768088)
    val ColorBorder = Color(0xFFEBECED)
    val ColorInputText = Color(0xFFF8F8F8)
    val ColorInvoicesReplace = Color(0xFFFFD751)
    val CheckBoxColor = Color(0xFFFF772E)

    // ---- Neutral aliases ----
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val Red = Color(0xFFF44336)
    val Transparent = Color(0x00000000)
    val RedAccent = Color(0xFFFF5252)
    val White54 = Color(0x8AFFFFFF)
    val White30 = Color(0x4DFFFFFF)
    val Black87 = Color(0xDD000000)
    val Grey = Color(0xFF9E9E9E)
    val GreyShade400 = Color(0xFFBDBDBD)
    val GreyShade100 = Color(0xFFF5F5F5)
    val Purple300 = Color(0xFFBA68C8)
    val Indigo700 = Color(0xFF303F9F)

    // ---- Bổ sung theo bản thiết kế UI/UX 10/2026 (không có trong app Flutter gốc) ----
    // Chữ/icon nhấn đặt trên nền nhạt: các màu gốc (F24E1E, 0B8E3F, E11D48) không đủ tương phản
    // 4.5:1 khi làm chữ nhỏ, nên cần một bậc đậm hơn (light) / sáng hơn (dark).
    val PrimaryStrongLight = Color(0xFFC23A12)
    val PrimaryStrongDark = Color(0xFFFF8A65)
    val PrimarySoftDark = Color(0xFF33201A)
    val SuccessStrongLight = Color(0xFF086B30)
    val SuccessStrongDark = Color(0xFF6EE7B7)
    val DangerStrongLight = Color(0xFFB8123A)
    val DangerStrongDark = Color(0xFFFF8FA3)
    val PurpleStrongDark = Color(0xFFA5A0FF)

    // ---- Gradients ----
    val GradientOrange = listOf(Color(0xFFFF7E5F), Color(0xFFFF5F6D))
    val GradientBlue = listOf(Color(0xFF58A0FF), Color(0xFF5967FF))
    val GradientBlueLogin = listOf(Color(0xFF5967FF), Color(0xFF596AFE))
    val GradientBlack = listOf(Black, Black87)
    val GradientGrey = listOf(Color(0xFF9CA4BC), Color(0xFF9CA4BC))
    val GradientGray = listOf(Color(0x20FFFFFF), Color(0x20FFFFFF))
    val GradientIconHome = listOf(Color(0xFFFD754A), Color(0xFFFD8058))
    val GradientHeadPayroll = listOf(Color(0xFFF6921E), Color(0xFFF15922))
}

/**
 * Lớp ngữ nghĩa (theme-aware), tương đương các static method của `AppColors` bên
 * Flutter kiểu `textColor()`, `cardBackgroundColor()`, `scaffoldBackgroundColor`...
 * Chỉ port các token còn ý nghĩa tái sử dụng chung (không phụ thuộc nghiệp vụ
 * hoá đơn/VAT/payroll của app Flutter gốc — hex thô của chúng vẫn còn đủ trong
 * [AppPalette] nếu sau này cần thêm token mới).
 */
data class AppExtendedColors(
    /** true ở dark theme — cho những nơi phải tự suy màu (vd màu riêng của từng bộ thẻ). */
    val isDark: Boolean,
    val textPrimary: Color,
    /** Chữ phụ nhưng cần đậm hơn [textSecondary] (nhãn trong badge xám, icon trên nền trắng). */
    val textStrong: Color,
    val textSecondary: Color,
    val textHint: Color,
    val textOnAccent: Color,
    val errorText: Color,
    val primary: Color,
    /** Chữ/icon màu nhấn trên nền nhạt — dùng thay [primary] khi là chữ nhỏ. */
    val primaryStrong: Color,
    val primarySoft: Color,
    /** Nền nút huỷ diệt (xoá tài khoản…), luôn đi với chữ trắng. */
    val destructive: Color,
    val scaffoldBackground: Color,
    val appBarBackground: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val cardShadow: Color,
    val divider: Color,
    val border: Color,
    /** Viền nút phụ và ô nhập. */
    val borderStrong: Color,
    /** Nền trung tính nhạt: badge xám, rãnh progress, khối skeleton. */
    val neutralSoft: Color,
    val heroBackground: Color,
    val heroBorder: Color,
    val heroText: Color,
    val heroTextSecondary: Color,
    val chipSelectedBackground: Color,
    val chipSelectedText: Color,
    val inputBackground: Color,
    val inputHint: Color,
    val bottomSheetBackground: Color,
    val navBackground: Color,
    val navUnselectedText: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val snackbarBackground: Color,
    val snackbarText: Color,
    val dialogScrim: Color,
    val statusGreenBg: Color,
    val statusGreenText: Color,
    val statusRedBg: Color,
    val statusRedText: Color,
    val statusPurpleBg: Color,
    val statusPurpleText: Color,
    val statusBlueBg: Color,
    val statusBlueText: Color,
    val statusOrangeBg: Color,
    val statusOrangeText: Color,
)

val LightAppExtendedColors = AppExtendedColors(
    isDark = false,
    textPrimary = AppPalette.GrayLight1,
    textStrong = AppPalette.GrayLight2,
    textSecondary = AppPalette.GrayLight3,
    textHint = AppPalette.GrayLight4,
    textOnAccent = AppPalette.White,
    errorText = AppPalette.DangerStrongLight,
    primary = AppPalette.PrimaryLight2,
    primaryStrong = AppPalette.PrimaryStrongLight,
    primarySoft = AppPalette.PrimaryLight7,
    destructive = AppPalette.DangerStrongLight,
    scaffoldBackground = Color(0xFFF6F6F6),
    appBarBackground = AppPalette.White,
    cardBackground = AppPalette.White,
    cardBorder = AppPalette.ColorBorder,
    cardShadow = AppPalette.Black.copy(alpha = 0.08f),
    divider = AppPalette.ColorBorder,
    border = AppPalette.ColorBorder,
    borderStrong = AppPalette.GrayLight6,
    neutralSoft = AppPalette.GrayLight7,
    heroBackground = AppPalette.GrayLight1,
    heroBorder = AppPalette.GrayLight1,
    heroText = AppPalette.White,
    heroTextSecondary = AppPalette.GrayDark2,
    chipSelectedBackground = AppPalette.GrayLight1,
    chipSelectedText = AppPalette.White,
    inputBackground = AppPalette.White,
    inputHint = AppPalette.GrayLight4,
    bottomSheetBackground = AppPalette.White,
    navBackground = AppPalette.White,
    navUnselectedText = AppPalette.GrayLight3,
    shimmerBase = AppPalette.GreyShade400,
    shimmerHighlight = AppPalette.GreyShade100,
    snackbarBackground = AppPalette.GrayLight7,
    snackbarText = AppPalette.GrayDark6,
    dialogScrim = AppPalette.Black.copy(alpha = 0.4f),
    statusGreenBg = AppPalette.ColorD4F8E2,
    statusGreenText = AppPalette.SuccessStrongLight,
    statusRedBg = AppPalette.ColorFFE8ED,
    statusRedText = AppPalette.DangerStrongLight,
    statusPurpleBg = AppPalette.ColorF1F0FF,
    statusPurpleText = AppPalette.Color4F46E5,
    statusBlueBg = AppPalette.ColorD2DFFC,
    statusBlueText = AppPalette.Color0891B2,
    statusOrangeBg = AppPalette.ColorFFEAD2,
    statusOrangeText = AppPalette.ColorD97706,
)

val DarkAppExtendedColors = AppExtendedColors(
    isDark = true,
    textPrimary = AppPalette.White,
    textStrong = AppPalette.GrayLight7,
    textSecondary = AppPalette.GrayDark2,
    textHint = AppPalette.GrayDark3,
    // Nút cam giữ chữ trắng ở cả hai theme (đúng bản thiết kế).
    textOnAccent = AppPalette.White,
    errorText = AppPalette.DangerStrongDark,
    primary = AppPalette.PrimaryDark2,
    primaryStrong = AppPalette.PrimaryStrongDark,
    primarySoft = AppPalette.PrimarySoftDark,
    destructive = AppPalette.DangerStrongLight,
    scaffoldBackground = AppPalette.GrayDark7,
    appBarBackground = AppPalette.GrayDark6,
    cardBackground = AppPalette.GrayDark6,
    // Trùng màu nền thẻ: ở dark theme thẻ tách khỏi nền bằng độ sáng, không cần viền.
    cardBorder = AppPalette.GrayDark6,
    cardShadow = AppPalette.Black.copy(alpha = 0.3f),
    divider = AppPalette.GrayDark5,
    border = AppPalette.GrayDark5,
    borderStrong = AppPalette.GrayDark4,
    neutralSoft = AppPalette.GrayDark5,
    heroBackground = AppPalette.GrayDark6,
    heroBorder = AppPalette.GrayDark5,
    heroText = AppPalette.White,
    heroTextSecondary = AppPalette.GrayDark2,
    chipSelectedBackground = AppPalette.White,
    chipSelectedText = AppPalette.GrayDark7,
    inputBackground = AppPalette.GrayDark6,
    inputHint = AppPalette.GrayDark2,
    bottomSheetBackground = AppPalette.GrayDark6,
    navBackground = Color(0xFF262626),
    navUnselectedText = Color(0xFF919396),
    shimmerBase = Color(0xFF3A3A3A),
    shimmerHighlight = Color(0xFF4A4A4A),
    snackbarBackground = AppPalette.GrayDark1,
    snackbarText = AppPalette.GrayDark6,
    dialogScrim = AppPalette.White.copy(alpha = 0.4f),
    statusGreenBg = AppPalette.Color33059669,
    statusGreenText = AppPalette.SuccessStrongDark,
    statusRedBg = AppPalette.Color33E11D48,
    statusRedText = AppPalette.DangerStrongDark,
    statusPurpleBg = AppPalette.Color334F46E5,
    statusPurpleText = AppPalette.PurpleStrongDark,
    statusBlueBg = AppPalette.Color330891B2,
    statusBlueText = AppPalette.Color0891B2,
    statusOrangeBg = AppPalette.Color33D97706,
    statusOrangeText = AppPalette.ColorD97706,
)

val LocalAppColors = staticCompositionLocalOf { LightAppExtendedColors }

/** Dùng như `AppColors.textColor()` bên Flutter: gọi `appColors().textPrimary`. */
@Composable
fun appColors(): AppExtendedColors = LocalAppColors.current
