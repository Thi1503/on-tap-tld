package com.ledinhthi.ontaptld.core.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ported từ `lib/const/dimens.dart` (class `AppDimens`).
 * Bỏ qua các giá trị chỉ phục vụ layout PDF/A4 (sizeA4Height, webPdfWidth,
 * scaleFactor...) vì không liên quan UI theme của app này.
 */
object AppDimens {

    // ---- Font sizes (dùng cho Type.kt) ----
    val fontSize10 = 10.sp
    val fontSmallest = 12.sp
    val fontSmall = 14.sp
    val fontMedium = 16.sp
    val fontBig = 18.sp
    val fontBiggest = 20.sp
    val fontSize24 = 24.sp

    // ---- Image sizes ----
    val sizeImage: Dp = 50.dp
    val sizeImageMedium: Dp = 70.dp
    val sizeImageBig: Dp = 90.dp
    val sizeImageLarge: Dp = 200.dp

    // ---- Button sizes ----
    val btnSmall: Dp = 20.dp
    val btnRecommend: Dp = 30.dp
    val btnDefault: Dp = 40.dp
    val btnMedium: Dp = 50.dp
    val btnLarge: Dp = 70.dp
    val btnDefaultFigma: Dp = 38.dp
    val btnLargeFigma: Dp = 42.dp
    val btnLoginFigmaHeight: Dp = 54.dp
    val btnFloatingButton: Dp = 48.dp
    val btnHeight46: Dp = 46.dp

    // ---- Icon sizes ----
    val sizeIcon: Dp = 20.dp
    val sizeIconMedium: Dp = 24.dp
    val sizeIconSpinner: Dp = 30.dp
    val sizeIconLarge: Dp = 36.dp
    val sizeIconExtraLarge: Dp = 200.dp
    val sizeDialogNotiIcon: Dp = 40.dp
    val sizeDialogButton: Dp = 38.dp
    val sizeIconDefault: Dp = 16.dp
    val sizeIconSmall: Dp = 18.dp
    val size42: Dp = 42.dp

    // ---- Chip / misc sizes ----
    val heightChip: Dp = 30.dp
    val widthChip: Dp = 100.dp
    val width35: Dp = 35.dp
    val height30: Dp = 30.dp

    const val maxLengthDescription = 250

    // ---- Padding / spacing scale ----
    val paddingSmallest: Dp = 4.dp
    val paddingVerySmall: Dp = 8.dp
    val padding6: Dp = 6.dp
    val padding10: Dp = 10.dp
    val paddingSmall: Dp = 12.dp
    val defaultPadding: Dp = 16.dp
    val paddingItemList: Dp = 18.dp
    val paddingMedium: Dp = 20.dp
    val padding24: Dp = 24.dp
    val padding25: Dp = 25.dp
    val paddingHuge: Dp = 32.dp
    val paddingExtra: Dp = 64.dp
    val paddingDivider: Dp = 15.dp

    // ---- AppBar sizes ----
    val showAppBarDetails: Dp = 200.dp
    val sizeAppBarBig: Dp = 120.dp
    val sizeAppBarMedium: Dp = 92.dp
    val sizeAppBar: Dp = 72.dp
    val sizeAppBarSmall: Dp = 44.dp
    val sizeCancel: Dp = 149.dp
    val bottomAppBarHeight: Dp = 60.dp

    // ---- Corner radius scale ----
    val radius2: Dp = 2.dp
    val radius3: Dp = 3.dp
    val radius4: Dp = 4.dp
    val radius6: Dp = 6.dp
    val radius8: Dp = 8.dp
    val radius12: Dp = 12.dp
    val radius13: Dp = 13.dp
    val radius20: Dp = 20.dp
    val radius30: Dp = 30.dp

    // ---- Home / search bar ----
    val sizeItemNewsHome: Dp = 110.dp
    val heightImageLogoHome: Dp = 50.dp
    val paddingSearchBarBig: Dp = 50.dp
    val paddingSearchBar: Dp = 45.dp
    val paddingSearchBarMedium: Dp = 30.dp
    val paddingSearchBarSmall: Dp = 10.dp

    val paddingTitleAndTextForm: Dp = 3.dp
}
