# TÀI LIỆU ĐẶC TẢ KỸ THUẬT — ONTAP APP

Created by: Thi 
Created time: 8 tháng 8, 2026 16:53
: Android/Kotlin
Last edited by: Thi 
Last updated time: 6 tháng 9, 2026 15:34

### (Chia 2 Sprint: MVP Google Play → Full Store — Native Android, Kotlin + Jetpack Compose)

| 📄 Loại tài liệu | Đặc tả kỹ thuật (Technical Specification) |
| --- | --- |
| 🎯 Mục tiêu dự án | Sản phẩm portfolio **native Android**, đối trọng với TLD Tracker (Flutter) trong CV, phục vụ tìm việc Middle Mobile Developer |
| ⏱ Deadline cứng | 30 ngày → publish Production trên Google Play (Sprint 1) |
| 🧩 Base | Kế thừa toàn bộ phần trao đổi & bản kế hoạch trước trong cùng hội thoại; theo đúng khung cấu trúc tài liệu Expense Tracker V2 đã có |
| 🏷 Trạng thái | Final — sẵn sàng để bắt đầu code |

---

## MỤC LỤC

1. Tổng quan dự án
2. ⚠️ Ràng buộc thời gian bắt buộc phải đọc trước khi code
3. Chiến lược 2 Sprint
4. Tech Stack (bản đầy đủ) + Quyết định kỹ thuật quan trọng
5. Kiến trúc mã nguồn (Clean Architecture)
6. Thiết kế Database Local (Room)
7. Kiến trúc dữ liệu Cloud (Firestore + Security Rules) — Sprint 2
8. Luồng nghiệp vụ cốt lõi (Business Workflows)
9. Danh sách màn hình (UI Screens) theo Sprint
10. Yêu cầu phi chức năng (NFR)
11. Checklist Submit Store
12. Kế hoạch thực thi chi tiết — Sprint 1 (30 ngày)
13. Kế hoạch sơ bộ — Sprint 2
14. Rủi ro & phương án dự phòng
15. Phụ lục

---

## 1. TỔNG QUAN DỰ ÁN

### 1.1 Bối cảnh & mục tiêu

**Tên dự án:** On Tap TLD — Ghi chú & Ôn tập bằng AI

Đây là dự án portfolio thứ hai, đối trọng trực tiếp với TLD Tracker: TLD Tracker chứng minh năng lực Flutter, On Tap TLD chứng minh năng lực **native Android**. Ngoài yêu cầu sản phẩm, tài liệu này thiết kế để thể hiện:

- Tư duy kiến trúc Clean Architecture bằng Kotlin — cùng triết lý đã áp dụng ở TLD Tracker, khác công cụ.
- Tích hợp AI thực tế có sản phẩm rõ ràng (không phải gắn cho có): AI xử lý dữ liệu người dùng tự tạo ra (ảnh chụp), không phải demo suông.
- Hiểu chính sách nền tảng Google Play đủ sâu để **chủ động thiết kế né rủi ro** thay vì fix sau khi bị từ chối — cùng tinh thần "privacy-first" đã dùng ở Expense Tracker.

### 1.2 Nguyên tắc thiết kế cốt lõi

- **Hai đường tạo thẻ, một kho dữ liệu:** người dùng tạo Flashcard theo cách **thủ công** (gõ tay, tức thì, không cần ảnh) hoặc **tự động** (chụp ảnh → OCR → AI sinh thẻ). Cả hai lưu chung 1 bảng `flashcards`, không tách 2 hệ thống — xem quyết định schema ở Mục 4.1.
- **Offline-first tuyệt đối:** tạo thẻ thủ công, xem thẻ, ôn tập đều chạy 100% trên local (Room), không phụ thuộc mạng. Chỉ 2 việc cần mạng: gọi AI sinh thẻ, và đồng bộ Cloud (Sprint 2, tuỳ chọn).
- **Giảm bề mặt chính sách tối đa ở Sprint 1:** không có tài khoản người dùng, không có nội dung công khai giữa người dùng — phân tích chi tiết ở Mục 4.1 và 11.

### 1.3 Phạm vi nghiệp vụ

| Có / Không | Nội dung |
| --- | --- |
| ❌ KHÔNG | Chatbot mở, tạo ảnh/video bằng AI, nội dung AI không giới hạn chủ đề |
| ❌ KHÔNG | Chia sẻ/công khai deck giữa các người dùng khác nhau (tránh chính sách UGC) |
| ❌ KHÔNG | Upload ảnh gốc lên Cloud, kể cả ở Sprint 2 |
| ✅ CÓ | Tạo/Sửa/Xoá Deck và Flashcard **thủ công** đầy đủ, không bắt buộc phải có ảnh |
| ✅ CÓ | Chụp ảnh → OCR (ML Kit) → AI sinh flashcard (Gemini), có bước duyệt/sửa trước khi lưu |
| ✅ CÓ | Ôn tập theo thuật toán lặp lại ngắt quãng SM-2 |
| ✅ CÓ (mới, Sprint 2) | Đăng nhập Google tuỳ chọn + đồng bộ 2 chiều Local ⇄ Firestore |

---

## 2. ⚠️ RÀNG BUỘC THỜI GIAN BẮT BUỘC PHẢI BIẾT TRƯỚC KHI CODE

Giống hệt ràng buộc đã áp dụng cho Expense Tracker — đọc kỹ trước khi lên kế hoạch tuần, vì nó quyết định thứ tự làm tính năng.

> Tài khoản Google Play Console Personal tạo sau 13/11/2023 (gần như chắc chắn đúng với bạn) bắt buộc chạy Closed Testing với tối thiểu **12 tester opt-in liên tục 14 ngày**, TRƯỚC KHI được nộp đơn xin quyền Production. Sau khi nộp, Google review thêm tối đa **~7 ngày** nữa. Ngoài ra, từ **31/8/2026** — chỉ còn khoảng 3 tuần nữa — app mới nộp lên Google Play bắt buộc target **Android 16 (API level 36)** trở lên.
> 

**Hệ quả trực tiếp (giống Expense Tracker):**

1. Dựng xong bản build **tối thiểu chạy được** (thêm thẻ thủ công + xem thẻ) trong **7 ngày đầu**.
2. Tạo Closed Testing track ngay, mời ≥ 12–15 người — đếm 14 ngày càng sớm càng tốt.
3. Trong lúc đồng hồ chạy, tiếp tục đẩy update lên **cùng track** để hoàn thiện AI, SM-2, widget (update không reset đồng hồ).
4. Đủ 14 ngày + 12 tester → nộp đơn Production → chờ review.

**Tìm 12 tester thật — mẹo riêng cho bạn:** nếu vẫn còn liên lạc với nhóm đã/đang test TLD Tracker, hỏi lại đúng nhóm đó trước — họ đã quen luồng cài Closed Testing, phản hồi thường nhanh hơn người mới. Ngoài ra: bạn bè/đồng nghiệp dùng Android, group Facebook/Zalo Flutter & Mobile Dev Việt Nam (thread "đổi test lẫn nhau"), bạn học IT. Không nên mua dịch vụ tester — rủi ro bị Google gắn cờ gian lận là thật.

---

## 3. CHIẾN LƯỢC 2 SPRINT

### 3.1 Tổng quan

| Tiêu chí | 🟢 Sprint 1 — MVP | 🔵 Sprint 2 — Full |
| --- | --- | --- |
| Mục tiêu | Pass Google Play, thể hiện rõ 3 điểm khác biệt cốt lõi (không gõ tay bắt buộc / thẻ có nguồn gốc / ôn trên widget) | Hoàn thiện, đủ chiều sâu kỹ thuật để kể chuyện phỏng vấn |
| Nền tảng | Android | Android (không có kế hoạch iOS — native Kotlin, không dùng chung codebase đa nền tảng) |
| Thời hạn | 30 ngày (cứng) | Linh hoạt, ~3 tuần sau khi Sprint 1 lên sóng |
| Tài khoản người dùng | Không có | Tuỳ chọn — thêm để đồng bộ đa thiết bị |
| Triết lý | Đủ tính năng lõi (thủ công + AI + ôn tập), không cầu toàn | Đầy đủ: sync, đa ngôn ngữ, thống kê, unit test sâu |

### 3.2 Phân bổ tính năng chi tiết

| Tính năng | Sprint 1 (MVP) | Sprint 2 (Full) |
| --- | --- | --- |
| Tạo/Sửa/Xoá Deck thủ công | ✅ | ✅ |
| Tạo/Sửa/Xoá Flashcard thủ công | ✅ | ✅ |
| Chụp ảnh + Crop (CameraX) | ✅ | ✅ |
| OCR (ML Kit Text Recognition) | ✅ | ✅ |
| AI sinh flashcard (Gemini qua Firebase AI Logic) | ✅ | ✅ |
| Duyệt/sửa thẻ AI trước khi lưu | ✅ | ✅ |
| Ôn tập theo SM-2 (4 mức đánh giá) | ✅ | ✅ |
| Widget màn hình chính (Jetpack Glance) | ✅ (đẩy update tuần 3) | ✅ nâng cấp |
| Notification nhắc ôn tập (WorkManager) | ✅ (tuần 3) | ✅ |
| Dark/Light theme | ✅ | ✅ |
| Đăng nhập Google (tuỳ chọn) | — | ✅ |
| Đồng bộ 2 chiều Local ⇄ Firestore | — | ✅ |
| Xoá tài khoản (chỉ hiện nếu đã đăng nhập) | — | ✅ Bắt buộc chính sách một khi có đăng nhập |
| Thống kê tiến độ ôn tập | — | ✅ |
| Đa ngôn ngữ (vi/en) | — | ✅ |
| Gemini Nano on-device (bonus, nếu máy hỗ trợ) | — | ✅ tuỳ chọn |
| Unit test cho SM-2 & UseCase | Khuyến khích | ✅ Bắt buộc để có CV story tốt |

---

## 4. TECH STACK (BẢN ĐẦY ĐỦ)

- **Core Framework:** Kotlin + Jetpack Compose (native Android, không dùng Flutter cho app này — mục tiêu chính là chứng minh năng lực native).
- **State management:** `ViewModel` + `StateFlow`/`SharedFlow`, Kotlin Coroutines.
- **Dependency Injection:** `Hilt` (nếu muốn gọn ở tuần đầu, constructor injection thủ công cũng chấp nhận được, chuyển sang Hilt sau không tốn nhiều công).
- **Local Storage:** `Room` (`room-runtime`, `room-ktx`, KSP compiler).
- **Camera & OCR:** `CameraX` + `ML Kit Text Recognition` (bản Latin mặc định — đủ cho tiếng Việt có dấu).
- **AI:** `Firebase AI Logic` (Gemini Developer API provider) — **không** gọi thẳng Gemini API bằng key nhúng trong app.
- **Bảo mật AI:** `Firebase App Check` (Play Integrity) — bắt buộc bật trước bản build đầu tiên gọi AI, kể cả bản tester.
- **Cloud (Sprint 2):** `Firebase Auth` (Google Sign-In), `Cloud Firestore`.
- **Notification & Background:** `WorkManager` (nhắc ôn tập hằng ngày, đồng bộ định kỳ Sprint 2).
- **Widget:** `Jetpack Glance` (App Widget viết bằng Compose).
- **Data Visualization (Sprint 2):** thư viện chart Compose-native (vd Vico) hoặc tự vẽ bằng `Canvas` nếu muốn tối giản dependency.
- **Định dạng số/ngày:** built-in Kotlin/Java — `java.time`, `NumberFormat` — không cần package ngoài tương đương `intl`.
- **ID Generation:** `java.util.UUID` (built-in).
- **List interactions (swipe sửa/xoá):** `SwipeToDismissBox` (Material3 Compose, built-in — không cần package tương đương `flutter_slidable`).
- **Testing:** `JUnit`, `Turbine` (test Flow), `MockK` (mocking idiomatic Kotlin — bạn đã quen Mockito nên học MockK khá nhanh).

> **Vì sao không có Retrofit/OkHttp?** Firebase AI Logic SDK và Firestore SDK tự quản lý network layer nội bộ. Thêm Retrofit là dư thừa trừ khi sau này cần gọi một API ngoài hệ sinh thái Firebase.
> 

### 4.1 Quyết định kỹ thuật quan trọng & lý do

| Quyết định | Lý do |
| --- | --- |
| **Sprint 1 KHÔNG có tài khoản người dùng** | Google chỉ bắt buộc tính năng "xoá tài khoản" **một khi app có đăng nhập**. Không đăng nhập ở Sprint 1 = né toàn bộ yêu cầu đó, review nhẹ hơn, tới Closed Testing nhanh hơn. `Firebase App Check` bảo vệ Gemini API độc lập với đăng nhập người dùng nên không mất tính năng AI. |
| `Flashcard.noteId` là nullable + có field `source` (`'manual'` / `'ai'`) | Một bảng `flashcards` duy nhất chứa cả thẻ gõ tay lẫn thẻ AI sinh ra — không tách 2 hệ thống dữ liệu, không lặp logic ôn tập/hiển thị cho 2 loại thẻ. |
| Không upload ảnh gốc lên Cloud, kể cả Sprint 2 | Ảnh chỉ lưu local; Cloud chỉ đồng bộ text đã OCR — tránh phát sinh chi phí Firebase Storage, đơn giản hoá bảo mật ảnh cá nhân. |
| Ưu tiên `Gemini Flash`/`Flash-Lite`, không dùng Pro | Hạn mức miễn phí rộng hơn nhiều so với Pro; việc tóm tắt/sinh câu hỏi không cần suy luận sâu nên Flash là đủ. |
| Field SM-2 (`easeFactor`, `interval`, `repetitions`, `dueDate`) có mặt trên `Flashcard` từ schema Room đầu tiên | Cùng nguyên tắc với Mục 6.7 của Expense Tracker: field liên quan thuật toán cốt lõi không nên vá sau khi đã có bản release, kể cả bản Closed Testing. |

---

## 5. KIẾN TRÚC MÃ NGUỒN (CLEAN ARCHITECTURE)

```
app/src/main/java/com/thi/On Tap TLD/
├── core/
│   ├── di/                       # Hilt modules
│   ├── theme/                    # Compose theme, design tokens, Dark/Light
│   ├── utils/                    # Formatters, validators
│   ├── ai/                       # FirebaseAiLogicClient, App Check setup
│   └── sync/                     # SyncService dùng chung — Sprint 2
│
├── feature/
│   ├── deck/                     # Deck & Flashcard CRUD — cả thủ công lẫn AI dùng chung
│   │   ├── data/
│   │   │   ├── local/            # Room DAO, Entity
│   │   │   ├── remote/           # Firestore datasource — Sprint 2
│   │   │   └── repository/
│   │   ├── domain/
│   │   │   ├── entity/           # Deck, Flashcard, Note
│   │   │   ├── repository/       # interface
│   │   │   └── usecase/          # CreateDeck, CreateFlashcardManual,
│   │   │                         # CreateFlashcardFromNote, EditFlashcard, DeleteFlashcard
│   │   └── presentation/
│   │       ├── viewmodel/
│   │       ├── screen/
│   │       └── component/
│   │
│   ├── capture/                  # Camera + OCR + gọi AI — output là danh sách flashcard đề xuất
│   │   ├── data/
│   │   │   └── datasource/       # CameraXController, MlKitOcrDataSource, GeminiFlashcardDataSource
│   │   ├── domain/
│   │   │   └── usecase/          # RunOcr, GenerateFlashcardsWithAi
│   │   └── presentation/         # CaptureScreen, OcrReviewScreen, AiSuggestionScreen
│   │
│   ├── review/                   # Luồng ôn tập
│   │   ├── domain/
│   │   │   └── usecase/          # GetDueCards, ReviewCard (áp SM-2)
│   │   └── presentation/         # ReviewScreen
│   │
│   ├── widget/                   # Jetpack Glance widget — Sprint 1 tuần 3
│   │
│   ├── auth/                     # [Sprint 2] Đăng nhập Google, xoá tài khoản
│   │
│   ├── stats/                    # [Sprint 2]
│   │
│   └── settings/
│
└── MainActivity.kt / On Tap TLDApp.kt
```

**Lưu ý khi implement:** `feature/deck` là nơi cả 2 đường tạo thẻ (thủ công từ `presentation` của chính nó, và tự động từ `feature/capture` gọi ngược vào UseCase của `deck`) cùng ghi vào **1 repository duy nhất** — đây là cách hiện thực hoá quyết định "một kho dữ liệu" ở Mục 1.2.

---

## 6. THIẾT KẾ DATABASE LOCAL (ROOM)

### 6.1 Entity: `DeckEntity`

kotlin

```kotlin
@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey val id: String,       // UUID sinh local
    val userId: String?,              // null ở Sprint 1 (chưa có tài khoản)
    val name: String,
    val colorHex: String,
    val createdAt: Long,
    val updatedAt: Long,              // dùng cho Last-Write-Wins khi sync — Sprint 2
    val synced: Boolean = false,      // Sprint 2
    val isDeleted: Boolean = false    // soft delete — Sprint 2
)
```

### 6.2 Entity: `NoteEntity` (ảnh + text OCR gốc — chỉ tồn tại khi thẻ tạo qua đường tự động)

kotlin

```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val deckId: String,
    val imagePath: String,            // đường dẫn ảnh gốc lưu local — KHÔNG sync lên Cloud
    val ocrText: String,
    val createdAt: Long,
    val updatedAt: Long,
    val synced: Boolean = false,      // chỉ sync phần text, Sprint 2
    val isDeleted: Boolean = false
)
```

### 6.3 Entity: `FlashcardEntity` — trung tâm của quyết định "thủ công + AI song song"

kotlin

```kotlin
@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val deckId: String,
    val noteId: String?,              // null nếu tạo THỦ CÔNG — không qua ảnh/OCR
    val source: String,               // "manual" | "ai" — hiển thị badge nguồn gốc trên UI
    val question: String,
    val answer: String,
    // Toạ độ vùng trong ảnh gốc — chỉ có giá trị khi source = "ai"
    val sourceBoxLeft: Float? = null,
    val sourceBoxTop: Float? = null,
    val sourceBoxRight: Float? = null,
    val sourceBoxBottom: Float? = null,
    // Field thuật toán SM-2 — có mặt từ đầu, xem lý do Mục 4.1
    val easeFactor: Double = 2.5,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val dueDate: Long,                // timestamp lần ôn tiếp theo
    val lastReviewedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val synced: Boolean = false,
    val isDeleted: Boolean = false
)
```

### 6.4 Entity: `SyncQueueEntity` — [Sprint 2]

kotlin

```kotlin
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String,           // "deck" | "note" | "flashcard"
    val entityId: String,
    val action: String,               // "create" | "update" | "delete"
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null
)
```

> **Tối ưu (giống Expense Tracker Mục 6.3):** trước khi thêm lệnh vào `sync_queue`, kiểm tra đã có lệnh `pending` cho cùng `entityId` chưa — nếu user sửa 1 flashcard nhiều lần lúc offline, chỉ giữ 1 lệnh update duy nhất.
> 

### 6.5 Quy tắc bắt buộc khi mở rộng schema Room

- Luôn viết `Migration` tường minh khi đổi schema **sau khi đã có bản release đầu tiên** — kể cả bản Closed Testing, vì tester đã cài bản có schema cũ. Không dùng `fallbackToDestructiveMigration()` một khi đã mời tester, vì sẽ xoá sạch dữ liệu ôn tập của họ mỗi lần cập nhật.
- Không đổi kiểu dữ liệu 1 cột đã tồn tại trực tiếp trong data class — viết `Migration` với câu lệnh `ALTER TABLE` tương ứng.

---

## 7. KIẾN TRÚC DỮ LIỆU CLOUD (FIRESTORE) — SPRINT 2

### 7.1 Cấu trúc cây thư mục

```
/users (Collection)
 └── {uid} (Document — khớp Firebase Auth UID)
      ├── decks (Sub-collection)
      │    └── {deck_id}
      ├── notes (Sub-collection)          — chỉ ocrText, KHÔNG có ảnh
      │    └── {note_id}
      └── flashcards (Sub-collection)
           └── {flashcard_id}
```

### 7.2 Firestore Security Rules (bắt buộc trước khi bật sync)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, create, update, delete: if request.auth != null
                                            && request.auth.uid == userId;

      match /{collection}/{docId} {
        allow read, write: if request.auth != null
                             && request.auth.uid == userId;
      }
    }
  }
}
```

---

## 8. LUỒNG NGHIỆP VỤ CỐT LÕI

### 8.1 Luồng: Khởi chạy ứng dụng (Sprint 1 — không có Login)

```
[Mở App] → [Splash] → Đọc Room trực tiếp → [Home]
(Không có bước kiểm tra auth — app dùng được ngay lần đầu mở, không ép đăng nhập)
```

### 8.2 Luồng: Tạo Flashcard THỦ CÔNG

```
[Home] → chọn Deck (hoặc tạo mới) → [+Thêm thẻ thủ công]
  → nhập Câu hỏi + Câu trả lời → Lưu
  → FlashcardEntity mới: noteId = null, source = "manual",
    dueDate = now (sẵn sàng ôn ngay lần đầu)
```

### 8.3 Luồng: Tạo Flashcard TỰ ĐỘNG (chụp ảnh → OCR → AI)

```
[Home] → [Chụp ảnh mới] → Camera & Crop
  → ML Kit OCR chạy local → hiện văn bản trích được
  → Người dùng sửa lỗi OCR nếu cần (bắt buộc có bước này — xem rủi ro Mục 14)
  → [Tạo thẻ ôn tập bằng AI] → gọi Gemini qua Firebase AI Logic
  → Nhận về danh sách flashcard đề xuất kèm toạ độ vùng ảnh nguồn
  → Người dùng duyệt: sửa/xoá/thêm trước khi lưu
  → Lưu: mỗi FlashcardEntity có noteId trỏ về NoteEntity vừa tạo, source = "ai"
```

### 8.4 Luồng: Ôn tập (SM-2)

Mỗi lần ôn 1 thẻ, người dùng chấm 1 trong 4 mức, ánh xạ sang quality `q` (0-5) của SM-2 gốc:

| Nút bấm | q |
| --- | --- |
| Quên | 1 |
| Khó | 3 |
| Dễ | 4 |
| Rất dễ | 5 |

```
if q < 3:
    repetitions = 0
    interval = 1
else:
    if repetitions == 0: interval = 1
    elif repetitions == 1: interval = 6
    else: interval = round(interval_cu * easeFactor)
    repetitions += 1

easeFactor = max(1.3, easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
dueDate = now + interval (ngày)
```

Đây là UseCase đáng viết unit test kỹ nhất — input/output rõ ràng, không phụ thuộc UI, điểm giải thích sâu được khi phỏng vấn.

### 8.5 Luồng: Đồng bộ 2 chiều — [Sprint 2, giống hệt cơ chế đã tự viết cho TLD Tracker]

- **Push:** có mạng → đọc tuần tự `sync_queue` → đẩy lên Firestore → thành công thì `synced = true` + xoá khỏi queue; thất bại thì tăng `retryCount`, backoff (5s → 30s → 2 phút), dừng sau ngưỡng lỗi liên tiếp.
- **Pull:** kích hoạt khi vừa đăng nhập / vừa có mạng lại → query theo `updatedAt > lastSyncTimestamp` → so khớp `id`: không có local thì tạo mới; có local và `synced = true` thì Last-Write-Wins theo `updatedAt`; có local nhưng `synced = false` thì giữ nguyên local, chờ Push xử lý trước.

### 8.6 Luồng: Đăng nhập lần đầu từ trạng thái local-only — [Sprint 2, MỚI]

```
[Cài đặt] → [Đăng nhập Google] → có Firebase UID
  → Gán userId cho TOÀN BỘ Deck/Note/Flashcard local hiện có (trước đó userId = null)
  → Đẩy toàn bộ vào sync_queue (action = "create") để Push lần đầu lên Firestore
```

### 8.7 Luồng: Xoá tài khoản — [Sprint 2, chỉ tồn tại nếu đã có đăng nhập]

1. Dialog xác nhận mạnh (nhập "XÓA" hoặc xác nhận 2 bước).
2. Re-authenticate nếu session cũ (yêu cầu bảo mật của Firebase Auth).
3. Xoá toàn bộ sub-collection Firestore của user → `user.delete()` → xoá sạch Room local → về màn không-đăng-nhập.
4. Trang web tĩnh mô tả cách yêu cầu xoá tài khoản qua email (GitHub Pages là đủ) — bắt buộc theo chính sách Google Play một khi app có đăng nhập, gắn link vào Data Safety form.

### 8.8 Luồng: Widget

```
Glance đọc trực tiếp Room (qua Repository, không qua ViewModel)
  → hiển thị số thẻ có dueDate <= hôm nay
  → tap "Ôn ngay" trên widget → mở thẳng ReviewScreen (deep link), không qua Home
```

---

## 9. DANH SÁCH MÀN HÌNH (UI SCREENS)

### 9.1 Sprint 1 — Màn hình MVP

| # | Màn hình | Ghi chú |
| --- | --- | --- |
| 1 | Splash | Không kiểm tra auth — vào thẳng Home |
| 2 | Home | Danh sách Deck, số thẻ cần ôn hôm nay, banner rỗng khi chưa có dữ liệu |
| 3 | Chi tiết Deck | Danh sách Flashcard trong deck, phân biệt badge "Thủ công" / "AI" |
| 4 | Thêm thẻ thủ công | Form Câu hỏi/Câu trả lời đơn giản |
| 5 | Camera & Crop | Chụp/chọn ảnh, crop vùng cần OCR |
| 6 | OCR Review | Hiện text trích được, cho sửa tay |
| 7 | AI Flashcard Suggestions | Duyệt/sửa/xoá thẻ AI đề xuất trước khi lưu |
| 8 | Màn Ôn tập | Flip card, 4 nút đánh giá |
| 9 | Cài đặt (tối giản) | Dark/Light, giới thiệu, xoá dữ liệu local — KHÔNG có mục xoá tài khoản (chưa có tài khoản) |
| 10 | Widget (Glance) | Đẩy update tuần 3 |

> Mọi màn danh sách cần đủ 3 trạng thái: Empty / Loading / Error — dễ bị soi khi phỏng vấn demo app.
> 

### 9.2 Sprint 2 — Màn hình bổ sung

| # | Màn hình | Ghi chú |
| --- | --- | --- |
| 11 | Đăng nhập Google | Tuỳ chọn, truy cập từ Cài đặt |
| 12 | Xoá tài khoản | Chỉ hiện nếu đã đăng nhập |
| 13 | Thống kê ôn tập | Streak, tỉ lệ nhớ |
| 14 | Chọn ngôn ngữ | Trong Cài đặt |
| 15 | Onboarding | 2-3 màn giới thiệu — điểm cộng UX, không bắt buộc |

---

## 10. YÊU CẦU PHI CHỨC NĂNG (NFR)

- **Hiệu năng:** Room đọc/ghi mượt tới ~2.000–3.000 flashcard — đủ cho scope portfolio cá nhân.
- **Cold start:** dưới 2 giây trên thiết bị tầm trung.
- **Migration:** tuân thủ tuyệt đối quy tắc Mục 6.5.
- **Bảo mật AI:** `Firebase App Check` phải bật và verify hoạt động **trước** build đầu tiên gọi Gemini, kể cả bản đưa cho tester.
- **Bảo mật Cloud:** Firestore Security Rules (Mục 7.2) phải hoàn tất trước khi bật tính năng đăng nhập/sync ở Sprint 2.
- **Quản lý quota AI:** Sprint 1 chưa có user riêng biệt (không đăng nhập) — giới hạn số lần gọi Gemini/ngày theo bộ đếm local trên thiết bị, tránh 1 thiết bị test dồn hết quota chung.

---

## 11. CHECKLIST SUBMIT STORE

### 11.1 Trước khi mời tester (để bắt đầu đếm 14 ngày)

- [ ]  Đăng ký Google Play Console (Personal, phí $25 một lần)
- [ ]  Build dạng `.aab`, targetSdk **36 (Android 16)**
- [ ]  Privacy Policy công khai, có URL thật (bắt buộc vì app xin quyền Camera + gọi AI ra ngoài)
- [ ]  Data Safety form điền sơ bộ (cập nhật thêm ở Sprint 2 khi thêm đăng nhập)
- [ ]  `Firebase App Check` đã bật và verify gọi Gemini thành công qua debug provider
- [ ]  Tạo Closed Testing track, mời ≥ 12–15 người

### 11.2 Trước khi nộp Production (cuối Sprint 1)

- [ ]  Đủ 12 tester opt-in liên tục 14 ngày
- [ ]  Khai báo mục AI-generated content trong App content (Play Console): mô tả rõ AI chỉ tóm tắt/sinh câu hỏi từ nội dung người dùng tự nhập — không phải chatbot mở
- [ ]  Có nút "Báo cáo nội dung AI" nhỏ trong màn duyệt thẻ (khuyến nghị, chi phí code gần như 0)
- [ ]  Store Listing: icon, feature graphic, ≥ 2 screenshot, mô tả ngắn/dài, Content rating questionnaire
- [ ]  Chọn Category: Education hoặc Productivity
- [ ]  Nộp đơn Production access — mô tả rõ feedback tester đã nhận và đã sửa gì (Google yêu cầu mục này)

---

## 12. KẾ HOẠCH THỰC THI CHI TIẾT — SPRINT 1 (30 NGÀY)

> Giả định làm gần như full-time. Nếu part-time buổi tối, cân nhắc dời Widget sang đầu Sprint 2.
> 

### Tuần 1 (Ngày 1–7): "Zero to Testable"

| Ngày | Công việc |
| --- | --- |
| 1–2 | Setup project Kotlin/Compose, cấu trúc package Clean Architecture, khởi tạo Firebase project (App Check + AI Logic — KHÔNG cần Auth), git repo |
| 3–4 | Room schema (Deck, Flashcard, Note) + DAO, base ViewModel/Repository |
| 5–6 | Deck list + Thêm Deck + CRUD Flashcard **thủ công** hoàn chỉnh |
| 7 | Home tối giản (danh sách deck) → **build release đầu tiên** |

➤ Cuối ngày 7: tạo Closed Testing track, mời ≥ 12–15 người (ưu tiên hỏi lại nhóm test TLD Tracker).

### Tuần 2 (Ngày 8–14): Pipeline AI + SM-2, đồng hồ 14 ngày đang chạy

| Ngày | Công việc |
| --- | --- |
| 8–9 | CameraX + Crop |
| 10–11 | ML Kit OCR + màn sửa văn bản |
| 12–13 | Firebase AI Logic (Gemini) sinh flashcard, App Check, màn duyệt/sửa/lưu |
| 14 | UseCase SM-2 + unit test cho thuật toán |

➤ Đẩy update lên cùng track, không tạo track mới.

### Tuần 3 (Ngày 15–21): Hoàn thiện, chờ đủ 14 ngày

| Ngày | Công việc |
| --- | --- |
| 15–16 | Màn Ôn tập (flip card, 4 nút, gọi UseCase SM-2) |
| 17–18 | Widget (Jetpack Glance): số thẻ cần ôn + ôn nhanh trên widget |
| 19 | Notification nhắc ôn tập (WorkManager) |
| 20 | Dark/Light theme, rà soát Empty/Loading/Error state |
| 21 | Sửa bug từ feedback tester |

➤ Khoảng ngày 21–22: nếu đủ 12 tester × 14 ngày liên tục → đủ điều kiện nộp Production access.

### Tuần 4 (Ngày 22–30): Store Listing, nộp bản chính thức, buffer

| Ngày | Công việc |
| --- | --- |
| 22–24 | Store Listing đầy đủ, Data Safety hoàn chỉnh, khai báo AI-generated content |
| 25 | Nộp đơn Production access |
| 26–29 | Chờ Google review (~7 ngày) — viết case-study cho CV/README trong lúc chờ |
| 30 | Buffer, publish nếu được duyệt |

---

## 13. KẾ HOẠCH SƠ BỘ — SPRINT 2

| Tuần | Nội dung |
| --- | --- |
| 1 | Đăng nhập Google (tuỳ chọn), Firestore schema + Security Rules, Sync Queue (Push) |
| 2 | Pull sync + Last-Write-Wins, luồng migrate dữ liệu local-only → gán userId (Mục 8.6), Xoá tài khoản (Mục 8.7) |
| 3 | Thống kê ôn tập, đa ngôn ngữ, mở rộng unit test, thử nghiệm Gemini Nano on-device (bonus), polish |

---

## 14. RỦI RO & PHƯƠNG ÁN DỰ PHÒNG

| Rủi ro | Mức độ | Phương án dự phòng |
| --- | --- | --- |
| Không tìm đủ 12 tester đúng hạn | 🔴 Cao | Hỏi lại nhóm test TLD Tracker trước, tham gia 2-3 group đổi-test, mời từ Ngày 1 |
| OCR sai nhiều với chữ viết tay / dấu tiếng Việt phức tạp | 🟡 Trung bình | Luôn có bước sửa tay text OCR trước khi tạo thẻ AI, không tự động 100% |
| Hết quota Gemini free tier khi nhiều tester dùng cùng lúc | 🟡 Trung bình | Ưu tiên Flash/Flash-Lite, giới hạn số lần gọi AI/ngày/thiết bị, cache kết quả, báo lỗi rõ ràng thay vì crash |
| App Check chặn nhầm request hợp lệ lúc dev | 🟢 Thấp | Cấu hình App Check debug provider cho build dev/tester ngay từ đầu |
| Google từ chối Production vì thiếu khai báo AI-generated content | 🟡 Trung bình | Rà kỹ Checklist Mục 11.2 trước khi nộp |
| Widget (Glance) phát sinh lỗi lạ trên một số máy | 🟢 Thấp | Không bắt buộc để pass Closed Testing — có thể lùi sang đầu Sprint 2 nếu tuần 3 không kịp |

---

## 15. PHỤ LỤC

### 15.1 Danh sách dependency chính (Gradle, theo nhóm)

kotlin

```kotlin
// Compose
androidx.compose:compose-bom
androidx.activity:activity-compose

// Local storage
androidx.room:room-runtime
androidx.room:room-ktx
androidx.room:room-compiler   // KSP

// Camera & OCR
androidx.camera:camera-camera2
androidx.camera:camera-lifecycle
androidx.camera:camera-view
com.google.mlkit:text-recognition

// AI
com.google.firebase:firebase-ai            // Firebase AI Logic
com.google.firebase:firebase-appcheck-playintegrity

// Sprint 2 — Auth & Sync
com.google.firebase:firebase-auth
com.google.firebase:firebase-firestore
com.google.android.gms:play-services-auth

// Background & Widget
androidx.work:work-runtime-ktx
androidx.glance:glance-appwidget

// DI (tuỳ chọn)
com.google.dagger:hilt-android

// Settings
androidx.datastore:datastore-preferences

// Test
junit:junit
app.cash.turbine:turbine
io.mockk:mockk
```

### 15.2 Nguồn tham khảo (đã kiểm tra tại thời điểm viết tài liệu — 08/2026)

- Google Play — Yêu cầu Closed Testing tài khoản Personal: `support.google.com/googleplay/android-developer/answer/14151465`
- Google Play — Đăng ký Play Console, phí $25: `support.google.com/googleplay/android-developer/answer/6112435`
- Google Play — Target API level requirements (deadline 31/8/2026): `support.google.com/googleplay/android-developer/answer/11926878`
- Google Play — AI-Generated Content policy: `support.google.com/googleplay/android-developer/answer/14094294`
- Google Play — Developer Program Policy (mục AI-generated content): `support.google.com/googleplay/android-developer/answer/16549787`
- Android Developers — Gemini Developer API qua Firebase AI Logic: `developer.android.com/ai/gemini/developer-api`
- Firebase — App Check cho AI Logic: `firebase.google.com/docs/ai-logic/app-check`
- ML Kit — Ngôn ngữ hỗ trợ Text Recognition v2: `developers.google.com/ml-kit/vision/text-recognition/v2/languages`
- ML Kit — Giới hạn ngôn ngữ GenAI Summarization API: `developers.google.com/ml-kit/genai/summarization/android`

> ⚠️ Chính sách store có thể thay đổi. Trước khi nộp bản chính thức ở Tuần 4, nên đọc lại trực tiếp các trang trên một lần nữa.
> 

---

*Hết tài liệu.*