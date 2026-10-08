package com.ledinhthi.ontaptld.feature.review.presentation.sourceimage

import androidx.lifecycle.SavedStateHandle
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.GetCardSourceImageUseCase
import com.ledinhthi.ontaptld.navigation.SourceImageRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel của màn xem ảnh nguồn (route `SourceImageRoute`): tìm ảnh ghi chú và vùng của thẻ
 * trên ảnh. Việc đọc file ảnh và phóng to / kéo ảnh do màn hình lo, vì đó thuần là hiển thị.
 */
@HiltViewModel
class SourceImageViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    savedState: SavedStateHandle,
    private val getCardSourceImage: GetCardSourceImageUseCase,
) : BaseViewModel<SourceImageState>(SourceImageState(), toolbox) {

    private val cardId: String = checkNotNull(savedState[SourceImageRoute::cardId.name])

    init {
        // Lỗi đọc database (hiếm) được báo ngay trong màn như trường hợp "không mở được ảnh",
        // nên trả `null` để bộ xử lý lỗi chung không bật thêm thông báo.
        launchGuarded(
            onError = {
                setState { copy(isLoading = false) }
                null
            },
        ) {
            val source = getCardSourceImage(cardId)
            setState { copy(isLoading = false, imagePath = source?.imagePath, box = source?.box) }
        }
    }

    fun onClose() = navigator.back()
}
