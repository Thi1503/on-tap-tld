package com.ledinhthi.ontaptld.feature.capture.presentation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Đường dây" để một màn phía sau trong luồng chụp ghi chú nhắn ngược về màn chụp. Hai màn có
 * hai ViewModel riêng, không gọi thẳng nhau được; cả hai cùng được đưa cho MỘT đối tượng này
 * (`@Singleton`) nên bên này phát, bên kia nghe.
 */
@Singleton
class CaptureFlowEvents @Inject constructor() {

    // SharedFlow = dòng sự kiện phát cho mọi nơi đang nghe. `extraBufferCapacity = 1` giữ tạm
    // được một sự kiện, nhờ đó `tryEmit` bên dưới không bao giờ phải chờ.
    private val _retakeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Màn chụp nghe dòng này: mỗi lần có tín hiệu thì bỏ ảnh đang giữ, quay về camera. */
    val retakeRequests: SharedFlow<Unit> = _retakeRequests.asSharedFlow()

    /** Màn Kiểm tra văn bản gọi khi người dùng bấm "Chụp lại". */
    fun requestRetake() {
        _retakeRequests.tryEmit(Unit)
    }
}
