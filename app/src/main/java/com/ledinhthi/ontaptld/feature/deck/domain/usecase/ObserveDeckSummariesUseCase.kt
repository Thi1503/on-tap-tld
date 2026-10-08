package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.NoInputFlowUseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.dueCutoffMillis
import com.ledinhthi.ontaptld.feature.deck.domain.model.DeckSummary
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Danh sách bộ thẻ kèm số thẻ và số thẻ cần ôn hôm nay — nguồn dữ liệu của Home.
 * Tự phát lại mỗi khi bảng `decks` hoặc `flashcards` đổi. Bộ thẻ chưa có thẻ nào vẫn có mặt
 * (số liệu = 0); thẻ thuộc bộ đã xoá thì không được tính.
 */
class ObserveDeckSummariesUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val clock: Clock,
) : NoInputFlowUseCase<List<DeckSummary>>() {

    // `combine` ghép 2 dòng dữ liệu thành 1: mỗi khi MỘT TRONG HAI dòng phát giá trị mới, khối
    // lệnh bên dưới chạy lại với giá trị mới nhất của cả hai.
    override fun invoke(): Flow<List<DeckSummary>> = combine(
        deckRepository.observeDecks(),
        flashcardRepository.observeDeckStats(dueCutoffMillis(clock.nowMillis())),
    ) { decks, stats ->
        // Đổi List thành Map(deckId -> số liệu) để tra theo id cho nhanh.
        val statsByDeck = stats.associateBy { it.deckId }
        decks.map { deck ->
            val s = statsByDeck[deck.id]
            DeckSummary(deck = deck, cardCount = s?.cardCount ?: 0, dueCount = s?.dueCount ?: 0)
        }
    }
}
