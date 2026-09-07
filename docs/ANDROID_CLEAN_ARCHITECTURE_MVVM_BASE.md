# ON TAP TLD — CLEAN ARCHITECTURE + MVVM (COMPOSE) BASE

> Tài liệu kiến trúc **chuẩn** của app **On Tap TLD** (native Android, Kotlin + Jetpack Compose).
> Dev mới đọc file này trước khi viết dòng code đầu tiên. AI/agent đọc file này rồi tuân thủ đúng
> quy ước ở Mục 18–19.
>
> File này là bản **chuyển hệ** từ `FLUTTER_CLEAN_ARCHITECTURE_BLOC_BASE.md` (base Flutter + Bloc
> của TLD Tracker) sang Android/Kotlin. Nguyên tắc chuyển: **giữ nguyên toàn bộ triết lý tách lớp**
> (domain sạch, 1 use case = 1 hành động, mapper 2 chiều, cây exception thống nhất, khung
> loading/error chuẩn hoá, điều hướng qua abstraction), **chỉ đổi phần công cụ** (Bloc→ViewModel,
> get_it→Hilt, Dio→bỏ, Hive→Room/DataStore, go_router→Navigation Compose).
>
> Nghiệp vụ app: xem `docs_tld.md`. File này chỉ nói về **cách code**.

---

## Mục lục

1. [Đánh giá app & vì sao kiến trúc này](#1-đánh-giá-app--vì-sao-kiến-trúc-này)
2. [Kế thừa gì từ base Flutter / đổi gì — bảng ánh xạ](#2-kế-thừa-gì-từ-base-flutter--đổi-gì--bảng-ánh-xạ)
3. [Cấu trúc thư mục](#3-cấu-trúc-thư-mục)
4. [Domain layer — Entity, UseCase, Repository](#4-domain-layer--entity-usecase-repository)
5. [Mapper layer](#5-mapper-layer)
6. [Exception layer + ExceptionHandler](#6-exception-layer--exceptionhandler)
7. [Data layer — Room, DataStore, AI (KHÔNG có REST)](#7-data-layer--room-datastore-ai-không-có-rest)
8. [BaseViewModel — thay `buildState()` / `guard()`](#8-baseviewmodel--thay-buildstate--guard)
9. [AppNavigator + UI effect](#9-appnavigator--ui-effect)
10. [Compose helpers — LoadingOverlay & 3 trạng thái màn hình](#10-compose-helpers--loadingoverlay--3-trạng-thái-màn-hình)
11. [DI với Hilt — thay get_it / BlocProvider](#11-di-với-hilt--thay-get_it--blocprovider)
12. [Feature mẫu đầy đủ: `deck`](#12-feature-mẫu-đầy-đủ-deck)
13. [Feature mẫu ngắn: Review (SM-2), Capture (AI), Home, Splash, Widget](#13-feature-mẫu-ngắn-review-sm-2-capture-ai-home-splash-widget)
14. [Routing — Navigation Compose type-safe](#14-routing--navigation-compose-type-safe)
15. [Application + MainActivity + bootstrap](#15-application--mainactivity--bootstrap)
16. [Gradle / version catalog](#16-gradle--version-catalog)
17. [Testing — JUnit + MockK + Turbine (SM-2 làm ví dụ)](#17-testing--junit--mockk--turbine-sm-2-làm-ví-dụ)
18. [Quy trình thêm 1 feature mới](#18-quy-trình-thêm-1-feature-mới)
19. [Prompt mẫu — dán vào phiên chat mới](#19-prompt-mẫu--dán-vào-phiên-chat-mới)
20. [Chừa chỗ cho Sprint 2 (sync + auth)](#20-chừa-chỗ-cho-sprint-2-sync--auth)

---

## 1. Đánh giá app & vì sao kiến trúc này

### 1.1 Đặc điểm nghiệp vụ quyết định kiến trúc

| Đặc điểm (từ `docs_tld.md`) | Hệ quả kiến trúc |
| --- | --- |
| **Offline-first tuyệt đối** — tạo/xem/ôn thẻ chạy 100% trên Room, không cần mạng | Nguồn sự thật (source of truth) là **Room**, không phải server. Repository trả `Flow` từ DAO, UI observe trực tiếp. **Không có REST layer** (khác hẳn base Flutter vốn xoay quanh Dio). |
| Chỉ 2 việc cần mạng: **gọi AI sinh thẻ** và **sync Cloud (Sprint 2)** | Mạng nằm gọn trong 2 datasource: `GeminiClient` (Firebase AI Logic SDK) và (Sprint 2) `FirestoreSyncDataSource`. Cả hai SDK **tự quản network layer** → không cần Retrofit/OkHttp. |
| **Hai đường tạo thẻ, một kho dữ liệu** (thủ công + AI cùng ghi vào bảng `flashcards`) | `feature/capture` **không** có repository riêng cho flashcard — nó gọi ngược vào UseCase của `feature/deck`. Một `FlashcardRepository` duy nhất. |
| **SM-2** là thuật toán lõi, field SM-2 nằm trên `flashcards` từ schema đầu | Tách `Sm2Calculator` thành **hàm thuần** (pure), 0 phụ thuộc Android → unit test dày nhất ở đây (điểm kể chuyện phỏng vấn). |
| Không tài khoản ở Sprint 1; Sprint 2 thêm đăng nhập + sync 2 chiều | Schema Room đã có sẵn `userId?`, `synced`, `isDeleted`, `updatedAt`. Code Sprint 1 **không dùng** các field này nhưng **không được bỏ** — xem Mục 20. |
| Widget (Glance) đọc dữ liệu ôn tập; Notification (WorkManager) nhắc ôn | Widget & Worker **đọc Repository trực tiếp** (qua Hilt `EntryPoint`), không đi qua ViewModel. |

### 1.2 Điểm mạnh cần giữ khi code

- Domain layer sạch (không import Compose/Hilt/Room) → test được, kể chuyện kiến trúc được.
- 1 UseCase = 1 hành động nghiệp vụ → diff nhỏ, review nhanh, dễ test.
- Khung `guard()` chuẩn hoá loading + error cho **mọi** action → không lặp `try/catch` rải rác.

### 1.3 Rủi ro kiến trúc & cách phòng

| Rủi ro | Phòng ngừa trong base |
| --- | --- |
| Migration Room làm mất dữ liệu ôn tập của tester | Mục 7.3: cấm `fallbackToDestructiveMigration` sau bản Closed Testing đầu tiên; bắt buộc `MigrationTest`. |
| AI trả JSON sai định dạng → crash | `GeminiClient` bắt `SerializationException` → ném `AiException(RESPONSE_PARSE_ERROR)`, `ExceptionHandler` hiện dialog thân thiện. |
| Hết quota Gemini free tier | `AiQuotaGuard` đọc bộ đếm DataStore, chặn trước khi gọi, ném `AiException(QUOTA_EXCEEDED_LOCAL)`. |
| Over-engineer vì "cho giống base" | Base Flutter có `BaseRepository` quản `CancelToken`, `BaseBindingsFactory` + tag `SDS_`… **tất cả biến mất** ở Android (coroutine structured concurrency + Hilt ViewModel scope lo giúp). Xem Mục 2. |

---

## 2. Kế thừa gì từ base Flutter / đổi gì — bảng ánh xạ

| Base Flutter (Bloc) | Base Android (MVVM) | Ghi chú |
| --- | --- | --- |
| `Entity` marker class | `interface DomainModel` (marker) | Giữ nguyên ý tưởng. |
| `UseCase<In,Out>` / `NoInputUseCase<Out>` | `UseCase<In, Out>` / `NoInputUseCase<Out>` (operator `invoke`) | Giữ nguyên. `suspend operator fun invoke(...)`. |
| Repository interface ở `domain/`, impl ở `data/` | Giống hệt | Giữ nguyên — nguyên tắc bất biến. |
| `BaseDataMapper` / `BaseEntityMapper` | `EntityMapper<E, D>` / `ModelMapper<D, E>` | Giữ nguyên. Ở đây "Data" = Room `@Entity` hoặc DTO của AI. |
| Cây `AppException` → `RemoteException`/`custom`/`uncaught` + `AppExceptionWrapper` + `ExceptionHandler` | Cây `AppException` → `LocalException`/`AiException`/`OcrException`/`custom`/`uncaught` | **Bỏ** `RemoteException` (không có HTTP). **Thêm** loại lỗi hợp với app. `ExceptionHandler` giữ nguyên vai trò: map lỗi → hành động UI. |
| `BaseCubit.guard()` (bật loading → chạy → bắt lỗi → tắt loading) | `BaseViewModel.launchGuarded { }` | **Cùng semantics tuyệt đối.** Đây là phần lõi của việc chuyển hệ. |
| 1 `State` (Equatable) implements `BaseCubitState` (isLoading/isLoadingOverlay/exceptionWrapper) | 1 `data class State` implements `UiState` (có `status: UiStatus`) | Kotlin gom 3 field trạng thái vào `UiStatus` lồng bên trong → base set trạng thái mà không cần biết field nghiệp vụ. |
| `Get.put` permanent / `get_it registerLazySingleton` | Hilt `@Singleton` trong `@Module @InstallIn(SingletonComponent::class)` | Đăng ký 1 lần. |
| `get_it registerFactory` + `BlocProvider(create: getIt<XCubit>())` mỗi route | `@HiltViewModel` + `hiltViewModel()` trong `composable<T>` | Mỗi entry trên back stack → 1 ViewModel, tự `onCleared()` khi pop. |
| `BaseBindingsFactory` + `GetInterfaceExt` + tag `SDS_` | **Không cần** | `SavedStateHandle` + nav argument (`DeckDetailRoute(deckId)`) đã tự cho mỗi màn 1 instance state độc lập, kể cả mở chồng cùng route. |
| `AppNavigator` (interface) / `AppNavigatorImpl` (`Get.*`, BotToast) | `AppNavigator` (interface) / `AppNavigatorImpl` (SharedFlow lệnh + `NavController` collect ở `AppNavHost`) | Interface giữ gần nguyên chữ ký. |
| `GetMaterialApp` + `AppPages(GetPage list)` | `NavHost` + `composable<Route>` (type-safe, `@Serializable`) | |
| `BaseGetPage` / `GetPageMixin` (overlay loading) | `LoadingOverlay { }` composable + `collectAsStateWithLifecycle()` | |
| `sl<T>()` / `slf<T>()` (get_finder) | Hilt inject qua constructor; Worker/Widget dùng `@EntryPoint` | |
| `BotToast` snackbar | `SnackbarHostState` + `Scaffold` | Bỏ 1 dependency. |
| `BaseRepository` quản `CancelToken`, cancel khi dispose | **Bỏ** — `viewModelScope` huỷ coroutine tự động khi ViewModel clear | Repository chỉ là class thường nhận DAO/datasource qua constructor. |
| `dio`, `hive`, `path_provider` | `Room`, `DataStore` | |
| `equatable` | Kotlin `data class` | |
| `logger` | `Timber` | |
| Domain / Data / Mapper / Exception-model | **Giữ nguyên tinh thần 100%** — chỉ đổi ngôn ngữ Dart→Kotlin | Bằng chứng base gốc tách lớp đúng. |

---

## 3. Cấu trúc thư mục

```
app/src/main/java/com/ledinhthi/ontaptld/
├── core/
│   ├── di/                    # Hilt modules chung: DatabaseModule, CoreBindsModule, DispatcherModule, DataStoreModule, AppModule
│   ├── domain/
│   │   ├── model/             # DomainModel (marker)
│   │   ├── usecase/           # UseCase<In,Out>, NoInputUseCase<Out>
│   │   └── util/              # Clock, IdGenerator (wrap UUID) — để test mock được
│   ├── data/
│   │   ├── local/
│   │   │   ├── db/            # AppDatabase, Converters, BaseDao, migrations/
│   │   │   └── prefs/         # AppPreferences + DataStorePreferences
│   │   └── ai/                # GeminiClient + FirebaseGeminiClient, AppCheck init, AiQuotaGuard
│   ├── presentation/
│   │   ├── mvi/               # UiState, UiEffect, UiStatus, BaseViewModel
│   │   ├── navigation/        # AppNavigator (+Impl), NavIntent, AppDialog, SnackBarMessage
│   │   ├── components/        # LoadingOverlay, ScreenStateHost (Empty/Loading/Error), ObserveEffects
│   │   └── theme/             # Compose theme, design tokens, Dark/Light  (thư mục ui/theme cũ chuyển vào đây)
│   ├── exception/             # AppException + con, AppExceptionWrapper, GlobalExceptionHandler
│   └── sync/                  # [Sprint 2] SyncScheduler dùng chung
│
├── feature/
│   ├── deck/                  # Deck & Flashcard CRUD — cả thủ công lẫn AI dùng chung repository
│   │   ├── data/
│   │   │   ├── local/         # DeckDao, DeckEntity, FlashcardDao, FlashcardEntity, NoteDao, NoteEntity
│   │   │   ├── mapper/        # DeckEntityMapper, FlashcardEntityMapper
│   │   │   └── repository/    # DeckRepositoryImpl, FlashcardRepositoryImpl
│   │   ├── domain/
│   │   │   ├── model/         # Deck, Flashcard, Note, FlashcardSource
│   │   │   ├── repository/    # DeckRepository, FlashcardRepository (interface)
│   │   │   ├── usecase/       # ObserveDecks, CreateDeck, CreateManualFlashcard,
│   │   │   │                  # CreateFlashcardsFromNote, EditFlashcard, DeleteFlashcard
│   │   │   └── exception/     # DeckException
│   │   ├── presentation/
│   │   │   ├── decklist/      # DeckListState, DeckListViewModel, DeckListScreen, components/
│   │   │   ├── deckdetail/
│   │   │   └── manualcard/
│   │   └── di/                # DeckModule (@Binds repository)
│   │
│   ├── capture/               # Camera + OCR + gọi AI → output là danh sách flashcard đề xuất
│   │   ├── data/datasource/   # CameraXController, MlKitOcrDataSource, (GeminiClient dùng từ core/data/ai)
│   │   ├── domain/usecase/    # RunOcr, GenerateFlashcardsWithAi
│   │   └── presentation/      # capture/, ocrreview/, aisuggestion/
│   │
│   ├── review/               # Luồng ôn tập
│   │   ├── domain/
│   │   │   ├── Sm2Calculator.kt   # HÀM THUẦN — test kỹ nhất
│   │   │   └── usecase/           # GetDueCards, ReviewFlashcard
│   │   └── presentation/review/
│   │
│   ├── widget/               # Jetpack Glance — Sprint 1 tuần 3
│   ├── settings/
│   ├── auth/                 # [Sprint 2]
│   └── stats/                # [Sprint 2]
│
├── navigation/               # AppDestinations (@Serializable routes), AppNavHost
├── OnTapTldApp.kt            # @HiltAndroidApp
└── MainActivity.kt           # @AndroidEntryPoint, setContent { AppRoot() }
```

**Luật vàng về hướng phụ thuộc:**

- `domain/` **không bao giờ** import `androidx.*` (trừ `androidx.annotation`), `hilt`, `room`, `compose`, `firebase`.
- `data/` **không bao giờ** import gì thuộc `presentation/`.
- Mọi điều hướng / dialog / snackbar trong ViewModel đi qua `navigator` — không chạm `NavController`/`Context`.

---

## 4. Domain layer — Entity, UseCase, Repository

```kotlin
// core/domain/model/DomainModel.kt
package com.ledinhthi.ontaptld.core.domain.model

/** Marker cho mọi model tầng domain (tương đương `Entity` ở base Flutter). */
interface DomainModel
```

```kotlin
// core/domain/usecase/UseCase.kt
package com.ledinhthi.ontaptld.core.domain.usecase

/** 1 use case = 1 hành động nghiệp vụ. Gọi bằng `useCase(input)`. */
abstract class UseCase<in Input, out Output> {
    abstract suspend operator fun invoke(input: Input): Output
}

abstract class NoInputUseCase<out Output> {
    abstract suspend operator fun invoke(): Output
}

/**
 * Use case trả luồng dữ liệu observe được (đọc Room). Không `suspend` — trả `Flow`.
 * Đây là điểm khác base Flutter: app offline-first nên phần lớn "đọc" là stream, không phải request 1 lần.
 */
abstract class FlowUseCase<in Input, out Output> {
    abstract operator fun invoke(input: Input): kotlinx.coroutines.flow.Flow<Output>
}

abstract class NoInputFlowUseCase<out Output> {
    abstract operator fun invoke(): kotlinx.coroutines.flow.Flow<Output>
}
```

```kotlin
// core/domain/util/Clock.kt  — bọc thời gian & id để test deterministic
package com.ledinhthi.ontaptld.core.domain.util

interface Clock { fun nowMillis(): Long }
interface IdGenerator { fun newId(): String }
```

Repository interface **ở domain**, ví dụ:

```kotlin
// feature/deck/domain/repository/DeckRepository.kt
package com.ledinhthi.ontaptld.feature.deck.domain.repository

import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    fun observeDecks(): Flow<List<Deck>>
    fun observeDeck(deckId: String): Flow<Deck?>
    suspend fun upsert(deck: Deck)
    suspend fun delete(deckId: String)
}
```

> **Không có `BaseRepository`.** Base Flutter cần nó để gom `CancelToken` và cancel khi controller
> dispose. Ở Android, request sống trong `viewModelScope` (hoặc `WorkManager` scope) — huỷ scope là
> huỷ mọi coroutine con. Repository chỉ là class nhận DAO/datasource qua constructor.

---

## 5. Mapper layer

```kotlin
// core/data/mapper/Mapper.kt
package com.ledinhthi.ontaptld.core.data.mapper

/** Data (Room @Entity / DTO)  ->  Domain model. */
interface EntityMapper<E, D> {
    fun toDomain(entity: E): D
    fun toDomainOrNull(entity: E?): D? = entity?.let(::toDomain)
    fun toDomainList(entities: List<E>?): List<D> = entities?.map(::toDomain).orEmpty()
}

/** Domain model  ->  Data. */
interface ModelMapper<D, E> {
    fun toEntity(model: D): E
    fun toEntityList(models: List<D>?): List<E> = models?.map(::toEntity).orEmpty()
}

/** Khi cần cả 2 chiều, implement cả 2 interface trên 1 class. */
```

Quy ước: mapper là class `@Inject constructor()` **không state**, đặt ở `feature/<f>/data/mapper/`.
Nếu chỉ ánh xạ field-đối-field đơn giản có thể viết extension fun trong cùng file mapper, nhưng
**không** đặt logic map ở trong `@Entity` hay trong domain model.

---

## 6. Exception layer + ExceptionHandler

### 6.1 Cây exception

```kotlin
// core/exception/AppException.kt
package com.ledinhthi.ontaptld.core.exception

sealed class AppException(open val userMessage: String? = null) : Exception() {

    /** Lỗi Room / DataStore / IO local. */
    data class LocalException(
        val kind: LocalErrorKind,
        override val cause: Throwable? = null,
    ) : AppException()

    /** Lỗi khi gọi Gemini qua Firebase AI Logic. */
    data class AiException(
        val kind: AiErrorKind,
        val httpStatus: Int? = null,
        override val cause: Throwable? = null,
        override val userMessage: String? = null,
    ) : AppException()

    /** Lỗi ML Kit OCR. */
    data class OcrException(
        val kind: OcrErrorKind,
        override val cause: Throwable? = null,
    ) : AppException()

    /** Lỗi nghiệp vụ có định nghĩa rõ (validate, trạng thái không hợp lệ…). Feature tự tạo class con. */
    abstract class CustomException(override val userMessage: String? = null) : AppException(userMessage)

    /** Lỗi không lường trước — luôn được bọc bởi BaseViewModel. */
    data class UncaughtException(override val cause: Throwable?) : AppException()
}

enum class LocalErrorKind { NOT_FOUND, CONSTRAINT_VIOLATION, DISK_FULL, MIGRATION_FAILED, UNKNOWN }

enum class AiErrorKind {
    QUOTA_EXCEEDED_LOCAL,   // bộ đếm DataStore chặn trước khi gọi
    QUOTA_EXCEEDED_SERVER,  // Google trả 429
    APP_CHECK_FAILED,       // App Check chưa cấu hình / token sai
    NETWORK,                // mất mạng khi gọi AI
    CONTENT_BLOCKED,        // prompt/response bị chặn vì safety
    RESPONSE_PARSE_ERROR,   // JSON trả về sai schema
    EMPTY_RESPONSE,
    MODEL_UNAVAILABLE,
    UNKNOWN,
}

enum class OcrErrorKind { NO_TEXT_FOUND, RECOGNITION_FAILED }
```

```kotlin
// core/exception/AppExceptionWrapper.kt
package com.ledinhthi.ontaptld.core.exception

data class AppExceptionWrapper(
    val exception: AppException,
    val stackTrace: String? = null,
    val overrideMessage: String? = null,
) {
    val isConnectivityIssue: Boolean
        get() = exception is AppException.AiException &&
            exception.kind == AiErrorKind.NETWORK
}
```

### 6.2 GlobalExceptionHandler — map lỗi → hành động UI

Vai trò **giống hệt** `ExceptionHandler` của base Flutter, chỉ đổi nhánh `switch` từ HTTP code sang
loại lỗi của app. Đây là nơi **duy nhất** quyết định "lỗi này thì hiện dialog hay snackbar".

```kotlin
// core/exception/GlobalExceptionHandler.kt
package com.ledinhthi.ontaptld.core.exception

import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.navigation.SnackBarType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalExceptionHandler @Inject constructor(
    private val navigator: AppNavigator,
    // Sau này gắn string resources / i18n ở đây thay cho literal.
) {
    fun handle(wrapper: AppExceptionWrapper) {
        when (val e = wrapper.exception) {
            is AppException.AiException -> handleAi(e, wrapper.overrideMessage)
            is AppException.OcrException -> navigator.showSnackBar(
                "Không nhận được chữ từ ảnh, thử chụp rõ hơn.", SnackBarType.FAILURE,
            )
            is AppException.LocalException -> navigator.showErrorDialog(
                "Không đọc/ghi được dữ liệu trên máy. Thử khởi động lại app.",
            )
            is AppException.CustomException -> navigator.showSnackBar(
                wrapper.overrideMessage ?: e.userMessage ?: "Đã có lỗi xảy ra.",
            )
            is AppException.UncaughtException -> navigator.showSnackBar("Đã có lỗi xảy ra.")
        }
    }

    private fun handleAi(e: AppException.AiException, override: String?) = when (e.kind) {
        AiErrorKind.QUOTA_EXCEEDED_LOCAL, AiErrorKind.QUOTA_EXCEEDED_SERVER ->
            navigator.showNotificationDialog("Bạn đã dùng hết lượt tạo thẻ bằng AI hôm nay. Thử lại vào ngày mai nhé.")
        AiErrorKind.NETWORK ->
            navigator.showErrorDialog("Cần mạng để tạo thẻ bằng AI. Tạo thẻ thủ công vẫn dùng offline bình thường.")
        AiErrorKind.CONTENT_BLOCKED ->
            navigator.showNotificationDialog("Nội dung này không tạo được thẻ bằng AI. Bạn có thể tự thêm thẻ thủ công.")
        AiErrorKind.RESPONSE_PARSE_ERROR, AiErrorKind.EMPTY_RESPONSE ->
            navigator.showSnackBar("AI trả về kết quả không hợp lệ, thử lại giúp mình.")
        AiErrorKind.APP_CHECK_FAILED ->
            navigator.showErrorDialog("Lỗi xác thực ứng dụng. Cập nhật app lên bản mới nhất.")
        AiErrorKind.MODEL_UNAVAILABLE, AiErrorKind.UNKNOWN ->
            navigator.showSnackBar(override ?: "Dịch vụ AI tạm thời không dùng được.")
    }
}
```

> **i18n:** literal tiếng Việt ở trên chỉ để chạy nhanh Sprint 1. Sprint 2 (đa ngôn ngữ) thay bằng
> `context.getString(R.string.…)` — nhưng handler ở tầng core không có `Context`, nên truyền vào 1
> `StringProvider` interface (`fun get(@StringRes id: Int, vararg args: Any): String`) qua constructor.

---

## 7. Data layer — Room, DataStore, AI (KHÔNG có REST)

### 7.1 Room — `AppDatabase`

```kotlin
// core/data/local/db/AppDatabase.kt
package com.ledinhthi.ontaptld.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
// import các @Entity của từng feature

@Database(
    entities = [
        DeckEntity::class,
        NoteEntity::class,
        FlashcardEntity::class,
        // SyncQueueEntity::class,   // Sprint 2
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun noteDao(): NoteDao
    abstract fun flashcardDao(): FlashcardDao
}
```

```kotlin
// core/data/local/db/BaseDao.kt
package com.ledinhthi.ontaptld.core.data.local.db

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

/** DAO con kế thừa để có sẵn 4 thao tác ghi chuẩn. Query đặc thù viết trong DAO con. */
interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(entity: T)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(entities: List<T>)
    @Update suspend fun update(entity: T)
    @Delete suspend fun delete(entity: T)
}
```

```kotlin
// core/data/local/db/Converters.kt
package com.ledinhthi.ontaptld.core.data.local.db

import androidx.room.TypeConverter

class Converters {
    // Lưu enum dưới dạng String để migration dễ đọc (xem Mục 7.3).
    @TypeConverter fun sourceToString(s: com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource) = s.name
    @TypeConverter fun stringToSource(s: String) =
        com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource.valueOf(s)
}
```

> **DAO trả `Flow` cho read, `suspend` cho write.** UI observe `Flow` → mọi thay đổi Room tự đẩy
> lên màn hình, không cần "refresh" thủ công. Đây là điểm mạnh offline-first, tận dụng triệt để.

### 7.2 DataStore — thay `AppHive`

```kotlin
// core/data/local/prefs/AppPreferences.kt
package com.ledinhthi.ontaptld.core.data.local.prefs

import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AiUsage(val date: String, val count: Int)

interface AppPreferences {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    /** Bộ đếm số lần gọi AI theo ngày — phục vụ AiQuotaGuard (NFR Mục 10 của docs_tld). */
    val aiUsageToday: Flow<AiUsage>
    suspend fun incrementAiUsage(today: String)

    // ---- Sprint 2 ----
    val lastSyncAtMillis: Flow<Long>
    suspend fun setLastSyncAt(millis: Long)
}
```

```kotlin
// core/data/local/prefs/DataStorePreferences.kt
package com.ledinhthi.ontaptld.core.data.local.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStorePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AppPreferences {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val AI_DATE = stringPreferencesKey("ai_usage_date")
        val AI_COUNT = intPreferencesKey("ai_usage_count")
        val LAST_SYNC = longPreferencesKey("last_sync_at")
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { p ->
        ThemeMode.valueOf(p[Keys.THEME] ?: ThemeMode.SYSTEM.name)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override val aiUsageToday: Flow<AiUsage> = dataStore.data.map { p ->
        AiUsage(date = p[Keys.AI_DATE].orEmpty(), count = p[Keys.AI_COUNT] ?: 0)
    }

    override suspend fun incrementAiUsage(today: String) {
        dataStore.edit { p ->
            if (p[Keys.AI_DATE] != today) { p[Keys.AI_DATE] = today; p[Keys.AI_COUNT] = 0 }
            p[Keys.AI_COUNT] = (p[Keys.AI_COUNT] ?: 0) + 1
        }
    }

    override val lastSyncAtMillis: Flow<Long> = dataStore.data.map { it[Keys.LAST_SYNC] ?: 0L }
    override suspend fun setLastSyncAt(millis: Long) { dataStore.edit { it[Keys.LAST_SYNC] = millis } }
}
```

### 7.3 Quy tắc bắt buộc khi mở rộng schema Room

(Kế thừa Mục 6.5 của `docs_tld.md` — nâng lên thành luật code.)

- `exportSchema = true` + cấu hình `room.schemaLocation` để commit file schema JSON vào git.
- Sau **bản Closed Testing đầu tiên**: mọi đổi schema phải có `Migration` tường minh trong
  `core/data/local/db/migrations/`. **Cấm** `fallbackToDestructiveMigration()` (xoá sạch dữ liệu ôn
  tập của tester mỗi lần update).
- Không đổi kiểu 1 cột đã tồn tại trực tiếp trong `@Entity` — viết `Migration` với `ALTER TABLE`.
- Mỗi `Migration` phải có 1 test trong `androidTest` dùng `MigrationTestHelper`.
- Field liên quan thuật toán lõi (SM-2: `easeFactor`, `interval`, `repetitions`, `dueDate`) đã có
  từ `version = 1` — **không được** thêm sau.

### 7.4 AI datasource — `GeminiClient`

```kotlin
// core/data/ai/GeminiClient.kt
package com.ledinhthi.ontaptld.core.data.ai

interface GeminiClient {
    /** Trả danh sách thẻ đề xuất từ text OCR. Ném AppException.AiException khi lỗi. */
    suspend fun generateFlashcards(ocrText: String, maxCards: Int): List<AiFlashcardDto>
}

@kotlinx.serialization.Serializable
data class AiFlashcardDto(
    val question: String,
    val answer: String,
    val sourceBoxLeft: Float? = null,
    val sourceBoxTop: Float? = null,
    val sourceBoxRight: Float? = null,
    val sourceBoxBottom: Float? = null,
)
```

```kotlin
// core/data/ai/FirebaseGeminiClient.kt
package com.ledinhthi.ontaptld.core.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.generationConfig
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseGeminiClient @Inject constructor(
    private val json: Json,
) : GeminiClient {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-2.5-flash",          // Flash/Flash-Lite — KHÔNG dùng Pro (docs_tld 4.1)
            generationConfig = generationConfig { responseMimeType = "application/json" },
        )
    }

    override suspend fun generateFlashcards(ocrText: String, maxCards: Int): List<AiFlashcardDto> {
        return try {
            val response = model.generateContent(buildPrompt(ocrText, maxCards))
            val raw = response.text ?: throw AppException.AiException(AiErrorKind.EMPTY_RESPONSE)
            json.decodeFromString<List<AiFlashcardDto>>(raw)
        } catch (e: CancellationException) {
            throw e                                   // coroutine cancel PHẢI lan ra
        } catch (e: SerializationException) {
            throw AppException.AiException(AiErrorKind.RESPONSE_PARSE_ERROR, cause = e)
        } catch (e: PromptBlockedException) {
            throw AppException.AiException(AiErrorKind.CONTENT_BLOCKED, cause = e)
        } catch (e: ResponseStoppedException) {
            throw AppException.AiException(AiErrorKind.CONTENT_BLOCKED, cause = e)
        } catch (e: QuotaExceededException) {         // nếu SDK cũ chưa có: bắt ServerException + check "quota" trong message
            throw AppException.AiException(AiErrorKind.QUOTA_EXCEEDED_SERVER, cause = e)
        } catch (e: ServerException) {
            throw AppException.AiException(AiErrorKind.MODEL_UNAVAILABLE, cause = e)
        } catch (e: FirebaseAIException) {
            // Firebase AI không có exception riêng cho mạng/App Check — phân loại best-effort qua message.
            val kind = when {
                e.message?.contains("App Check", ignoreCase = true) == true -> AiErrorKind.APP_CHECK_FAILED
                e.message?.contains("network", ignoreCase = true) == true -> AiErrorKind.NETWORK
                else -> AiErrorKind.UNKNOWN
            }
            throw AppException.AiException(kind, cause = e)
        }
    }

    private fun buildPrompt(ocrText: String, maxCards: Int): String = """
        Bạn là trợ lý tạo flashcard ôn tập. Từ đoạn văn dưới đây, tạo tối đa $maxCards flashcard.
        Chỉ trả về JSON array, mỗi phần tử: {"question": "...", "answer": "..."}.
        Không thêm giải thích ngoài JSON.

        Đoạn văn:
        ""${'"'}$ocrText""${'"'}
    """.trimIndent()
}
```

```kotlin
// core/data/ai/AiQuotaGuard.kt  — chặn gọi AI khi vượt hạn mức/ngày/thiết bị (docs_tld NFR 10)
package com.ledinhthi.ontaptld.core.data.ai

import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.exception.AiErrorKind
import com.ledinhthi.ontaptld.core.exception.AppException
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class AiQuotaGuard @Inject constructor(
    private val prefs: AppPreferences,
    private val clock: Clock,
) {
    suspend fun ensureCanCall() {
        val usage = prefs.aiUsageToday.first()
        val count = if (usage.date == todayString()) usage.count else 0
        if (count >= MAX_CALLS_PER_DAY) throw AppException.AiException(AiErrorKind.QUOTA_EXCEEDED_LOCAL)
    }

    suspend fun recordCall() = prefs.incrementAiUsage(todayString())

    // LocalDate.ofInstant cần API 26 — đã bật core library desugaring (Mục 16).
    private fun todayString(): String =
        LocalDate.ofInstant(Instant.ofEpochMilli(clock.nowMillis()), ZoneId.systemDefault()).toString()

    companion object { const val MAX_CALLS_PER_DAY = 20 }
}
```

> **App Check** khởi tạo trong `OnTapTldApp` — bắt buộc trước build đầu tiên gọi AI, kể cả bản
> tester (docs_tld NFR + Mục 4). Xem Mục 15.

---

## 8. BaseViewModel — thay `buildState()` / `guard()`

### 8.1 `UiState` / `UiStatus` / `UiEffect`

```kotlin
// core/presentation/mvi/UiStatus.kt
package com.ledinhthi.ontaptld.core.presentation.mvi

import com.ledinhthi.ontaptld.core.exception.AppExceptionWrapper

/** Gom 3 field trạng thái mà base Flutter để rời trên State (isLoading/isLoadingOverlay/exceptionWrapper). */
data class UiStatus(
    val isLoading: Boolean = false,
    val isLoadingOverlay: Boolean = false,
    val exceptionWrapper: AppExceptionWrapper? = null,
)

interface UiState {
    val status: UiStatus
    /** Concrete state là data class → chỉ cần `copy(status = status)`. */
    fun withStatus(status: UiStatus): UiState
}

/** Sự kiện 1 lần (điều hướng nội bộ màn, focus field, hiện toast cục bộ…). */
interface UiEffect
```

### 8.2 `BaseViewModel`

`launchGuarded` giữ **đúng semantics** `guard()`/`buildState()` gốc: bật loading → chạy action →
lỗi thì log + build `AppExceptionWrapper` + cho ViewModel tự xử lý trước (`onError` trả `null` = đã
xử lý) → nếu không thì đẩy `GlobalExceptionHandler` xử lý mặc định → tắt loading.

```kotlin
// core/presentation/mvi/ViewModelToolbox.kt
package com.ledinhthi.ontaptld.core.presentation.mvi

import com.ledinhthi.ontaptld.core.exception.GlobalExceptionHandler
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import javax.inject.Inject

/** Gom phụ thuộc chung của mọi ViewModel để chữ ký constructor subclass gọn. */
class ViewModelToolbox @Inject constructor(
    val navigator: AppNavigator,
    val exceptionHandler: GlobalExceptionHandler,
)
```

```kotlin
// core/presentation/mvi/BaseViewModel.kt
package com.ledinhthi.ontaptld.core.presentation.mvi

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.AppExceptionWrapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseViewModel<S : UiState>(
    initialState: S,
    protected val toolbox: ViewModelToolbox,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _effect = Channel<UiEffect>(Channel.BUFFERED)
    val effect: Flow<UiEffect> = _effect.receiveAsFlow()

    protected val navigator get() = toolbox.navigator
    protected val currentState: S get() = _uiState.value

    @VisibleForTesting var isTestMode = false

    /** Cập nhật phần nghiệp vụ của state. */
    protected fun setState(reducer: S.() -> S) = _uiState.update(reducer)

    protected fun sendEffect(effect: UiEffect) { _effect.trySend(effect) }

    fun onErrorConsumed() = updateStatus { it.copy(exceptionWrapper = null) }

    @Suppress("UNCHECKED_CAST")
    private fun updateStatus(transform: (UiStatus) -> UiStatus) {
        _uiState.update { it.withStatus(transform(it.status)) as S }
    }

    /** Tương đương `guard()` / `buildState()` của base Flutter. */
    protected fun launchGuarded(
        showLoading: Boolean = false,
        showLoadingOverlay: Boolean = false,
        handleError: Boolean = true,
        overrideErrorMessage: String? = null,
        onError: (suspend (AppException) -> AppException?)? = null,
        onFinally: (suspend () -> Unit)? = null,
        block: suspend () -> Unit,
    ): Job = viewModelScope.launch {
        if (showLoadingOverlay) updateStatus { it.copy(isLoadingOverlay = true) }
        if (showLoading) updateStatus { it.copy(isLoading = true) }
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (!isTestMode) Timber.e(e)
            val appException = e as? AppException ?: AppException.UncaughtException(e)

            val unhandled = onError?.invoke(appException)
            if (onError != null && unhandled == null) return@launch   // ViewModel đã tự xử lý

            if (handleError) {
                val wrapper = AppExceptionWrapper(
                    exception = unhandled ?: appException,
                    stackTrace = e.stackTraceToString(),
                    overrideMessage = overrideErrorMessage,
                )
                updateStatus { it.copy(exceptionWrapper = wrapper) }
                toolbox.exceptionHandler.handle(wrapper)
            }
        } finally {
            if (showLoading) updateStatus { it.copy(isLoading = false) }
            if (showLoadingOverlay) updateStatus { it.copy(isLoadingOverlay = false) }
            onFinally?.invoke()
        }
    }
}
```

> **Khác biệt nhỏ so với Flutter đã cân nhắc:** base Flutter giữ overlay hiển thị khi lỗi
> `cancellation` (vì thường có request mới đang bắt đầu). Ở Kotlin, `CancellationException` được
> rethrow → `finally` vẫn chạy và tắt loading. Điều này đúng với coroutine (scope bị huỷ thì màn
> cũng đi) nên không cần cờ `hideLoadingOnFinally`.

---

## 9. AppNavigator + UI effect

```kotlin
// core/presentation/navigation/AppNavigator.kt
package com.ledinhthi.ontaptld.core.presentation.navigation

import kotlinx.coroutines.flow.Flow

sealed interface NavIntent {
    data class To(val route: Any, val singleTop: Boolean = true) : NavIntent
    data class ReplaceAll(val route: Any) : NavIntent      // xoá hết back stack (vd sau login/splash)
    data object Back : NavIntent
}

enum class SnackBarType { SUCCESS, FAILURE, INFO }
data class SnackBarMessage(val text: String, val type: SnackBarType)
data class AppDialog(
    val message: String,
    val isError: Boolean = false,
    val onClose: (() -> Unit)? = null,
)

/** Interface gần như giữ nguyên chữ ký `AppNavigator` của base Flutter. */
interface AppNavigator {
    val intents: Flow<NavIntent>
    val dialogs: Flow<AppDialog>
    val snackBars: Flow<SnackBarMessage>

    fun to(route: Any)
    fun replaceAll(route: Any)
    fun back()

    fun showErrorDialog(message: String, onClose: (() -> Unit)? = null)
    fun showNotificationDialog(message: String, onClose: (() -> Unit)? = null)
    fun showSnackBar(message: String, type: SnackBarType = SnackBarType.FAILURE)
}
```

```kotlin
// core/presentation/navigation/AppNavigatorImpl.kt
package com.ledinhthi.ontaptld.core.presentation.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNavigatorImpl @Inject constructor() : AppNavigator {

    private fun <T> event() = MutableSharedFlow<T>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _intents = event<NavIntent>()
    private val _dialogs = event<AppDialog>()
    private val _snackBars = event<SnackBarMessage>()

    // Khởi tạo 1 lần (không dùng `get()`) → instance ổn định để `LaunchedEffect(flow)` không restart.
    override val intents = _intents.asSharedFlow()
    override val dialogs = _dialogs.asSharedFlow()
    override val snackBars = _snackBars.asSharedFlow()

    override fun to(route: Any) { _intents.tryEmit(NavIntent.To(route)) }
    override fun replaceAll(route: Any) { _intents.tryEmit(NavIntent.ReplaceAll(route)) }
    override fun back() { _intents.tryEmit(NavIntent.Back) }

    override fun showErrorDialog(message: String, onClose: (() -> Unit)?) {
        _dialogs.tryEmit(AppDialog(message, isError = true, onClose = onClose))
    }
    override fun showNotificationDialog(message: String, onClose: (() -> Unit)?) {
        _dialogs.tryEmit(AppDialog(message, isError = false, onClose = onClose))
    }
    override fun showSnackBar(message: String, type: SnackBarType) {
        _snackBars.tryEmit(SnackBarMessage(message, type))
    }
}
```

`NavIntent`/`AppDialog`/`SnackBarMessage` được **collect ở `AppNavHost`** (Mục 14) — ViewModel
không bao giờ giữ `NavController`.

---

## 10. Compose helpers — LoadingOverlay & 3 trạng thái màn hình

```kotlin
// core/presentation/components/LoadingOverlay.kt
package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun LoadingOverlay(isLoading: Boolean, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        content()
        if (isLoading) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    // Nuốt MỌI pointer event để chặn thao tác lên UI phía dưới khi đang loading.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
    }
}
```

```kotlin
// core/presentation/components/ScreenStateHost.kt
// Mọi màn danh sách BẮT BUỘC đủ 3 trạng thái Empty / Loading / Error (docs_tld Mục 9).
package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> ScreenStateHost(
    isLoading: Boolean,
    items: List<T>,
    error: String?,
    onRetry: () -> Unit,
    emptyText: String,
    content: @Composable (List<T>) -> Unit,
) = when {
    isLoading && items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
    error != null && items.isEmpty() -> Column(
        Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(error); Spacer(Modifier.height(12.dp)); Button(onClick = onRetry) { Text("Thử lại") }
    }
    items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(emptyText) }
    else -> content(items)
}
```

```kotlin
// core/presentation/components/ObserveEffects.kt
package com.ledinhthi.ontaptld.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> ObserveEffects(flow: Flow<T>, onEffect: suspend (T) -> Unit) {
    val owner = LocalLifecycleOwner.current
    androidx.compose.runtime.LaunchedEffect(flow, owner) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect { onEffect(it) } }
    }
}
```

UI đọc state: `val state by viewModel.uiState.collectAsStateWithLifecycle()`
(cần `androidx.lifecycle:lifecycle-runtime-compose`).

---

## 11. DI với Hilt — thay get_it / BlocProvider

### 11.1 Module core (singleton toàn app) — tương đương `configureCoreDependencies`

```kotlin
// core/di/DatabaseModule.kt
package com.ledinhthi.ontaptld.core.di

import android.content.Context
import androidx.room.Room
import com.ledinhthi.ontaptld.core.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "ontaptld.db")
            // .addMigrations(MIGRATION_1_2, …)   // BẮT BUỘC sau bản Closed Testing đầu tiên
            .build()

    @Provides fun provideDeckDao(db: AppDatabase) = db.deckDao()
    @Provides fun provideNoteDao(db: AppDatabase) = db.noteDao()
    @Provides fun provideFlashcardDao(db: AppDatabase) = db.flashcardDao()
}
```

```kotlin
// core/di/CoreBindsModule.kt
package com.ledinhthi.ontaptld.core.di

import com.ledinhthi.ontaptld.core.data.ai.FirebaseGeminiClient
import com.ledinhthi.ontaptld.core.data.ai.GeminiClient
import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.data.local.prefs.DataStorePreferences
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CoreBindsModule {
    @Binds @Singleton fun navigator(impl: AppNavigatorImpl): AppNavigator
    @Binds @Singleton fun prefs(impl: DataStorePreferences): AppPreferences
    @Binds @Singleton fun gemini(impl: FirebaseGeminiClient): GeminiClient
}
```

```kotlin
// core/di/DispatcherModule.kt  — inject Dispatcher để test đổi được
package com.ledinhthi.ontaptld.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

@Qualifier annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @IoDispatcher fun io(): CoroutineDispatcher = Dispatchers.IO
}
```

```kotlin
// core/di/DataStoreModule.kt
package com.ledinhthi.ontaptld.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Extension property: 1 DataStore duy nhất trên toàn process (tạo 2 lần -> crash).
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.settingsDataStore
}
```

```kotlin
// core/di/AppModule.kt  — Clock / IdGenerator / Json
package com.ledinhthi.ontaptld.core.di

import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun clock(): Clock = object : Clock { override fun nowMillis() = System.currentTimeMillis() }
    @Provides @Singleton fun idGenerator(): IdGenerator = object : IdGenerator { override fun newId() = UUID.randomUUID().toString() }
    @Provides @Singleton fun json(): Json = Json { ignoreUnknownKeys = true }
}
```

### 11.2 Module feature — tương đương `<feature>_injector.dart`

```kotlin
// feature/deck/di/DeckModule.kt
package com.ledinhthi.ontaptld.feature.deck.di

import com.ledinhthi.ontaptld.feature.deck.data.repository.DeckRepositoryImpl
import com.ledinhthi.ontaptld.feature.deck.data.repository.FlashcardRepositoryImpl
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DeckModule {
    @Binds @Singleton fun deckRepository(impl: DeckRepositoryImpl): DeckRepository
    @Binds @Singleton fun flashcardRepository(impl: FlashcardRepositoryImpl): FlashcardRepository
}
```

> **UseCase & Mapper không cần khai báo module** — chúng có `@Inject constructor()`, Hilt tự dựng.
> **ViewModel không cần module** — `@HiltViewModel` + `hiltViewModel()`. Đây là chỗ Hilt xoá sạch
> phần `registerFactory` lặp đi lặp lại của base Flutter.

---

## 12. Feature mẫu đầy đủ: `deck`

Feature `deck` được viết đầy đủ 4 lớp để làm khuôn. Chú ý: **domain + data + mapper không import
Compose/Hilt-runtime/Room-annotation-ở-domain**; chỉ presentation đổi so với base Flutter.

### 12.1 Domain — model

```kotlin
// feature/deck/domain/model/Deck.kt
package com.ledinhthi.ontaptld.feature.deck.domain.model

import com.ledinhthi.ontaptld.core.domain.model.DomainModel

data class Deck(
    val id: String,
    val name: String,
    val colorHex: String,
    val createdAt: Long,
    val updatedAt: Long,
) : DomainModel
```

```kotlin
// feature/deck/domain/model/Flashcard.kt
package com.ledinhthi.ontaptld.feature.deck.domain.model

import com.ledinhthi.ontaptld.core.domain.model.DomainModel

enum class FlashcardSource { MANUAL, AI }

data class Flashcard(
    val id: String,
    val deckId: String,
    val noteId: String?,            // null nếu tạo thủ công
    val source: FlashcardSource,
    val question: String,
    val answer: String,
    val sourceBox: SourceBox? = null,   // chỉ có khi source = AI
    // --- SM-2 (có từ schema v1) ---
    val easeFactor: Double = 2.5,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val dueDate: Long,
    val lastReviewedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
) : DomainModel

data class SourceBox(val left: Float, val top: Float, val right: Float, val bottom: Float)
```

### 12.2 Domain — repository interface + exception

```kotlin
// feature/deck/domain/repository/FlashcardRepository.kt
package com.ledinhthi.ontaptld.feature.deck.domain.repository

import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import kotlinx.coroutines.flow.Flow

interface FlashcardRepository {
    fun observeByDeck(deckId: String): Flow<List<Flashcard>>
    fun observeDue(nowMillis: Long): Flow<List<Flashcard>>       // dùng cho Review + Widget
    fun observeDueCount(nowMillis: Long): Flow<Int>
    suspend fun getById(id: String): Flashcard?
    suspend fun upsert(card: Flashcard)
    suspend fun upsertAll(cards: List<Flashcard>)
    suspend fun delete(id: String)
}
```

```kotlin
// feature/deck/domain/exception/DeckException.kt
package com.ledinhthi.ontaptld.feature.deck.domain.exception

import com.ledinhthi.ontaptld.core.exception.AppException

class DeckException(val kind: Kind) : AppException.CustomException(
    userMessage = when (kind) {
        Kind.BLANK_NAME -> "Tên deck không được để trống."
        Kind.BLANK_CARD -> "Câu hỏi và câu trả lời không được để trống."
        Kind.DECK_NOT_FOUND -> "Không tìm thấy deck."
        Kind.CARD_NOT_FOUND -> "Không tìm thấy thẻ."
    },
) {
    enum class Kind { BLANK_NAME, BLANK_CARD, DECK_NOT_FOUND, CARD_NOT_FOUND }
}
```

### 12.3 Domain — use case

```kotlin
// feature/deck/domain/usecase/ObserveDecksUseCase.kt
package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.NoInputFlowUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDecksUseCase @Inject constructor(
    private val repository: DeckRepository,
) : NoInputFlowUseCase<List<Deck>>() {
    override fun invoke(): Flow<List<Deck>> = repository.observeDecks()
}
```

```kotlin
// feature/deck/domain/usecase/CreateDeckUseCase.kt
package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import javax.inject.Inject

class CreateDeckUseCase @Inject constructor(
    private val repository: DeckRepository,
    private val clock: Clock,
    private val ids: IdGenerator,
) : UseCase<CreateDeckUseCase.Params, Deck>() {

    data class Params(val name: String, val colorHex: String)

    override suspend fun invoke(input: Params): Deck {
        val name = input.name.trim()
        if (name.isEmpty()) throw DeckException(DeckException.Kind.BLANK_NAME)
        val now = clock.nowMillis()
        val deck = Deck(
            id = ids.newId(), name = name, colorHex = input.colorHex,
            createdAt = now, updatedAt = now,
        )
        repository.upsert(deck)
        return deck
    }
}
```

```kotlin
// feature/deck/domain/usecase/CreateManualFlashcardUseCase.kt
package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import javax.inject.Inject

/** Tạo thẻ THỦ CÔNG: noteId = null, source = MANUAL, dueDate = now (ôn được ngay). */
class CreateManualFlashcardUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val clock: Clock,
    private val ids: IdGenerator,
) : UseCase<CreateManualFlashcardUseCase.Params, Flashcard>() {

    data class Params(val deckId: String, val question: String, val answer: String)

    override suspend fun invoke(input: Params): Flashcard {
        val q = input.question.trim(); val a = input.answer.trim()
        if (q.isEmpty() || a.isEmpty()) throw DeckException(DeckException.Kind.BLANK_CARD)
        val now = clock.nowMillis()
        val card = Flashcard(
            id = ids.newId(), deckId = input.deckId, noteId = null,
            source = FlashcardSource.MANUAL, question = q, answer = a,
            dueDate = now, createdAt = now, updatedAt = now,
        )
        repository.upsert(card)
        return card
    }
}
```

```kotlin
// feature/deck/domain/usecase/CreateFlashcardsFromNoteUseCase.kt
// Điểm "một kho dữ liệu": feature/capture gọi use case NÀY (của deck), không tự ghi flashcard.
package com.ledinhthi.ontaptld.feature.deck.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.core.domain.util.IdGenerator
import com.ledinhthi.ontaptld.feature.deck.domain.model.Flashcard
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource
import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import javax.inject.Inject

class CreateFlashcardsFromNoteUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val clock: Clock,
    private val ids: IdGenerator,
) : UseCase<CreateFlashcardsFromNoteUseCase.Params, List<Flashcard>>() {

    data class Draft(val question: String, val answer: String, val box: SourceBox?)
    data class Params(val deckId: String, val noteId: String, val drafts: List<Draft>)

    override suspend fun invoke(input: Params): List<Flashcard> {
        val now = clock.nowMillis()
        val cards = input.drafts.map { d ->
            Flashcard(
                id = ids.newId(), deckId = input.deckId, noteId = input.noteId,
                source = FlashcardSource.AI, question = d.question.trim(), answer = d.answer.trim(),
                sourceBox = d.box, dueDate = now, createdAt = now, updatedAt = now,
            )
        }
        repository.upsertAll(cards)
        return cards
    }
}
```

### 12.4 Data — Room entity + DAO

```kotlin
// feature/deck/data/local/DeckEntity.kt
package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey val id: String,
    val userId: String? = null,      // null ở Sprint 1
    val name: String,
    val colorHex: String,
    val createdAt: Long,
    val updatedAt: Long,             // Last-Write-Wins khi sync — Sprint 2
    val synced: Boolean = false,     // Sprint 2
    val isDeleted: Boolean = false,  // soft delete — Sprint 2
)
```

```kotlin
// feature/deck/data/local/FlashcardEntity.kt
package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ledinhthi.ontaptld.feature.deck.domain.model.FlashcardSource

@Entity(
    tableName = "flashcards",
    indices = [Index("deckId"), Index("dueDate"), Index("noteId")],
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val deckId: String,
    val noteId: String? = null,
    val source: FlashcardSource,
    val question: String,
    val answer: String,
    val sourceBoxLeft: Float? = null,
    val sourceBoxTop: Float? = null,
    val sourceBoxRight: Float? = null,
    val sourceBoxBottom: Float? = null,
    val easeFactor: Double = 2.5,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val dueDate: Long,
    val lastReviewedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val synced: Boolean = false,
    val isDeleted: Boolean = false,
)
```

```kotlin
// feature/deck/data/local/DeckDao.kt
package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Dao
import androidx.room.Query
import com.ledinhthi.ontaptld.core.data.local.db.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao : BaseDao<DeckEntity> {
    @Query("SELECT * FROM decks WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :id AND isDeleted = 0")
    fun observeById(id: String): Flow<DeckEntity?>

    @Query("UPDATE decks SET isDeleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}
```

```kotlin
// feature/deck/data/local/FlashcardDao.kt
package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Dao
import androidx.room.Query
import com.ledinhthi.ontaptld.core.data.local.db.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao : BaseDao<FlashcardEntity> {
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeByDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getById(id: String): FlashcardEntity?

    @Query("SELECT * FROM flashcards WHERE isDeleted = 0 AND dueDate <= :now ORDER BY dueDate ASC")
    fun observeDue(now: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT COUNT(*) FROM flashcards WHERE isDeleted = 0 AND dueDate <= :now")
    fun observeDueCount(now: Long): Flow<Int>
}
```

```kotlin
// feature/deck/data/local/NoteEntity.kt  — ảnh gốc + text OCR, chỉ tồn tại khi thẻ tạo qua đường AI
package com.ledinhthi.ontaptld.feature.deck.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val deckId: String,
    val imagePath: String,           // đường dẫn ảnh local — KHÔNG sync lên Cloud (docs_tld 6.2)
    val ocrText: String,
    val createdAt: Long,
    val updatedAt: Long,
    val synced: Boolean = false,     // chỉ sync phần text, Sprint 2
    val isDeleted: Boolean = false,
)

// feature/deck/data/local/NoteDao.kt
@androidx.room.Dao
interface NoteDao : com.ledinhthi.ontaptld.core.data.local.db.BaseDao<NoteEntity> {
    @androidx.room.Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): NoteEntity?
}
```

### 12.5 Data — mapper + repository impl

```kotlin
// feature/deck/data/mapper/DeckEntityMapper.kt
package com.ledinhthi.ontaptld.feature.deck.data.mapper

import com.ledinhthi.ontaptld.core.data.mapper.EntityMapper
import com.ledinhthi.ontaptld.core.data.mapper.ModelMapper
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckEntity
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import javax.inject.Inject

class DeckEntityMapper @Inject constructor() :
    EntityMapper<DeckEntity, Deck>, ModelMapper<Deck, DeckEntity> {

    override fun toDomain(entity: DeckEntity) = Deck(
        id = entity.id, name = entity.name, colorHex = entity.colorHex,
        createdAt = entity.createdAt, updatedAt = entity.updatedAt,
    )

    override fun toEntity(model: Deck) = DeckEntity(
        id = model.id, name = model.name, colorHex = model.colorHex,
        createdAt = model.createdAt, updatedAt = model.updatedAt,
    )
}
```

```kotlin
// feature/deck/data/repository/DeckRepositoryImpl.kt
package com.ledinhthi.ontaptld.feature.deck.data.repository

import com.ledinhthi.ontaptld.core.data.local.db.wrapLocal   // helper Mục 12.7
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.feature.deck.data.local.DeckDao
import com.ledinhthi.ontaptld.feature.deck.data.mapper.DeckEntityMapper
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck
import com.ledinhthi.ontaptld.feature.deck.domain.repository.DeckRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeckRepositoryImpl @Inject constructor(
    private val dao: DeckDao,
    private val mapper: DeckEntityMapper,
    private val clock: Clock,
) : DeckRepository {

    override fun observeDecks(): Flow<List<Deck>> =
        dao.observeAll().map { mapper.toDomainList(it) }

    override fun observeDeck(deckId: String): Flow<Deck?> =
        dao.observeById(deckId).map { mapper.toDomainOrNull(it) }

    override suspend fun upsert(deck: Deck) = wrapLocal { dao.upsert(mapper.toEntity(deck)) }

    override suspend fun delete(deckId: String) = wrapLocal { dao.softDelete(deckId, clock.nowMillis()) }
}
```

### 12.6 Presentation — State + ViewModel + Screen

```kotlin
// feature/deck/presentation/decklist/DeckListState.kt
package com.ledinhthi.ontaptld.feature.deck.presentation.decklist

import com.ledinhthi.ontaptld.core.presentation.mvi.UiState
import com.ledinhthi.ontaptld.core.presentation.mvi.UiStatus
import com.ledinhthi.ontaptld.feature.deck.domain.model.Deck

data class DeckListState(
    override val status: UiStatus = UiStatus(),
    val decks: List<Deck> = emptyList(),
    val dueCount: Int = 0,
) : UiState {
    override fun withStatus(status: UiStatus) = copy(status = status)
}
```

```kotlin
// feature/deck/presentation/decklist/DeckListViewModel.kt
package com.ledinhthi.ontaptld.feature.deck.presentation.decklist

import androidx.lifecycle.viewModelScope
import com.ledinhthi.ontaptld.core.presentation.mvi.BaseViewModel
import com.ledinhthi.ontaptld.core.presentation.mvi.ViewModelToolbox
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateDeckUseCase
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.ObserveDecksUseCase
import com.ledinhthi.ontaptld.navigation.DeckDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DeckListViewModel @Inject constructor(
    toolbox: ViewModelToolbox,
    observeDecks: ObserveDecksUseCase,
    private val createDeck: CreateDeckUseCase,
) : BaseViewModel<DeckListState>(DeckListState(), toolbox) {

    init {
        observeDecks()
            .onEach { decks -> setState { copy(decks = decks) } }
            .launchIn(viewModelScope)
    }

    fun onCreateDeck(name: String, colorHex: String) = launchGuarded(
        showLoadingOverlay = true,
        onError = { e ->
            if (e is com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException) {
                navigator.showSnackBar(e.userMessage ?: "")
                null                       // đã xử lý xong
            } else e                       // để GlobalExceptionHandler lo
        },
    ) {
        createDeck(CreateDeckUseCase.Params(name, colorHex))
    }

    fun onDeckClick(deckId: String) = navigator.to(DeckDetailRoute(deckId))
}
```

```kotlin
// feature/deck/presentation/decklist/DeckListScreen.kt
package com.ledinhthi.ontaptld.feature.deck.presentation.decklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.core.presentation.components.LoadingOverlay
import com.ledinhthi.ontaptld.core.presentation.components.ScreenStateHost

@Composable
fun DeckListScreen(viewModel: DeckListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    LoadingOverlay(isLoading = state.status.isLoadingOverlay) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Deck của tôi") }) },
            floatingActionButton = { FloatingActionButton(onClick = { showCreate = true }) { Text("+") } },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                ScreenStateHost(
                    isLoading = state.status.isLoading,
                    items = state.decks,
                    error = state.status.exceptionWrapper?.let { "Không tải được danh sách deck." },
                    onRetry = { /* Flow tự re-emit; nút retry chỉ để UX */ },
                    emptyText = "Chưa có deck nào. Bấm + để tạo deck đầu tiên.",
                ) { decks ->
                    LazyColumn {
                        items(decks, key = { it.id }) { deck ->
                            ListItem(
                                headlineContent = { Text(deck.name) },
                                modifier = Modifier.clickable { viewModel.onDeckClick(deck.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        // CreateDeckDialog: composable cục bộ ở feature/deck/presentation/decklist/components/
        CreateDeckDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name -> showCreate = false; viewModel.onCreateDeck(name, colorHex = "#4F7CFF") },
        )
    }
}
```

### 12.7 Helper `wrapLocal` — biến lỗi Room thô thành `AppException.LocalException`

```kotlin
// core/data/local/db/LocalErrorMapping.kt
package com.ledinhthi.ontaptld.core.data.local.db

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteFullException
import com.ledinhthi.ontaptld.core.exception.AppException
import com.ledinhthi.ontaptld.core.exception.LocalErrorKind
import kotlinx.coroutines.CancellationException

inline fun <T> wrapLocal(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: SQLiteFullException) {
    throw AppException.LocalException(LocalErrorKind.DISK_FULL, e)
} catch (e: SQLiteConstraintException) {
    throw AppException.LocalException(LocalErrorKind.CONSTRAINT_VIOLATION, e)
} catch (e: Exception) {
    throw AppException.LocalException(LocalErrorKind.UNKNOWN, e)
}
```

---

## 13. Feature mẫu ngắn: Review (SM-2), Capture (AI), Home, Splash, Widget

### 13.1 Review — `Sm2Calculator` (hàm thuần) + use case

```kotlin
// feature/review/domain/Sm2Calculator.kt
package com.ledinhthi.ontaptld.feature.review.domain

import kotlin.math.roundToInt

enum class ReviewGrade(val q: Int) { FORGOT(1), HARD(3), EASY(4), VERY_EASY(5) }

data class Sm2Input(val easeFactor: Double, val interval: Int, val repetitions: Int)
data class Sm2Output(val easeFactor: Double, val interval: Int, val repetitions: Int)

/** SM-2 gốc, ánh xạ 4 nút → q như docs_tld Mục 8.4. Không phụ thuộc Android — test kỹ nhất ở đây. */
object Sm2Calculator {
    fun next(current: Sm2Input, grade: ReviewGrade): Sm2Output {
        val q = grade.q
        var repetitions = current.repetitions
        val interval: Int
        if (q < 3) {
            repetitions = 0
            interval = 1
        } else {
            interval = when (repetitions) {
                0 -> 1
                1 -> 6
                else -> (current.interval * current.easeFactor).roundToInt()
            }
            repetitions += 1
        }
        val ease = (current.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
            .coerceAtLeast(1.3)
        return Sm2Output(easeFactor = ease, interval = interval, repetitions = repetitions)
    }
}
```

```kotlin
// feature/review/domain/usecase/ReviewFlashcardUseCase.kt
package com.ledinhthi.ontaptld.feature.review.domain.usecase

import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.core.domain.util.Clock
import com.ledinhthi.ontaptld.feature.deck.domain.exception.DeckException
import com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
import com.ledinhthi.ontaptld.feature.review.domain.ReviewGrade
import com.ledinhthi.ontaptld.feature.review.domain.Sm2Calculator
import com.ledinhthi.ontaptld.feature.review.domain.Sm2Input
import javax.inject.Inject

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

class ReviewFlashcardUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val clock: Clock,
) : UseCase<ReviewFlashcardUseCase.Params, Unit>() {

    data class Params(val cardId: String, val grade: ReviewGrade)

    override suspend fun invoke(input: Params) {
        val card = repository.getById(input.cardId)
            ?: throw DeckException(DeckException.Kind.CARD_NOT_FOUND)
        val out = Sm2Calculator.next(
            Sm2Input(card.easeFactor, card.interval, card.repetitions), input.grade,
        )
        val now = clock.nowMillis()
        repository.upsert(
            card.copy(
                easeFactor = out.easeFactor,
                interval = out.interval,
                repetitions = out.repetitions,
                dueDate = now + out.interval * DAY_MILLIS,
                lastReviewedAt = now,
                updatedAt = now,
            ),
        )
    }
}
```

### 13.2 Capture — use case gọi AI, rồi ghi qua use case của `deck`

```kotlin
// feature/capture/domain/usecase/GenerateFlashcardsWithAiUseCase.kt
package com.ledinhthi.ontaptld.feature.capture.domain.usecase

import com.ledinhthi.ontaptld.core.data.ai.AiQuotaGuard
import com.ledinhthi.ontaptld.core.data.ai.GeminiClient
import com.ledinhthi.ontaptld.core.domain.usecase.UseCase
import com.ledinhthi.ontaptld.feature.deck.domain.model.SourceBox
import com.ledinhthi.ontaptld.feature.deck.domain.usecase.CreateFlashcardsFromNoteUseCase
import javax.inject.Inject

/** Trả DRAFT để người dùng duyệt — CHƯA lưu. Lưu là bước riêng sau khi user bấm xác nhận. */
class GenerateFlashcardsWithAiUseCase @Inject constructor(
    private val gemini: GeminiClient,
    private val quota: AiQuotaGuard,
) : UseCase<GenerateFlashcardsWithAiUseCase.Params, List<CreateFlashcardsFromNoteUseCase.Draft>>() {

    data class Params(val ocrText: String, val maxCards: Int = 8)

    override suspend fun invoke(input: Params): List<CreateFlashcardsFromNoteUseCase.Draft> {
        quota.ensureCanCall()
        val dtos = gemini.generateFlashcards(input.ocrText, input.maxCards)
        quota.recordCall()
        return dtos.map { d ->
            val box = if (d.sourceBoxLeft != null && d.sourceBoxTop != null &&
                d.sourceBoxRight != null && d.sourceBoxBottom != null
            ) SourceBox(d.sourceBoxLeft, d.sourceBoxTop, d.sourceBoxRight, d.sourceBoxBottom) else null
            CreateFlashcardsFromNoteUseCase.Draft(question = d.question, answer = d.answer, box = box)
        }
    }
}
```

`AiSuggestionViewModel` gọi `GenerateFlashcardsWithAiUseCase` bên trong `launchGuarded(showLoadingOverlay = true)`,
người dùng sửa danh sách draft, bấm lưu → gọi `CreateFlashcardsFromNoteUseCase`. **Cross-feature qua use
case, không qua repository.**

### 13.3 Home / Splash

- `SplashScreen`: không kiểm tra auth (Sprint 1). `LaunchedEffect` → `navigator.replaceAll(HomeRoute)`
  sau khi DB sẵn sàng (Room lazy, thường tức thì).
- `HomeViewModel`: observe `dueCount` + danh sách deck; giống `DeckListViewModel`. Nếu Home == Deck
  list thì gộp làm 1 màn, đừng tạo thừa.

### 13.4 Widget (Glance) — đọc Repository qua `@EntryPoint`

```kotlin
// feature/widget/ReviewWidget.kt (rút gọn — import lược bớt)
package com.ledinhthi.ontaptld.feature.widget

// GlanceAppWidget KHÔNG nhận @AndroidEntryPoint (không phải Android component của Hilt) →
// lấy dependency qua Hilt @EntryPoint thủ công.
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface WidgetDeps {
    fun flashcardRepository(): com.ledinhthi.ontaptld.feature.deck.domain.repository.FlashcardRepository
    fun clock(): com.ledinhthi.ontaptld.core.domain.util.Clock
}

class ReviewWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(context, WidgetDeps::class.java)
        val dueCount = deps.flashcardRepository()
            .observeDueCount(deps.clock().nowMillis())
            .first()   // kotlinx.coroutines.flow.first — snapshot 1 lần, widget không giữ collector
        provideContent {
            // Text("Cần ôn: $dueCount") + nút actionStartActivity(deep link "ontaptld://review")
        }
    }
}
```

Tap "Ôn ngay" → deep link thẳng `ReviewRoute` (không qua Home) — cấu hình `deepLinks` trên `composable<ReviewRoute>`.

---

## 14. Routing — Navigation Compose type-safe

```kotlin
// navigation/AppDestinations.kt
package com.ledinhthi.ontaptld.navigation

import kotlinx.serialization.Serializable

@Serializable data object SplashRoute
@Serializable data object HomeRoute
@Serializable data class DeckDetailRoute(val deckId: String)
@Serializable data class ManualCardRoute(val deckId: String)
@Serializable data object CaptureRoute
@Serializable data class ReviewRoute(val deckId: String? = null)
@Serializable data object SettingsRoute
```

```kotlin
// navigation/AppNavHost.kt
package com.ledinhthi.ontaptld.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navDeepLink
import com.ledinhthi.ontaptld.core.presentation.components.ObserveEffects
import com.ledinhthi.ontaptld.core.presentation.navigation.*

@Composable
fun AppNavHost(navigator: AppNavigator) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<AppDialog?>(null) }

    // ViewModel phát lệnh -> đây là nơi DUY NHẤT chạm NavController.
    ObserveEffects(navigator.intents) { intent ->
        when (intent) {
            is NavIntent.To -> navController.navigate(intent.route) { launchSingleTop = intent.singleTop }
            is NavIntent.ReplaceAll -> navController.navigate(intent.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            }
            NavIntent.Back -> navController.popBackStack()
        }
    }
    ObserveEffects(navigator.snackBars) { snackbarHostState.showSnackbar(it.text) }
    ObserveEffects(navigator.dialogs) { dialog = it }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        NavHost(navController, startDestination = SplashRoute, modifier = Modifier.padding(innerPadding)) {
            // Screen tự lấy ViewModel qua hiltViewModel(); ViewModel đọc nav-arg từ SavedStateHandle
            // (xem ghi chú dưới) → composable KHÔNG cần truyền tham số thủ công.
            composable<SplashRoute> { SplashScreen() }
            composable<HomeRoute> { DeckListScreen() }
            composable<DeckDetailRoute> { DeckDetailScreen() }
            composable<ManualCardRoute> { ManualCardScreen() }
            composable<CaptureRoute> { CaptureScreen() }
            composable<ReviewRoute>(
                deepLinks = listOf(navDeepLink { uriPattern = "ontaptld://review" }),
            ) { ReviewScreen() }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }

    dialog?.let { d ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null; d.onClose?.invoke() }) { Text("Đóng") } },
            text = { Text(d.message) },
        )
    }
}
```

> **`DeckDetailRoute(deckId)` cho mỗi màn 1 `SavedStateHandle` riêng** → ViewModel đọc `deckId` từ
> `SavedStateHandle`, mỗi lần push là 1 instance state độc lập, kể cả mở chồng ProductDetail→ProductDetail
> kiểu base cũ. **Không cần tag/factory.** ViewModel lấy arg:
> ```kotlin
> @HiltViewModel
> class DeckDetailViewModel @Inject constructor(
>     toolbox: ViewModelToolbox, savedState: SavedStateHandle, /* usecases */
> ) : BaseViewModel<...>(...) {
>     private val args = savedState.toRoute<DeckDetailRoute>()
> }
> ```

---

## 15. Application + MainActivity + bootstrap

```kotlin
// src/main/java/com/ledinhthi/ontaptld/OnTapTldApp.kt
package com.ledinhthi.ontaptld

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class OnTapTldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        Firebase.initialize(this)
        // App Check BẮT BUỘC trước build đầu tiên gọi AI, kể cả bản tester (docs_tld NFR).
        // Provider khác nhau theo build type → tách ra source set riêng để bản `release`
        // KHÔNG kéo theo artifact `firebase-appcheck-debug`.
        AppCheckInstaller.install()
    }
}
```

```kotlin
// src/debug/java/com/ledinhthi/ontaptld/AppCheckInstaller.kt
package com.ledinhthi.ontaptld

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckInstaller {
    fun install() = Firebase.appCheck
        .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
```

```kotlin
// src/release/java/com/ledinhthi/ontaptld/AppCheckInstaller.kt
package com.ledinhthi.ontaptld

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object AppCheckInstaller {
    fun install() = Firebase.appCheck
        .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
}
```

```kotlin
// MainActivity.kt
package com.ledinhthi.ontaptld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledinhthi.ontaptld.core.data.local.prefs.AppPreferences
import com.ledinhthi.ontaptld.core.data.local.prefs.ThemeMode
import com.ledinhthi.ontaptld.core.presentation.navigation.AppNavigator
import com.ledinhthi.ontaptld.core.presentation.theme.OnTapTldTheme
import com.ledinhthi.ontaptld.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Lưu ý: theme scaffold có sẵn của project tên `OnTapTLDTheme` ở `ui/theme/`. Khi dời vào
// `core/presentation/theme/` thì đổi tên thành `OnTapTldTheme` và thêm tham số `themeMode`
// (đọc ThemeMode -> chọn darkColorScheme/lightColorScheme/dynamic).
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var navigator: AppNavigator
    @Inject lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            OnTapTldTheme(themeMode = theme) {
                AppNavHost(navigator = navigator)
            }
        }
    }
}
```

> **Không còn `main()` gọi `registerXDependencies()` như Flutter.** Hilt tự sinh graph lúc compile;
> thêm feature = thêm `@Module` với `@InstallIn(SingletonComponent::class)`, không đụng file bootstrap.

---

## 16. Gradle / version catalog

Project hiện tại (`app/build.gradle.kts`): `namespace/applicationId = com.ledinhthi.ontaptld`,
`minSdk = 24`, `compileSdk/targetSdk = 37`, AGP `9.4.0`, Kotlin `2.2.10`, Compose BOM `2026.02.01`.
Cần bổ sung các nhóm sau (thêm vào `gradle/libs.versions.toml` + `app/build.gradle.kts`):

```toml
# gradle/libs.versions.toml — thêm
[versions]
hilt = "2.57"
hiltNavigationCompose = "1.2.0"
ksp = "2.2.10-2.0.2"          # khớp Kotlin 2.2.10
room = "2.8.2"
navigationCompose = "2.9.6"
kotlinxSerialization = "1.9.0"
datastore = "1.1.7"
lifecycle = "2.9.4"           # nâng từ 2.6.1: cần lifecycle-runtime-compose + viewmodel-compose
coroutines = "1.10.2"
camerax = "1.5.0"
mlkitText = "16.0.1"
firebaseBom = "34.3.0"
glance = "1.1.1"
work = "2.10.5"
timber = "5.0.1"
desugar = "2.1.5"
# test
turbine = "1.2.1"
mockk = "1.14.2"
coroutinesTest = "1.10.2"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }
mlkit-text-recognition = { group = "com.google.mlkit", name = "text-recognition", version.ref = "mlkitText" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-ai = { group = "com.google.firebase", name = "firebase-ai" }
firebase-appcheck-playintegrity = { group = "com.google.firebase", name = "firebase-appcheck-playintegrity" }
firebase-appcheck-debug = { group = "com.google.firebase", name = "firebase-appcheck-debug" }
androidx-glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hiltNavigationCompose" }
timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }
desugar-jdk-libs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugar" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
google-services = { id = "com.google.gms.google-services", version = "4.4.3" }
```

> **AGP 9 preview:** project hiện tại chỉ apply `android-application` + `kotlin-compose` (dựa vào
> "built-in Kotlin support" của AGP 9). Các Kotlin compiler plugin thêm vào đây (`kotlin-serialization`)
> và KSP có thể cần Kotlin Gradle Plugin được apply tường minh. Nếu Gradle báo thiếu KGP hoặc plugin
> serialization không nạp, thêm `alias(libs.plugins.kotlin.android)` **trước** `kotlin-compose`.
> `kotlin-parcelize` không cần (nav type-safe dùng `@Serializable`). `agp = "9.4.0"` là bản preview —
> nếu vỡ tương thích Hilt/KSP thì hạ AGP về nhánh 8.x ổn định và chỉnh `ksp`/`hilt` cho khớp.

```kotlin
// app/build.gradle.kts — bổ sung
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // alias(libs.plugins.kotlin.android)   // bỏ comment nếu AGP 9 không tự apply KGP (xem ghi chú trên)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

android {
    // ...
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true    // java.time trên minSdk 24
    }
    ksp { arg("room.schemaLocation", "$projectDir/schemas") }   // commit schemas/ vào git
}

dependencies {
    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Nav + serialization + lifecycle-compose
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    // Camera + OCR
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.text.recognition)
    // Firebase AI + App Check
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    // Widget + background
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime.ktx)
    // Misc
    implementation(libs.timber)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
}
```

> **Vì sao không có Retrofit/OkHttp:** Firebase AI Logic SDK và (Sprint 2) Firestore SDK tự quản
> network layer. Thêm Retrofit là dư thừa trừ khi cần gọi 1 API ngoài hệ Firebase.
> **Sprint 2** mới thêm: `firebase-auth`, `firebase-firestore`, `play-services-auth`, `hilt-work`
> (nếu Worker cần inject), chart lib (Vico).

---

## 17. Testing — JUnit + MockK + Turbine (SM-2 làm ví dụ)

### 17.1 `Sm2Calculator` — bắt buộc phủ kỹ

```kotlin
// test/.../feature/review/domain/Sm2CalculatorTest.kt
package com.ledinhthi.ontaptld.feature.review.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2CalculatorTest {

    @Test fun `quen (q=1) reset repetitions ve 0 va interval ve 1`() {
        val out = Sm2Calculator.next(Sm2Input(easeFactor = 2.5, interval = 10, repetitions = 5), ReviewGrade.FORGOT)
        assertEquals(0, out.repetitions)
        assertEquals(1, out.interval)
        assertTrue(out.easeFactor >= 1.3)
    }

    @Test fun `lan on dau (repetitions=0, q>=3) cho interval 1`() {
        val out = Sm2Calculator.next(Sm2Input(2.5, 0, 0), ReviewGrade.EASY)
        assertEquals(1, out.interval)
        assertEquals(1, out.repetitions)
    }

    @Test fun `lan on thu hai (repetitions=1) cho interval 6`() {
        val out = Sm2Calculator.next(Sm2Input(2.5, 1, 1), ReviewGrade.EASY)
        assertEquals(6, out.interval)
        assertEquals(2, out.repetitions)
    }

    @Test fun `tu lan thu ba interval = round(interval_cu * ease)`() {
        val out = Sm2Calculator.next(Sm2Input(easeFactor = 2.5, interval = 6, repetitions = 2), ReviewGrade.EASY)
        assertEquals(15, out.interval)   // round(6 * 2.5)
    }

    @Test fun `ease factor khong bao gio duoi 1_3`() {
        var s = Sm2Input(1.3, 5, 3)
        repeat(10) {
            val o = Sm2Calculator.next(s, ReviewGrade.HARD)
            s = Sm2Input(o.easeFactor, o.interval, o.repetitions)
        }
        assertTrue(s.easeFactor >= 1.3)
    }
}
```

### 17.2 ViewModel — MockK + Turbine + rule đổi Dispatcher

```kotlin
// test/.../core/MainDispatcherRule.kt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    // UnconfinedTestDispatcher: coroutine chạy trên Main (kể cả init{} của ViewModel) thực thi ngay,
    // không lệ thuộc scheduler của runTest -> tránh footgun "advanceUntilIdle không đẩy được init".
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

```kotlin
// test/.../feature/deck/DeckListViewModelTest.kt
@OptIn(ExperimentalCoroutinesApi::class)
class DeckListViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val observeDecks = mockk<ObserveDecksUseCase>()
    private val createDeck = mockk<CreateDeckUseCase>()
    private val navigator = mockk<AppNavigator>(relaxed = true)
    private val exceptionHandler = mockk<GlobalExceptionHandler>(relaxed = true)
    private val toolbox = ViewModelToolbox(navigator, exceptionHandler)

    @Test fun `tao deck ten rong -> hien snackbar, khong goi ExceptionHandler`() = runTest {
        every { observeDecks.invoke() } returns flowOf(emptyList())
        coEvery { createDeck.invoke(any()) } throws DeckException(DeckException.Kind.BLANK_NAME)

        val vm = DeckListViewModel(toolbox, observeDecks, createDeck).apply { isTestMode = true }
        vm.onCreateDeck(name = "  ", colorHex = "#fff")
        advanceUntilIdle()

        verify { navigator.showSnackBar(any(), any()) }
        verify(exactly = 0) { exceptionHandler.handle(any()) }
    }
}
```

> `runTest` cần nằm cùng scheduler với rule khi dùng `StandardTestDispatcher`. Với
> `UnconfinedTestDispatcher` như trên thì không cần bận tâm; nếu đổi sang `StandardTestDispatcher`
> nhớ truyền `MainDispatcherRule(StandardTestDispatcher())` rồi `runTest(mainRule.dispatcher.scheduler)`.

Nguyên tắc test:
- **Domain (usecase, Sm2)**: test thuần JVM, không Robolectric.
- **ViewModel**: MockK cho usecase/navigator, Turbine cho `uiState`/`effect`, `MainDispatcherRule`.
- **DAO + Migration**: `androidTest` với in-memory Room + `MigrationTestHelper`.
- `Clock`/`IdGenerator` luôn được mock → không có `System.currentTimeMillis()`/`UUID.randomUUID()` rải rác trong domain.

---

## 18. Quy trình thêm 1 feature mới

Làm đúng thứ tự (giống base Flutter, chỉ đổi tên lớp):

1. **Domain trước**
   `domain/model/*.kt` (implement `DomainModel`) → `domain/repository/XyzRepository.kt` (interface) →
   `domain/usecase/*.kt` (`UseCase`/`NoInputUseCase`/`FlowUseCase`, `@Inject constructor`) →
   `domain/exception/XyzException.kt` (extends `AppException.CustomException`) nếu cần lỗi nghiệp vụ riêng.
2. **Data**
   `data/local/*Entity.kt` + `*Dao.kt` (thêm `@Entity` vào `AppDatabase.entities`, thêm `abstract fun xDao()`,
   provide DAO trong `DatabaseModule`) → `data/mapper/*Mapper.kt` (`@Inject constructor`) →
   `data/repository/XyzRepositoryImpl.kt` (bọc thao tác ghi bằng `wrapLocal { }`).
3. **Presentation**
   `presentation/<screen>/XyzState.kt` (data class implements `UiState`, có `withStatus`) →
   `XyzViewModel.kt` (`@HiltViewModel`, extends `BaseViewModel<XyzState>`, gọi usecase trong `launchGuarded { }`,
   điều hướng qua `navigator`) → `XyzScreen.kt` (`hiltViewModel()`, `collectAsStateWithLifecycle()`,
   bọc `LoadingOverlay`/`ScreenStateHost` khi cần).
4. **DI feature**: `feature/xyz/di/XyzModule.kt` — chỉ `@Binds` cho repository interface → impl. (UseCase/Mapper/ViewModel không cần khai báo.)
5. **Route**: thêm `@Serializable XyzRoute` vào `AppDestinations.kt` + 1 `composable<XyzRoute>` trong `AppNavHost`.
6. **Xong** — không có bước "đăng ký vào main". Hilt sinh graph lúc build.

Bất biến:
- `domain/` không import `androidx.room`, `dagger.hilt.*` runtime, `androidx.compose.*`, `com.google.firebase.*`.
- `data/` không import `presentation/`.
- Điều hướng/dialog/snackbar trong ViewModel **chỉ** qua `navigator`.
- Đọc dữ liệu = `Flow` từ DAO; không "tự refresh".
- Field SM-2 và các field sync (`userId/synced/isDeleted/updatedAt`) không thêm sau bản release đầu.

---

## 19. Prompt mẫu — dán vào phiên chat mới

> Copy khối dưới, dán vào đầu conversation mới, đính kèm file `ANDROID_CLEAN_ARCHITECTURE_MVVM_BASE.md`
> này + `docs_tld.md`, rồi mô tả feature cần build.

```
Bạn đang là lập trình viên Android (Kotlin + Jetpack Compose) làm việc theo base kiến trúc Clean
Architecture + MVVM định nghĩa đầy đủ trong ANDROID_CLEAN_ARCHITECTURE_MVVM_BASE.md đính kèm.
Đọc kỹ toàn bộ file đó trước khi code và tuân thủ NGHIÊM NGẶT:

1. Cấu trúc: core/, feature/<feature>/{data,domain,presentation,di}, navigation/, OnTapTldApp.kt,
   MainActivity.kt — giữ đúng như tài liệu (Mục 3), không tự sáng tạo cấu trúc khác.
2. Domain: model implements DomainModel; repository interface ở domain; usecase extends
   UseCase/NoInputUseCase/FlowUseCase với @Inject constructor; exception nghiệp vụ extends
   AppException.CustomException. Domain KHÔNG import room/hilt-runtime/compose/firebase.
3. Data: Room @Entity + DAO (read trả Flow, write suspend, kế thừa BaseDao); mapper implements
   EntityMapper/ModelMapper; repository impl bọc ghi bằng wrapLocal{}. Data KHÔNG import presentation.
4. Presentation: mỗi màn 1 data class State implements UiState (có withStatus) + 1 @HiltViewModel
   extends BaseViewModel<State>; gọi usecase trong launchGuarded{}; Screen dùng hiltViewModel() +
   collectAsStateWithLifecycle() + LoadingOverlay/ScreenStateHost.
5. Điều hướng/dialog/snackbar trong ViewModel CHỈ qua navigator (AppNavigator) — không chạm
   NavController/Context.
6. DI: mỗi feature 1 XyzModule chỉ @Binds repository; singleton chung ở core/di/*. Route mới thêm
   @Serializable route + composable<> trong AppNavHost. KHÔNG có bước "đăng ký vào main".
7. KHÔNG có REST/Retrofit. Mạng chỉ ở GeminiClient (core/data/ai) và Sprint 2 sync. AI luôn đi qua
   AiQuotaGuard.ensureCanCall() trước khi gọi.
8. Thư viện: chỉ dùng nhóm đã liệt kê ở Mục 16; thêm lib mới phải nêu lý do. Không hard-code
   package/tên app dự án khác.
9. Coroutine: catch CancellationException thì rethrow; mọi lỗi khác trong ViewModel để launchGuarded lo.
   Thời gian/UUID trong domain lấy qua Clock/IdGenerator (mock được), không gọi trực tiếp API hệ thống.
10. Thêm feature theo đúng thứ tự Mục 18: domain → data → presentation → di → route.
11. Thiếu thông tin nghiệp vụ (field, validate rule, prompt AI cụ thể…) thì HỎI LẠI, không tự bịa.

Việc cần làm: <mô tả feature cụ thể>
```

---

## 20. Chừa chỗ cho Sprint 2 (sync + auth)

Sprint 1 **không code** phần này nhưng base đã chuẩn bị sẵn để Sprint 2 không phải sửa schema/kiến trúc:

| Đã sẵn ở Sprint 1 | Sprint 2 dùng để |
| --- | --- |
| `DeckEntity/FlashcardEntity/NoteEntity` có `userId?`, `synced`, `isDeleted`, `updatedAt` | Gán `userId` khi đăng nhập lần đầu (docs_tld 8.6); Last-Write-Wins theo `updatedAt`; soft-delete để sync xoá. |
| DAO đã lọc `isDeleted = 0` ở mọi query đọc | Soft delete hoạt động ngay, không phải sửa query. |
| `core/sync/` (thư mục trống) | `SyncScheduler` (WorkManager định kỳ) + `SyncQueueEntity` + `PushSyncUseCase`/`PullSyncUseCase`. |
| `AppPreferences.lastSyncAtMillis` | Mốc cho Pull (`updatedAt > lastSync`). |
| `feature/auth/` (thư mục trống) | Google Sign-In (`firebase-auth` + `play-services-auth`), màn Xoá tài khoản (docs_tld 8.7). |
| `AppException` là `sealed` | Thêm `data class SyncException(...)` + nhánh trong `GlobalExceptionHandler`, compiler ép xử lý đủ nhánh. |
| `GeminiClient` tách interface | Không đổi gì khi thêm sync — AI độc lập với auth (App Check bảo vệ, không cần user login). |

**Nguyên tắc:** khi bắt đầu Sprint 2, đọc lại Mục 7–8 của `docs_tld.md`, thêm feature theo đúng
quy trình Mục 18 — không "vá ngang".

---

*Hết tài liệu.*
