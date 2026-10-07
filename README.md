# On Tap TLD — README cho dev mới

Chào mừng bạn đến với dự án **On Tap TLD** — app Android học từ vựng/kiến thức bằng flashcard,
có thể tạo thẻ **thủ công** (gõ tay) hoặc **tự động** (chụp ảnh → OCR → AI sinh thẻ), rồi ôn tập
theo thuật toán lặp lại ngắt quãng SM-2 (giống Anki).

File này viết cho người **chưa rành Kotlin/Jetpack Compose** — mục tiêu là đọc xong, bạn tự mở
được code, hiểu 1 màn hình chạy như thế nào từ lúc bấm nút tới lúc lưu vào database, và biết thêm
1 màn hình mới đúng khuôn của dự án mà không phá kiến trúc.

> Đây **không phải** tài liệu nghiệp vụ hay tài liệu kiến trúc đầy đủ. Khi đã quen code, đọc thêm:
> - [`docs/docs_tld.md`](docs/docs_tld.md) — đặc tả nghiệp vụ, deadline, phạm vi sản phẩm.
> - [`docs/ANDROID_CLEAN_ARCHITECTURE_MVVM_BASE.md`](docs/ANDROID_CLEAN_ARCHITECTURE_MVVM_BASE.md) —
    tài liệu kiến trúc đầy đủ (chi tiết hơn README này rất nhiều).

---

## Mục lục

1. [Cần cài gì để mở dự án](#1-cần-cài-gì-để-mở-dự-án)
2. [10 khái niệm Kotlin/Compose cần biết trước](#2-10-khái-niệm-kotlincompose-cần-biết-trước)
3. [Bức tranh kiến trúc tổng quan](#3-bức-tranh-kiến-trúc-tổng-quan)
4. [Cấu trúc thư mục](#4-cấu-trúc-thư-mục)
5. [Đi theo 1 luồng thật từ đầu tới cuối](#5-đi-theo-1-luồng-thật-từ-đầu-tới-cuối)
6. [Màn hình hiển thị 3 trạng thái Loading/Error/Empty](#6-màn-hình-hiển-thị-3-trạng-thái-loadingerrorempty)
7. [Điều hướng (chuyển màn hình) hoạt động thế nào](#7-điều-hướng-chuyển-màn-hình-hoạt-động-thế-nào)
8. [Muốn thêm 1 màn hình mới thì làm sao](#8-muốn-thêm-1-màn-hình-mới-thì-làm-sao)
9. [5 luật KHÔNG ĐƯỢC phá](#9-5-luật-không-được-phá)
10. [Chạy & test dự án](#10-chạy--test-dự-án)
11. [Lỗi hay gặp khi mới học Compose](#11-lỗi-hay-gặp-khi-mới-học-compose)
12. [Trạng thái hiện tại của dự án](#12-trạng-thái-hiện-tại-của-dự-án)
13. [Thêm ảnh & đa ngôn ngữ — đối chiếu với Flutter](#13-thêm-ảnh--đa-ngôn-ngữ--đối-chiếu-với-flutter)
14. [Bảng thuật ngữ tra nhanh](#14-bảng-thuật-ngữ-tra-nhanh)

---

## 1. Cần cài gì để mở dự án

- **Android Studio** bản mới (Ladybug trở lên) — đã có sẵn Kotlin, Gradle, emulator quản lý trong
  đó.
- JDK 17 (Android Studio tự mang theo, không cần cài riêng).
- Mở thư mục gốc `OnTapTLD/` bằng Android Studio → chờ Gradle sync xong (lần đầu khá lâu, cần mạng).
- Bấm nút ▶ **Run** (chọn 1 emulator, ví dụ Pixel 7a) — hoặc dùng terminal:

```bash
./gradlew :app:installDebug
```

Không cần cấu hình API key/backend gì để chạy — app **offline-first**, chạy hoàn toàn bằng
database local (Room). Riêng AI sinh thẻ và đồng bộ thì cần Firebase (xem ngay dưới).

### Firebase (tuỳ chọn khi chỉ muốn chạy thử)

File `app/google-services.json` **không được commit** (nằm trong `.gitignore`), nên repo mới clone
về sẽ không có nó. Build vẫn chạy bình thường: `app/build.gradle.kts` chỉ apply plugin
`google-services` khi thấy file, và `OnTapTldApp` tự bỏ qua Firebase nếu không có cấu hình —
khi đó mọi phần offline (bộ thẻ, thẻ thủ công, ôn tập) vẫn dùng được, chỉ AI/đồng bộ là không.

Muốn bật Firebase trên máy mình:

1. Xin file `google-services.json` từ người giữ dự án (hoặc tải từ Firebase Console → Project
   settings → Your apps, project `on-tap-tld`), đặt vào thư mục `app/`.
2. Chạy bản debug một lần, mở Logcat lọc theo tag `DebugAppCheckProvider`, chép debug token được
   in ra.
3. Dán token vào Firebase Console → App Check → Apps → menu ⋮ → **Manage debug tokens**. App Check
   đang bật Enforce cho AI Logic, nên thiếu bước này mọi lần gọi AI từ máy dev đều bị từ chối.
4. Máy mới cũng cần đăng ký SHA-1/SHA-256 của debug keystore (`./gradlew signingReport`) trong
   Project settings thì đăng nhập Google mới chạy.

---

## 2. 10 khái niệm Kotlin/Compose cần biết trước

Không cần học hết Kotlin mới đọc được code này. Biết 10 điều dưới đây là đủ bắt đầu.

**Kotlin cơ bản**

1. **`val` vs `var`** — `val` là gán 1 lần rồi không đổi được (giống `final`), `var` là biến đổi
   được.
   Codebase này ưu tiên `val` gần như tuyệt đối.
2. **`data class`** — 1 class chỉ để giữ dữ liệu, Kotlin tự sinh `equals`, `toString`, và hàm
   `copy()` để tạo bản sao đã sửa vài field. Ví dụ dùng liên tục trong dự án:
   ```kotlin
   data class DeckListState(val decks: List<Deck> = emptyList())
   val newState = oldState.copy(decks = newDecks) // giữ nguyên field khác, chỉ đổi decks
   ```
3. **`?` (nullable) và `?.` / `?:`** — 1 kiểu có thể có `?` sau nó nghĩa là "có thể null" (vd
   `String?`). `x?.foo()` = "nếu x không null thì gọi foo()". `x ?: y` = "nếu x null thì dùng y".
4. **Hàm mở rộng & lambda `{ }`** — cặp ngoặc nhọn `{ ... }` sau tên hàm là 1 khối code truyền vào
   như tham số (giống callback). `list.map { it.name }` nghĩa là "với mỗi phần tử, lấy `name`".
5. **`suspend fun` + coroutine** — hàm đánh dấu `suspend` là hàm **chạy bất đồng bộ** (vd đọc
   database, gọi mạng) nhưng viết THẲNG như code tuần tự, không lồng callback địa ngục. Bạn chỉ
   cần biết: hàm `suspend` phải được gọi từ 1 coroutine khác (`viewModelScope.launch { ... }`) —
   dự án đã bọc sẵn việc này trong `launchGuarded { }`, bạn thường không tự viết `launch` tay.

**Jetpack Compose cơ bản**

6. **`@Composable` function = 1 khối UI** — thay vì viết file XML layout, Compose để bạn viết UI
   bằng hàm Kotlin bình thường, đánh dấu `@Composable`. Gọi hàm khác có `@Composable` bên trong =
   "nhúng" UI con vào UI cha. Ví dụ `Text("Xin chào")` chính là 1 lời gọi hàm.
7. **State + Recomposition** — Compose tự vẽ lại (gọi là "recompose") 1 phần UI khi dữ liệu nó
   đọc thay đổi. Trong dự án, dữ liệu đó là `StateFlow` của ViewModel, đọc bằng
   `collectAsStateWithLifecycle()`. Bạn **không tự sửa** biến UI trực tiếp — luôn gọi hàm trên
   ViewModel để đổi state, UI sẽ tự vẽ lại.
8. **`Modifier`** — tham số `modifier: Modifier` gần như component nào cũng có, dùng để chỉnh
   padding/size/click/scroll... Ví dụ `Modifier.fillMaxWidth().padding(16.dp)`.
9. **`Flow` / `StateFlow`** — hiểu đơn giản là "1 dòng nước dữ liệu chảy theo thời gian", bạn
   `collect` (hứng nước) để nhận giá trị mới mỗi khi có. `StateFlow` là `Flow` luôn có sẵn 1 giá
   trị hiện tại (khác `Flow` thường phải chờ mới có).
10. **Hilt (Dependency Injection)** — thay vì tự `MyRepository()` ở khắp nơi, bạn khai
    `@Inject constructor(...)` trên class, Hilt tự tạo và "bơm" instance đúng chỗ cần (constructor
    của ViewModel, Repository...). Bạn chỉ cần biết: thấy `@Inject constructor` hay
    `@HiltViewModel` nghĩa là **không tự new object đó**, cứ khai báo nó làm tham số là có.

---

## 3. Bức tranh kiến trúc tổng quan

Dự án theo **Clean Architecture + MVVM**, tách thành 4 lớp, luôn đi 1 chiều:

```
Người dùng bấm nút
        │
        ▼
┌───────────────────┐   Compose Screen: chỉ vẽ UI + gọi hàm trên ViewModel khi có sự kiện
│   presentation/    │   (Screen.kt, State.kt, ViewModel.kt)
└─────────┬──────────┘
          │ gọi UseCase
          ▼
┌───────────────────┐   1 UseCase = 1 hành động nghiệp vụ (vd "tạo thẻ", "xoá deck")
│      domain/       │   KHÔNG import Android/Compose/Room/Hilt gì hết — code Kotlin thuần
└─────────┬──────────┘
          │ gọi qua interface Repository
          ▼
┌───────────────────┐   Repository thật (Impl) đọc/ghi Room, map Entity ⇄ Model
│       data/        │   (DeckDao, FlashcardEntity, DeckRepositoryImpl...)
└─────────┬──────────┘
          ▼
      Room (SQLite)
```

Vì sao tách vậy: đổi UI không đụng vào logic nghiệp vụ, đổi database không đụng vào ViewModel, và
lớp `domain/` test được mà không cần khởi động cả Android (test rất nhanh, không cần emulator).

**Chiều dữ liệu đọc thì ngược lại** — không phải "gọi rồi trả 1 lần", mà Room phát ra 1 `Flow`,
chảy ngược lên UI, UI tự vẽ lại mỗi khi dữ liệu đổi:

```
Room table → DAO Flow<...> → Repository.observeX() → UseCase → ViewModel.setState → UI tự vẽ lại
```

Đây là lý do khi bạn thêm 1 deck mới, màn danh sách deck **tự động** hiện thêm dòng mới mà
`DeckListScreen` không cần code gì để "refresh" — nó chỉ đang lắng nghe Room suốt.

---

## 4. Cấu trúc thư mục

```
app/src/main/java/com/ledinhthi/ontaptld/
├── core/                      # Code DÙNG CHUNG cho mọi feature
│   ├── domain/                #   UseCase base class, Clock/IdGenerator (bọc thời gian & UUID)
│   ├── data/local/db/         #   AppDatabase (Room), BaseDao, wrapLocal (bắt lỗi Room)
│   ├── data/local/prefs/      #   DataStore (lưu setting nhỏ, kiểu SharedPreferences mới)
│   ├── presentation/mvi/      #   BaseViewModel, UiState/UiStatus — MỌI ViewModel kế thừa cái này
│   ├── presentation/navigation# AppNavigator — cách ViewModel chuyển màn mà không cầm NavController
│   ├── presentation/components# LoadingOverlay, ScreenStateHost — UI dùng lại ở nhiều màn
│   ├── presentation/theme/    #   Màu sắc, font, khoảng cách (Color.kt, Dimens.kt, Type.kt...)
│   ├── exception/             #   Cây lỗi AppException + nơi quyết định lỗi nào hiện dialog/snackbar
│   └── di/                    #   Khai báo Hilt (chỗ Hilt biết cách tạo DAO, Repository...)
│
├── feature/                   # Mỗi tính năng lớn = 1 thư mục, tự có đủ 4 lớp domain/data/presentation
│   ├── deck/                  #   Deck & Flashcard CRUD — feature "mẫu" đầy đủ nhất, đọc cái này trước
│   ├── review/                #   Ôn tập theo SM-2
│   ├── auth/presentation/login/ # Màn đăng nhập (UI xong, chưa nối backend thật — xem Mục 12)
│   ├── settings/, splash/     #   Nhỏ/placeholder
│
├── navigation/                # AppDestinations (khai tên các màn), AppNavHost (màn nào ứng route nào)
├── OnTapTldApp.kt             # Điểm khởi động app (bật Hilt + Timber log)
└── MainActivity.kt            # Duy nhất 1 Activity — mọi màn hình đều là Composable bên trong nó
```

**Mẹo đọc code:** trong 1 feature, tên file luôn theo khuôn `XState.kt` (dữ liệu màn hình),
`XViewModel.kt` (xử lý logic + lắng nghe sự kiện từ Screen), `XScreen.kt` (vẽ UI). Cứ mở đủ 3 file
này là hiểu trọn 1 màn hình.

---

## 5. Đi theo 1 luồng thật từ đầu tới cuối

Ví dụ dễ nhất để hiểu cả kiến trúc: màn **"Thêm thẻ thủ công"**
(`feature/deck/presentation/manualcard/`). Người dùng gõ câu hỏi/câu trả lời, bấm "Lưu".

**Bước 1 — UI phát sự kiện
** ([ManualCardScreen.kt](app/src/main/java/com/ledinhthi/ontaptld/feature/deck/presentation/manualcard/ManualCardScreen.kt)):

```kotlin
Button(onClick = viewModel::onSave) { Text("Lưu") }
```

`Screen` không tự lưu gì cả — nó chỉ gọi hàm `onSave()` trên ViewModel. Đây là quy tắc chung: **UI
không bao giờ tự chứa logic**, chỉ hiển thị `state` và gọi hàm trên `viewModel`.

**Bước 2 — ViewModel xử lý
** ([ManualCardViewModel.kt](app/src/main/java/com/ledinhthi/ontaptld/feature/deck/presentation/manualcard/ManualCardViewModel.kt)):

```kotlin
fun onSave() = launchGuarded(showLoadingOverlay = true) {
    createManualFlashcard(
        CreateManualFlashcardUseCase.Params(
            deckId = args.deckId,
            question = currentState.question,
            answer = currentState.answer,
        )
    )
    navigator.showSnackBar("Đã thêm thẻ.")
    navigator.back()
}
```

`launchGuarded { }` là hàm có sẵn ở `BaseViewModel` mà **mọi ViewModel đều kế thừa** — nó tự bật
vòng xoay loading, chạy code bên trong, bắt lỗi nếu có, rồi tắt loading. Bạn không cần tự viết
`try/catch` hay tự bật/tắt cờ loading ở từng nơi.

**Bước 3 — UseCase thực hiện nghiệp vụ
** ([CreateManualFlashcardUseCase.kt](app/src/main/java/com/ledinhthi/ontaptld/feature/deck/domain/usecase/CreateManualFlashcardUseCase.kt)):

```kotlin
override suspend fun invoke(input: Params): Flashcard {
    if (q.isEmpty() || a.isEmpty()) throw DeckException(DeckException.Kind.BLANK_CARD)
    val card = Flashcard(id = ids.newId(), ..., dueDate = now, ...)
    repository.upsert(card)
    return card
}
```

Đây là nơi **luật nghiệp vụ** sống (vd "không cho lưu thẻ trống") — hoàn toàn không biết Compose
hay Room là gì, chỉ biết `FlashcardRepository` (1 interface).

**Bước 4 — Repository ghi xuống Room
** ([FlashcardRepositoryImpl.kt](app/src/main/java/com/ledinhthi/ontaptld/feature/deck/data/repository/FlashcardRepositoryImpl.kt)):

```kotlin
override suspend fun upsert(card: Flashcard) = wrapLocal { dao.upsert(mapper.toEntity(card)) }
```

`mapper.toEntity(card)` đổi model nghiệp vụ (`Flashcard`) thành model database (`FlashcardEntity`
— có thêm field như `synced`, `isDeleted` mà tầng trên không cần quan tâm). `dao.upsert(...)` là
Room tự sinh code SQL `INSERT OR REPLACE` giúp bạn.

**Bước 5 — UI khác tự cập nhật, không cần ai gọi:** màn `DeckDetailScreen` đang
`observeCards(deckId)` (1 `Flow` từ Room) — ngay khi dòng mới được ghi, Room phát giá trị mới
xuống Flow, chảy tới `setState { copy(cards = cards) }`, `collectAsStateWithLifecycle()` nhận
state mới, Compose tự vẽ lại danh sách thẻ có thêm thẻ mới — **không có dòng code "refresh" nào
cả**.

Đọc thuộc luồng 5 bước này rồi thì mọi feature khác trong dự án (`decklist`, `deckdetail`, và
`auth/login` vừa thêm) đều đi đúng khuôn y hệt, chỉ khác tên.

---

## 6. Màn hình hiển thị 3 trạng thái Loading/Error/Empty

Mọi màn danh sách trong app đều phải xử lý đủ 3 trạng thái: đang tải / lỗi / rỗng. Thay vì mỗi màn
tự viết `if/else` riêng, dự án có sẵn [
`ScreenStateHost`](app/src/main/java/com/ledinhthi/ontaptld/core/presentation/components/ScreenStateHost.kt):

```kotlin
ScreenStateHost(
    isLoading = state.status.isLoading,
    items = state.decks,
    error = state.status.exceptionWrapper?.let { "Không tải được danh sách deck." },
    onRetry = { /* ... */ },
    emptyText = "Chưa có deck nào. Bấm + để tạo deck đầu tiên.",
) { decks ->
    LazyColumn { items(decks) { ... } } // chỉ code phần "có dữ liệu"
}
```

Bạn chỉ cần lo phần UI khi **có dữ liệu** (lambda cuối) — 3 trạng thái còn lại `ScreenStateHost` lo
sẵn. Còn khi thao tác (không phải load danh sách) cần chặn UI + hiện vòng xoay giữa màn hình (vd
lúc bấm "Lưu"), dùng [
`LoadingOverlay`](app/src/main/java/com/ledinhthi/ontaptld/core/presentation/components/LoadingOverlay.kt)
bọc quanh cả `Scaffold` — xem cách `ManualCardScreen`/`LoginScreen`
đang dùng.

---

## 7. Điều hướng (chuyển màn hình) hoạt động thế nào

Có 1 điểm dễ gây nhầm: **ViewModel không bao giờ cầm `NavController`**. Thay vào đó nó gọi
`navigator.to(...)`, `navigator.back()`, `navigator.replaceAll(...)` — `navigator` là
[
`AppNavigator`](app/src/main/java/com/ledinhthi/ontaptld/core/presentation/navigation/AppNavigator.kt),
1 interface không biết gì về Compose Navigation. Lý do: ViewModel test được mà không cần dựng cả
màn hình Compose thật.

Nơi DUY NHẤT thật sự cầm `NavController` là [
`AppNavHost.kt`](app/src/main/java/com/ledinhthi/ontaptld/navigation/AppNavHost.kt) — nó lắng nghe
các "lệnh" từ `navigator` rồi mới gọi `navController.navigate(...)`.

Muốn thêm 1 route mới, luôn sửa đúng 2 chỗ:

1. Khai tên route trong [
   `AppDestinations.kt`](app/src/main/java/com/ledinhthi/ontaptld/navigation/AppDestinations.kt) (vd
   `data object LoginRoute`).
2. Đăng ký `composable<LoginRoute> { LoginScreen() }` trong `AppNavHost.kt`.

> Lưu ý kỹ thuật (đã từng gây bug thật trong dự án): sự kiện điều hướng đi qua 1 `Channel`, không
> phải biến thường — nghĩa là **gọi `navigator.to(...)` xong không có gì hiển thị ngay lập tức**,
> nó chờ `AppNavHost` xử lý ở khung hình kế tiếp. Bình thường bạn không cần để ý điều này, chỉ cần
> biết nếu thấy màn hình "đứng yên không chuyển" sau khi gọi navigator, đây là chỗ đầu tiên nên
> nghi ngờ.

---

## 8. Muốn thêm 1 màn hình mới thì làm sao

Copy đúng khuôn của 1 feature nhỏ có sẵn (`manualcard` hoặc `auth/presentation/login`) là nhanh
nhất. Các bước:

1. Tạo thư mục `feature/<ten>/presentation/<man>/` với 3 file `XState.kt`, `XViewModel.kt`,
   `XScreen.kt` (xem khuôn ở Mục 5).
2. `XState` implement `UiState`, có `status: UiStatus = UiStatus()` + field dữ liệu riêng của màn.
3. `XViewModel` kế thừa `BaseViewModel<XState>(XState(), toolbox)`, nhận `ViewModelToolbox` qua
   constructor (Hilt tự bơm) — không tự tạo `AppNavigator` hay `GlobalExceptionHandler` tay.
4. `XScreen` nhận `viewModel: XViewModel = hiltViewModel()`, đọc
   `viewModel.uiState.collectAsStateWithLifecycle()`, KHÔNG tự giữ state nghiệp vụ bằng
   `remember { mutableStateOf(...) }` (chỉ dùng `remember` cho state thuần UI như "dialog đang mở
   hay đóng", state nghiệp vụ luôn nằm trong ViewModel).
5. Nếu cần gọi database/logic mới, viết 1 `UseCase` mới trong `domain/usecase/`, KHÔNG gọi thẳng
   `Repository` hay DAO từ ViewModel.
6. Thêm route vào `AppDestinations.kt` + `AppNavHost.kt` như Mục 7.
7. Build thử: `./gradlew :app:compileDebugKotlin`.

---

## 9. 5 luật KHÔNG ĐƯỢC phá

Đọc kỹ file [
`docs/ANDROID_CLEAN_ARCHITECTURE_MVVM_BASE.md`](docs/ANDROID_CLEAN_ARCHITECTURE_MVVM_BASE.md)
nếu muốn hiểu vì sao — ở đây chỉ liệt kê để nhớ khi code:

1. **`domain/` không bao giờ `import androidx.*` / `hilt.*` / `room.*` / compose.** Nếu 1 file
   trong `domain/usecase` hay `domain/model` cần import Compose/Room để chạy, bạn đang đặt sai
   thư mục.
2. **`data/` không bao giờ import từ `presentation/`.** Repository/DAO không được biết ViewModel
   hay Screen là gì.
3. **ViewModel không cầm `NavController`/`Context`.** Mọi điều hướng/dialog/snackbar đi qua
   `navigator` (Mục 7).
4. **UI (`Screen.kt`) không chứa logic nghiệp vụ**, chỉ đọc `state` và gọi hàm `viewModel.xxx()`.
5. **Mọi thao tác có thể lỗi (đọc/ghi Room, gọi AI...) bọc trong `launchGuarded { }`**, không tự
   viết `try/catch` rải rác trong ViewModel.

---

## 10. Chạy & test dự án

```bash
# Build thử code có lỗi compile không (nhanh nhất, dùng khi vừa sửa xong)
./gradlew :app:compileDebugKotlin

# Chạy toàn bộ unit test (test nằm ở app/src/test/)
./gradlew :app:testDebugUnitTest

# Build + cài lên emulator/thiết bị đang mở
./gradlew :app:installDebug
```

Unit test trong dự án **không cần emulator** vì `domain/` không đụng Android — chạy trong vài giây.
Ví dụ đáng đọc để học cách test: `Sm2CalculatorTest`, `CreateDeckUseCaseTest`.

---

## 11. Lỗi hay gặp khi mới học Compose

- **Sửa 1 biến `var` thường trong Composable mà UI không cập nhật** → phải dùng
  `remember { mutableStateOf(...) }` (cho state thuần UI) hoặc state từ ViewModel (cho state
  nghiệp vụ), không phải biến Kotlin thường.
- **Quên `collectAsStateWithLifecycle()`** khi đọc `StateFlow` trong Composable → UI sẽ không tự
  vẽ lại khi state đổi.
- **Gọi hàm nghiệp vụ trực tiếp trong `Screen.kt`** (vd gọi thẳng Repository) thay vì gọi qua
  `viewModel.xxx()` → phá kiến trúc, khó test, và thường là dấu hiệu đặt sai lớp.
- **Tưởng gọi `navigator.to(...)` là chuyển màn ngay lập tức** → xem lưu ý ở Mục 7.
- **Vòng lặp vô hạn re-compose** (màn hình giật/lag) thường do tạo object mới (list, lambda) ngay
  trong thân Composable mỗi lần vẽ lại thay vì dùng `remember`.

---

## 12. Trạng thái hiện tại của dự án

> Phần này thay đổi nhanh theo tiến độ — coi đây là ảnh chụp nhanh, không phải nguồn sự thật.
> Muốn chắc chắn, xem `git log` hoặc hỏi người đang giữ dự án.

- **Đã có đầy đủ:** base kiến trúc (Mục 3), feature `deck` (tạo/sửa/xoá deck + thẻ thủ công),
  `Sm2Calculator` (thuật toán ôn tập, đã unit test), màn `splash`, UI màn `login`
  (`feature/auth/presentation/login/`) — **UI xong nhưng chưa nối backend thật**, bấm "Đăng nhập"
  hợp lệ hiện tại chỉ demo rồi vào thẳng Home.
- **Chưa làm:** chụp ảnh + OCR + AI sinh thẻ (`feature/capture`), UI ôn tập thật, widget màn hình
  chính, đăng nhập Google + đồng bộ Cloud thật (Sprint 2 — xem `docs/docs_tld.md` Mục 3).
- **Firebase/AI:** đang dùng bản giả (`StubGeminiClient`) vì repo chưa có `google-services.json`.

---

## 13. Thêm ảnh & đa ngôn ngữ — đối chiếu với Flutter

Bên Flutter (TLD Tracker), ảnh khai trong `pubspec.yaml` (`assets: - assets/images/`) rồi gọi
`Image.asset('assets/images/x.png')`; chữ đa ngôn ngữ nằm trong file `.arb` (`app_vi.arb`,
`app_en.arb`), sinh code bằng `flutter gen-l10n`, gọi bằng `AppLocalizations.of(context).loginTitle`.

Android **không cần khai báo file nào cả** — cứ để đúng tên/đúng thư mục quy ước, Android Studio
tự quét và sinh ra class `R` (viết tắt "Resource") chứa hằng số trỏ tới từng file/từng key, lúc
build. Bảng đối chiếu:

| Flutter (TLD Tracker) | Android (dự án này) | Ghi chú |
| --- | --- | --- |
| `pubspec.yaml` khai `assets: - assets/images/` | Không cần khai gì — thả file đúng thư mục | Android tự quét `res/` lúc build |
| `assets/images/logo.png` | `res/drawable/logo.png` (hoặc `.xml` nếu là vector) | Tên file **chỉ chữ thường, số, `_`** — không dấu cách/hoa/gạch ngang |
| `Image.asset('assets/images/logo.png')` | `painterResource(R.drawable.logo)` trong Compose | `R.drawable.<tên file bỏ đuôi>` |
| `app_vi.arb` / `app_en.arb` (JSON key-value) | `res/values/strings.xml` (mặc định) + `res/values-en/strings.xml` | Thư mục `values-<mã ngôn ngữ>` = 1 bản dịch |
| `flutter gen-l10n` sinh class | AGP tự sinh class `R` — **không cần lệnh gen riêng**, chỉ cần build |
| `AppLocalizations.of(context).loginTitle` | `stringResource(R.string.login_title)` (trong `@Composable`) |
| Placeholder `{name}` trong `.arb` | Placeholder `%1$s`, `%2$d`... trong `strings.xml`, truyền qua `stringResource(id, arg)` |

### 13.1 Cách thêm 1 ảnh mới

1. Xuất ảnh từ Figma. Nếu Figma cho xuất **SVG** → ưu tiên chọn SVG (Android vẽ lại bằng vector,
   nét luôn sắc dù màn hình to nhỏ, 1 file dùng cho mọi độ phân giải — không như PNG phải xuất
   nhiều size). Nếu chỉ có PNG (ảnh chụp/minh hoạ phức tạp) thì dùng PNG/WebP bình thường.
2. Trong Android Studio: chuột phải `app/res` → `New` → `Vector Asset` (nếu có SVG, chọn "Local
   file" và trỏ tới file SVG — Studio tự convert sang `.xml`) hoặc kéo thả file PNG/WebP thẳng vào
   thư mục `app/src/main/res/drawable/`.
3. Đặt tên file theo quy ước: chữ thường + `_`, có tiền tố theo loại để dễ tìm — dự án này đang
   dùng `ic_launcher_foreground.xml` (icon) làm ví dụ; nên theo mẫu `ic_<tên>` cho icon,
   `img_<tên>` cho ảnh minh hoạ.
4. Dùng trong Compose:
   ```kotlin
   Image(
       painter = painterResource(R.drawable.img_ten_anh),
       contentDescription = null, // hoặc mô tả cho accessibility nếu ảnh có ý nghĩa (không phải trang trí)
   )
   ```
5. Nếu bắt buộc dùng PNG/JPG (không có vector) và cần nét đẹp trên mọi máy, xuất thêm các bản độ
   phân giải khác nhau, bỏ vào `drawable-mdpi/`, `drawable-hdpi/`, `drawable-xhdpi/`,
   `drawable-xxhdpi/`, `drawable-xxxhdpi/` — CÙNG 1 tên file, khác thư mục. Android tự chọn đúng
   bản theo mật độ điểm ảnh của máy. Nếu chỉ có 1 bản, để thẳng vào `drawable/` không phân
   density cũng chạy được, chỉ là ảnh có thể hơi mờ/nặng trên máy màn hình rất nét.

> `app/src/main/assets/` (khác `res/`) cũng tồn tại nhưng KHÔNG dùng cho ảnh hiển thị UI — đó là
> nơi để file thô đọc bằng đường dẫn tay (font, JSON mẫu, model AI...), không đi qua `R` nên không
> tự động theo density/theme. Ảnh UI luôn ưu tiên `res/drawable/`.

### 13.2 Cách thêm 1 chuỗi chữ (localkey) / 1 ngôn ngữ mới

Ví dụ thật đã làm trong dự án — màn Login vừa đổi từ chuỗi cứng (`Text("Đăng nhập")`) sang key:

**`res/values/strings.xml`** (ngôn ngữ mặc định — ở đây là tiếng Việt):
```xml
<string name="login_title">Đăng nhập</string>
<string name="login_subtitle">Đăng nhập để tiếp tục học cùng %1$s</string>
```

**`res/values-en/strings.xml`** (bản dịch tiếng Anh — chỉ cần khai key nào muốn dịch, key nào
thiếu Android tự lấy lại bản mặc định):
```xml
<string name="login_title">Sign in</string>
<string name="login_subtitle">Sign in to keep learning with %1$s</string>
```

**Dùng trong Compose** ([LoginScreen.kt](app/src/main/java/com/ledinhthi/ontaptld/feature/auth/presentation/login/LoginScreen.kt)):
```kotlin
Text(text = stringResource(R.string.login_title))
Text(text = stringResource(R.string.login_subtitle, stringResource(R.string.app_name)))
```

Muốn thêm 1 ngôn ngữ mới (vd tiếng Nhật): tạo thư mục `res/values-ja/strings.xml`, copy hết key từ
`res/values/strings.xml` sang rồi dịch giá trị — KHÔNG đổi tên `name`. Android tự chọn đúng file
theo ngôn ngữ máy đang đặt (Cài đặt hệ thống → Ngôn ngữ), không cần code gì thêm.

**Tự kiểm tra không cần đổi ngôn ngữ máy thật:** trên emulator có thể đổi ngôn ngữ hệ thống bằng
lệnh (dùng để test bản dịch em vừa thêm cho màn Login, đã tự chạy thử và chụp lại — kết quả đúng):
```bash
adb shell settings put system system_locales en-US
adb shell am force-stop com.ledinhthi.ontaptld && adb shell am start -n com.ledinhthi.ontaptld/.MainActivity
```

**Lưu ý quan trọng — không phải chỗ nào cũng đổi sang `stringResource` được ngay:** `stringResource()`
chỉ gọi được trong hàm `@Composable` (tức trong `Screen.kt`). Các message tạo ra ở tầng
`ViewModel`/`domain` (vd lỗi validate trong `LoginViewModel`, message trong
`GlobalExceptionHandler`) **không có `Context`** nên chưa thể gọi thẳng — đây là lý do dự án vẫn
để literal tiếng Việt ở tầng đó (xem comment trong `GlobalExceptionHandler.kt`). Theo đúng kế hoạch
đã ghi ở [`docs/ANDROID_CLEAN_ARCHITECTURE_MVVM_BASE.md`](docs/ANDROID_CLEAN_ARCHITECTURE_MVVM_BASE.md#6-exception-layer--exceptionhandler),
việc này để dành khi làm đa ngôn ngữ thật (Sprint 2): tạo 1 interface `StringProvider` (bọc
`context.getString(id, ...)`) rồi truyền qua constructor ViewModel — lúc đó `domain`/`ViewModel`
vẫn không cần biết Compose/Android là gì, chỉ gọi qua interface.

---

## 14. Bảng thuật ngữ tra nhanh

| Thuật ngữ                        | Nghĩa ngắn gọn                                                                                 |
|----------------------------------|------------------------------------------------------------------------------------------------|
| Composable (`@Composable`)       | 1 hàm Kotlin vẽ ra 1 phần UI, thay cho file XML layout cũ                                      |
| Recomposition                    | Compose tự "vẽ lại" 1 phần UI khi dữ liệu nó phụ thuộc thay đổi                                |
| `remember`                       | Giữ 1 giá trị sống qua các lần vẽ lại (không bị tạo mới mỗi lần)                               |
| `Modifier`                       | Tham số chỉnh kích thước/khoảng cách/hành vi của 1 component Compose                           |
| Coroutine                        | Cách viết code chạy nền/bất đồng bộ mà đọc như code tuần tự bình thường                        |
| `suspend fun`                    | Hàm chỉ gọi được từ 1 coroutine, dùng cho việc "chờ" (đọc DB, gọi mạng...)                     |
| `Flow` / `StateFlow`             | 1 luồng dữ liệu phát ra nhiều giá trị theo thời gian, UI lắng nghe (collect)                   |
| ViewModel                        | Nơi giữ state của 1 màn hình + xử lý logic khi người dùng thao tác, sống sót qua xoay màn hình |
| MVVM / MVI                       | Cách tổ chức code: View (UI) ⇄ ViewModel (state + logic) ⇄ Model (dữ liệu)                     |
| UseCase                          | 1 class = 1 hành động nghiệp vụ duy nhất (vd "tạo thẻ", "xoá deck")                            |
| Repository                       | Lớp trung gian che giấu nguồn dữ liệu thật (Room/API) khỏi tầng nghiệp vụ                      |
| Room                             | Thư viện của Google giúp thao tác SQLite bằng Kotlin thay vì viết SQL tay hoàn toàn            |
| DAO                              | "Data Access Object" — interface khai các câu lệnh SQL, Room tự sinh code chạy                 |
| Entity (Room)                    | 1 `data class` ánh xạ trực tiếp 1 bảng trong database                                          |
| Hilt / DI (Dependency Injection) | Thư viện tự tạo & "bơm" các object cần dùng, bạn không tự `new` tay                            |
| `@Inject constructor`            | Đánh dấu Hilt biết cách tự tạo instance của class này                                          |
| `@HiltViewModel`                 | Đánh dấu 1 ViewModel để Hilt tự tạo, dùng cùng `hiltViewModel()` trong Compose                 |
| Navigation Compose               | Thư viện điều hướng giữa các Composable (giống chuyển Activity/Fragment ngày xưa)              |
| Clean Architecture               | Cách tách code thành lớp domain/data/presentation, lớp trong không phụ thuộc lớp ngoài         |
