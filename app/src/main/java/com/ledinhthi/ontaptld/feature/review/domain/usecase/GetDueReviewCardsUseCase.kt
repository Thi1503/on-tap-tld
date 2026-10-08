package com.ledinhthi.ontaptld.feature.review.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.dueCutoffMillis
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.review.domain.model.ReviewCard
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Lấy danh sách thẻ cần ôn hôm nay để mở một phiên ôn. Tham số là id bộ thẻ, hoặc `null` để lấy
 * ở mọi bộ. Thẻ hẹn sớm nhất đứng trước.
 *
 * Đây là ảnh chụp MỘT LẦN (`first()` = lấy giá trị đầu tiên của Flow rồi thôi), không phải dòng
 * dữ liệu sống: trong lúc ôn, thẻ vừa chấm xong sẽ hết "đến hạn" — nếu cứ nghe database thì hàng
 * thẻ tự co lại ngay dưới tay người dùng.
 *
 * Dùng chung mốc [dueCutoffMillis] với Home và Chi tiết bộ thẻ để số thẻ ở ba nơi khớp nhau.
 */
class GetDueReviewCardsUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
    private val deckRepository: DeckRepository,
    private val clock: Clock,
) : UseCase<String?, List<ReviewCard>>() {

    override suspend fun invoke(input: String?): List<ReviewCard> {
        val decksById = deckRepository.observeDecks().first().associateBy { it.id }
        return flashcardRepository.observeDue(dueCutoffMillis(clock.nowMillis())).first()
            .filter { input == null || it.deckId == input }
            // Thẻ mà bộ thẻ của nó không còn (đã xoá) thì bỏ qua — Home cũng không đếm chúng.
            .mapNotNull { card -> decksById[card.deckId]?.let { deck -> ReviewCard(card, deck) } }
    }
}
