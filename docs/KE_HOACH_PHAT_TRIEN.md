# KẾ HOẠCH PHÁT TRIỂN — ON TAP TLD

Chốt ngày: 7 tháng 10, 2026
Người thực hiện: Thi + Claude

Tài liệu này ghi lại kế hoạch đã thống nhất. Nó **thay thế cách chia 2 sprint** trong
[`docs_tld.md`](docs_tld.md) Mục 3, 12, 13: làm gộp toàn bộ tính năng trong một mạch, và
**tạm gác phần build / phát hành** (Closed Testing, Store Listing, nộp Production) — Thi tự làm
sau khi phát triển xong.

- Đặc tả nghiệp vụ: [`docs_tld.md`](docs_tld.md)
- Kiến trúc: [`ANDROID_CLEAN_ARCHITECTURE_MVVM_BASE.md`](ANDROID_CLEAN_ARCHITECTURE_MVVM_BASE.md)
- Bản thiết kế UI/UX (56 màn, 2 trang Light / Dark theme): https://claude.ai/artifact/Xrgp4GyHTjyrgRePczrBPx

---

## 1. Hiện trạng repo (tại ngày chốt)

| Phần | Trạng thái |
| --- | --- |
| Base Clean Architecture + MVVM, Hilt, Room, DataStore, navigation | Đã có |
| Theme sáng / tối, bảng màu, thang chữ, khoảng cách | Đã có, cần thêm vài token theo design |
| Logic bộ thẻ + thẻ thủ công (use case, repository, DAO) | Đã có |
| UI Home / Chi tiết bộ thẻ / Thêm thẻ | UI tạm, chưa theo design |
| Thuật toán SM-2 + `ReviewFlashcardUseCase` | Đã có, có unit test |
| UI ôn tập | Chưa có |
| Chụp ảnh, OCR, AI thật | Chưa có (đang dùng `StubGeminiClient`) |
| Cài đặt | Màn trống |
| Splash | Đang nhảy vào màn Login email — cần đổi vào thẳng Home |
| Schema Room v1 | Có sẵn cột đồng bộ (`userId`, `synced`, `isDeleted`, `updatedAt`), **chưa có bảng `sync_queue`** |
| Widget, nhắc ôn, đăng nhập, đồng bộ, thống kê, onboarding, đa ngôn ngữ | Chưa có |

---

## 2. Các bước

Bước 1–3 không cần Firebase. Bước 4 và 6 cần Thi cấu hình Firebase xong trước.

### Bước 0 — Cấu hình Firebase (Thi, làm song song với bước 1)

Xong ngày 7/10/2026 — project `on-tap-tld`, gói Spark (miễn phí).

- [x] Tạo project Firebase, thêm app Android với package `com.ledinhthi.ontaptld`
- [x] Bật AI Logic (nhà cung cấp Gemini Developer API) và App Check (Play Integrity, **đã Enforce cho AI Logic** — bản debug phải đăng ký debug token ở App Check → Manage debug tokens thì mới gọi được AI)
- [x] Bật Auth (Google Sign-In, đã đăng ký SHA của debug keystore) và Firestore (đã đặt Security Rules theo Mục 7.2)
- [x] Đặt `google-services.json` vào thư mục `app/` (chưa commit; sẽ thêm vào `.gitignore` ở bước 1)

### Bước 1 — Nền

Code xong ngày 7/10/2026 trên nhánh `feat/foundation` (chưa commit) — chờ Thi review.

**Claude**
- [x] Thêm token màu mới theo design vào `Color.kt` (cam đậm, xanh đậm, đỏ đậm cho chữ trên nền nhạt; các tông dark tương ứng); ánh xạ đủ slot Material3 trong `Theme.kt`; thang chữ mới trong `Type.kt`
- [x] Font Nunito Sans: `res/font/nunito_sans.ttf` là **variable font** (kho Google Fonts không còn file tĩnh từng độ đậm); giấy phép OFL ở `assets/licenses/`. Ở Android 7.x (API 24–25) mọi độ đậm rơi về Regular
- [x] Component dùng chung trong `core/presentation/components/`: `PrimaryButton` / `SecondaryButton` / `AppTextButton` / `AppIconButton`, `AppCard`, `AppBadge` / `StatusPill`, `AppTopBar`, `EmptyState` / `ErrorState` / `LoadingState` / `SkeletonBlock`; `ScreenStateHost` nhận thêm slot `loading` / `empty`
- [x] Bộ icon nét (17 cái) dạng vector drawable `res/drawable/ic_*.xml`, lấy đúng nét từ bản thiết kế
- [x] Schema Room **v2** qua `MIGRATION_1_2`: thêm `sync_queue` và `review_logs` (xem ghi chú dưới); có `MigrationTest` chạy trên emulator
- [x] `StringProvider` cho ViewModel / `GlobalExceptionHandler`; chuỗi dùng chung và chuỗi lỗi đã vào `strings.xml` (vi / en). Chuỗi của từng màn sẽ chuyển dần khi dựng lại màn đó
- [x] Splash vào thẳng Home; gỡ màn Login email khỏi `AppNavHost` (file cũ còn giữ tới bước 6)
- [x] Gắn plugin `google-services` (chỉ apply khi máy có file), Firebase BoM, App Check: bản debug dùng debug provider, bản release dùng Play Integrity; README có hướng dẫn cho máy mới

> **Thêm ngoài kế hoạch — bảng `review_logs`:** `flashcards` chỉ giữ trạng thái SM-2 mới nhất, không
> đủ để dựng màn Thống kê (số thẻ ôn mỗi ngày, tỉ lệ nhớ, chuỗi ngày ôn). Bảng lịch sử này được
> thêm ngay từ bây giờ để bước 3 ghi dữ liệu và bước 7 có số liệu thật. Chỉ lưu local, không đồng bộ.

**Thi**
- [ ] Review, merge
- [ ] Chép debug token của App Check (Logcat, tag `DebugAppCheckProvider`) vào Firebase Console → App Check → Manage debug tokens
- [x] Cho phép Claude tải font Nunito Sans

### Bước 2 — Bộ thẻ

Home + bảng tạo bộ thẻ xong ngày 7/10/2026, **đã nằm trong `dev`** (commit `d81249c`, vào thẳng `dev`
không qua pull request do nhánh `feat/deck-ui` bị gắn nhầm với `origin/dev` — xem Mục 6.2).
Chi tiết bộ thẻ xong ngày 8/10/2026, đã vào `dev` qua pull request #7 (nhánh `feat/deck-detail`).
Thêm / sửa thẻ thủ công xong ngày 8/10/2026, đã vào `dev` qua pull request #8 (`feat/manual-card`).
Splash xong ngày 8/10/2026, đã vào `dev` qua pull request #9 (`feat/splash`). Phần việc của Claude
ở bước 2 đến đây là hết.

**Claude**
- [x] Home: thẻ "Cần ôn hôm nay", hai lối tạo thẻ (chụp ghi chú / gõ tay), danh sách bộ thẻ; đủ trạng thái rỗng / đang tải (skeleton) / lỗi
- [x] Chi tiết bộ thẻ: thống kê, lọc Tất cả / Thủ công / AI, vuốt để sửa / xoá
- [x] Thêm / sửa thẻ thủ công (có "lưu xong thêm thẻ tiếp")
- [x] Tạo bộ thẻ: bottom sheet tên + màu nhận diện
- [x] Splash theo design (màn chào giữ tối thiểu 0,8 giây rồi vào Home; màu cửa sổ lúc khởi động trùng nền Splash)
- [x] Đủ 3 trạng thái Empty / Loading / Error cho mọi màn danh sách (Home, Chi tiết bộ thẻ)

> **Chi tiết bộ thẻ — những chỗ design không vẽ:** nút ⋮ mở menu chỉ có "Xoá bộ thẻ" (xoá luôn
> các thẻ bên trong, trong một transaction); xoá thẻ và xoá bộ thẻ đều hỏi lại; **chạm vào thẻ =
> mở thẳng màn sửa thẻ** (Thi chốt 8/10/2026 — không có màn / bảng "chi tiết thẻ" riêng), chỉ
> vuốt mới lộ Sửa / Xoá; đổi chip lọc thì danh sách về đầu và không chạy hoạt ảnh; "Thẻ mới" =
> thẻ chưa ôn lần nào; nhãn "Có ảnh nguồn" chỉ hiện khi thẻ AI có `noteId`.
>
> **Thêm / sửa thẻ — những chỗ design không vẽ:** một màn cho hai chế độ, route có `cardId` là
> SỬA. Khi sửa: tiêu đề "Sửa thẻ", nút "Lưu thay đổi", ô Bộ thẻ chỉ để xem (chưa cho chuyển thẻ
> sang bộ khác), ẩn ô tick và dòng nhắc, badge theo nguồn thẻ (AI / Thủ công). Khi thêm với ô
> tick bật: lưu xong ở lại màn, xoá trắng hai ô, con trỏ về ô Câu hỏi, dòng nhắc đổi thành "Đã
> lưu N thẻ" (không dùng snackbar vì nó nằm sau bàn phím / đè nút Lưu). Nút Lưu chỉ bật khi cả
> hai ô có chữ. Thi chốt 8/10/2026: top bar dùng mũi tên ← thay nút ✕ của design; rời màn (nút
> ← hoặc Back hệ thống) khi còn nội dung chưa lưu thì hỏi "Bỏ nội dung chưa lưu?".

> **Quy ước "cần ôn hôm nay":** thẻ được tính là đến hạn nếu `dueDate` rơi vào bất kỳ lúc nào trong
> hôm nay (tới 23:59), không phải chỉ khi đã qua đúng giờ hẹn — xem `core/domain/util/DueCutoff.kt`.
> Home, Ôn tập và Widget phải dùng chung mốc này để con số khớp nhau.
>
> **Dữ liệu mẫu để chụp màn hình / demo:** `androidTest/.../tools/DemoDataSeeder.kt` nạp 4 bộ thẻ
> giống bản design vào bản debug (chỉ chạy khi gọi tường minh, không ghi đè dữ liệu đang có).

**Thi**
- [ ] Chạy thử trên máy, so với design, báo chỗ lệch

### Bước 3 — Ôn tập

Xong ngày 8/10/2026, đã vào `dev` qua pull request #10 (nhánh `feat/review`).

**Claude**
- [x] Màn câu hỏi, màn đáp án + 4 mức (Quên / Khó / Dễ / Rất dễ), màn hoàn thành phiên
- [x] Lấy thẻ đến hạn theo toàn bộ hoặc theo từng bộ thẻ
- [x] Unit test cho luồng ôn

> **Cách phiên ôn hoạt động (những chỗ design không nói):**
> - Cả phiên là MỘT route (`ReviewRoute`), một ViewModel, đi qua các chặng trong `ReviewPhase`.
>   "Ôn ngay" ở Home mở `ReviewRoute()`; "Ôn N thẻ" ở Chi tiết bộ thẻ mở `ReviewRoute(deckId)`.
> - Hàng thẻ được chụp MỘT LẦN lúc mở phiên (thẻ hẹn sớm nhất trước), không nghe database.
> - Mỗi lần chấm được lưu ngay (SM-2 + một dòng `review_logs`), nên thoát giữa phiên không mất
>   gì và không cần hỏi lại.
> - "Ôn lại N thẻ đã quên" mở lượt mới gồm các thẻ vừa chấm Quên; chấm ở lượt này VẪN được ghi
>   nhận như bình thường (tính lại SM-2, thêm dòng lịch sử).
> - "Lịch ôn tiếp theo" gom 3 nhóm: ngày mai / 2–13 ngày (ghi "Sau N ngày" nếu cả nhóm cùng số
>   ngày, không thì "Trong 2 tuần tới") / từ 2 tuần trở lên. Nhóm trống thì ẩn.
> - "Về trang chủ" xoá hết chồng màn rồi mở Home (`replaceAll` đã sửa để làm đúng việc này).
> - Nút bút chì ở mặt câu hỏi mở màn sửa thẻ; quay lại thì nội dung thẻ đang ôn được nạp lại.
> - CHƯA làm (thuộc bước 4): dòng "Trích từ ghi chú chụp…", nút "Xem cả ảnh" và đoạn trích ghi
>   chú ở mặt đáp án — hiện chỉ có badge nguồn AI / Thủ công. Lật thẻ chưa có hoạt ảnh.

**Thi**
- [ ] Chạy thử một phiên ôn đầy đủ

### Bước 4 — Chụp ảnh + AI *(cần bước 0)*

Màn Chụp ghi chú (bước 1/3 của luồng) code xong ngày 8/10/2026 trên nhánh `feat/capture` (chưa
commit) — chờ Thi xem và merge.

**Claude**
- [x] CameraX + crop vùng chữ
- [ ] OCR bằng ML Kit, màn sửa văn bản
- [ ] Gọi Gemini qua Firebase AI Logic, màn "đang tạo thẻ"
- [ ] Màn duyệt thẻ AI đề xuất (chọn / sửa / xoá / thêm, nút báo cáo nội dung AI)
- [ ] Giới hạn lượt AI mỗi ngày; màn lỗi mạng và màn hết lượt
- [ ] Xem ảnh nguồn của thẻ AI (tô sáng đúng vùng `sourceBox`)

> **Màn Chụp ghi chú — cách hoạt động (Thi chốt 8/10/2026) và những chỗ design không vẽ:**
> - Một route (`CaptureRoute`), hai chặng: **ngắm** (camera chạy, khung cắt hiện sẵn, kéo được) →
>   bấm chụp → **chỉnh** (ảnh đứng yên, kéo 4 góc, bấm ✓ mới sang bước 2). Ảnh chọn từ Thư viện
>   vào thẳng chặng chỉnh, khung mở đầu ôm gần hết ảnh.
> - Chặng chỉnh: hàng nút dưới đổi thành "Chụp lại" · ✓ · (trống). Back của hệ thống = chụp lại;
>   nút ✕ (giữ như design) = thoát cả luồng.
> - "Đèn tắt / Đèn bật" là đèn pin sáng liên tục; máy không có đèn thì nút bị mờ.
> - Quyền camera: vào màn là hỏi ngay. Bị từ chối thì khung ngắm hiện lời nhắc + nút "Cho phép
>   camera" (bị từ chối hẳn thì nút thành "Mở Cài đặt"); Thư viện vẫn dùng được, không cần quyền.
> - Chỉ kéo được 4 góc (không kéo cả khung, không xoay). Dòng nhắc "Kéo 4 góc…" tự ẩn khi khung
>   kéo xuống chạm tới nó.
> - Màn luôn tối kể cả khi app ở giao diện sáng; hai dải sau thanh trạng thái / thanh điều hướng
>   cũng tối theo khi màn này đang hiện (`AppNavHost`).
> - Ảnh của luồng chỉ nằm trong `cacheDir/capture/` (xoá khi chụp ảnh mới); ảnh đã cắt có cạnh
>   dài tối đa 2560 px. Việc chuyển ảnh sang chỗ lưu lâu dài làm ở bước lưu thẻ AI.
> - Mở từ Chi tiết bộ thẻ thì `deckId` đi theo route để bước 2 chọn sẵn bộ đó.
> - TẠM: bấm ✓ đang mở `OcrReviewPlaceholderScreen` (chỉ hiện ảnh đã cắt) — sẽ được thay bằng
>   màn Kiểm tra văn bản thật ở phần việc kế tiếp.

**Thi**
- [ ] Thử trên máy thật với vở viết tay và chữ in có dấu
- [ ] Kiểm tra App Check hoạt động

### Bước 5 — Cài đặt, nhắc ôn, widget

**Claude**
- [ ] Cài đặt: giao diện (theo máy / sáng / tối), nhắc ôn + giờ nhắc, lượt AI hôm nay, xoá dữ liệu trên máy, giới thiệu
- [ ] Thông báo nhắc ôn hằng ngày (WorkManager)
- [ ] Widget (Glance): tổng quan 4×2, số thẻ 2×2, ôn nhanh 4×2

**Thi**
- [ ] Thử thông báo và widget trên launcher thật

### Bước 6 — Tài khoản và đồng bộ *(cần bước 0, phần Auth + Firestore)*

**Claude**
- [ ] Đăng nhập Google tuỳ chọn, vào từ Cài đặt
- [ ] Đồng bộ hai chiều Local ⇄ Firestore (push qua `sync_queue`, pull theo `updatedAt`, Last-Write-Wins)
- [ ] Gán `userId` cho dữ liệu cũ khi đăng nhập lần đầu
- [ ] Cài đặt khi đã đăng nhập: trạng thái đồng bộ, đăng xuất
- [ ] Xoá tài khoản (xác nhận bằng cách nhập "XÓA", đăng nhập lại nếu cần)

**Thi**
- [ ] Đặt Firestore Security Rules ([`docs_tld.md`](docs_tld.md) Mục 7.2)
- [ ] Thử đồng bộ trên hai máy

### Bước 7 — Thống kê, onboarding, ngôn ngữ

**Claude**
- [ ] Màn thống kê: chuỗi ngày ôn, số thẻ ôn mỗi ngày, tỉ lệ nhớ
- [ ] Ba màn onboarding
- [ ] Chọn ngôn ngữ, bản dịch tiếng Anh

**Thi**
- [ ] Soát bản dịch, chạy thử

---

## 3. Cách phối hợp

- Claude chỉ bắt đầu một bước khi Thi nói rõ "bắt đầu".
- Mỗi bước làm trên một nhánh riêng. Claude biên dịch và chạy unit test trước khi báo.
- Thi chạy thử trên máy, báo lỗi hoặc chỗ lệch design; Claude sửa; Thi merge.
- Commit và push chỉ làm khi Thi yêu cầu.

---

## 4. Các quyết định đã chốt

| Vấn đề | Quyết định |
| --- | --- |
| Chia sprint | Không chia; làm gộp theo 7 bước ở trên |
| Build / phát hành | Gác lại, Thi làm sau khi phát triển xong |
| Đăng nhập | Chỉ Google, tuỳ chọn, vào từ Cài đặt; không chặn lúc mở app. Bỏ màn Login email / mật khẩu |
| Thử lại khi AI lỗi mạng | Không trừ lượt |
| Màn hết lượt AI | Bỏ nút "lưu ghi chú, tạo thẻ sau"; chỉ giữ "Tự gõ thẻ từ văn bản này" |
| 4 nút đánh giá khi ôn | Hiện mô tả ("Không nhớ", "Chật vật", "Nhớ được", "Nhớ ngay"), không hiện số ngày ôn lại |
| Điều hướng | Không có bottom nav; Cài đặt vào từ nút trên Home |
| `google-services.json` | Không commit, đưa vào `.gitignore` |

---

## 5. Việc còn treo

- Font Nunito Sans: cần file `.ttf` trong `res/font/` (xem bước 1).
- Nút "Tiếp tục với Google": design đang dùng chữ "G" tạm, khi code thay bằng nút chuẩn của Google.
- Con số hạn mức miễn phí của Gemini thay đổi theo thời gian — xem lại trang giá của Firebase AI Logic khi tạo project.
- Khi quay lại phần phát hành: bản có đăng nhập bắt buộc phải có chức năng xoá tài khoản, trang web hướng dẫn xoá tài khoản và Data Safety khai dữ liệu tài khoản.

- Home, thẻ "Cần ôn hôm nay": chú giải dùng tên thật của bộ thẻ nên tên dài bị cắt bằng "…" (design dùng tên ngắn). Chờ Thi quyết: giữ một dòng hay cho xuống hai dòng.
- Icon ứng dụng vẫn là robot Android mặc định. Trên Android 12+ hệ thống tự hiện icon này vài giây lúc khởi động nguội, TRƯỚC màn Splash của app, nên người dùng thấy robot xanh rồi mới tới logo "TLD". Cần bộ icon launcher riêng (và có thể đặt nó làm icon màn chào hệ thống) — chờ Thi quyết làm lúc nào.
- Chi tiết bộ thẻ: menu ⋮ có thêm "Đổi tên / đổi màu bộ thẻ" không. Sửa thẻ: có cho chuyển thẻ sang bộ khác không (hiện ô Bộ thẻ bị khoá). Dải thanh trạng thái / dải dưới thanh đáy có cho trùng màu top bar / thanh đáy không.

### Ký phát hành — CHƯA LÀM (tại ngày chốt)

Bước 0 chỉ đăng ký SHA của **debug keystore** (khoá Android Studio tự tạo để chạy bản debug).
Chưa có keystore phát hành, chưa cấu hình `signingConfigs` trong `app/build.gradle.kts`.
Trước khi build bản release phải làm đủ các việc sau, nếu không đăng nhập Google và App Check
sẽ lỗi trên bản release dù bản debug chạy bình thường:

- [ ] Tạo keystore phát hành; cất file và mật khẩu ở nơi an toàn, không commit vào repo
- [ ] Cấu hình `signingConfigs` cho build type `release` (mật khẩu đọc từ file ngoài repo hoặc biến môi trường)
- [ ] Lấy SHA-1 và SHA-256 của keystore phát hành, thêm vào Firebase: Project settings → Your apps → Add fingerprint
- [ ] Nếu dùng Play App Signing: thêm cả SHA của khoá do Google ký (Play Console → App integrity)
- [ ] Tải lại `google-services.json` sau khi thêm fingerprint
- [ ] App Check: bật Play Integrity API và liên kết với Play Console, rồi mới bật Enforce cho bản release

---

## 6. Sổ tay làm việc (đọc khi mở phiên chat mới với Claude)

Mục này ghi những thứ KHÔNG suy ra được từ code: cách hai bên phối hợp, bản design nằm ở đâu,
cách kiểm tra giao diện. Tiến độ xem checkbox ở Mục 2.

### 6.1 Việc tiếp theo

Bước 2 và bước 3 đã xong và nằm trong `dev` (pull request #7–#10).

Đang làm **bước 4 — Chụp ảnh + AI**, mỗi màn một nhánh. Màn đầu (Chụp ghi chú, nhánh
`feat/capture`) đã code xong, chờ Thi xem và merge. Thứ tự còn lại: OCR (ML Kit) + màn Kiểm tra
văn bản → gọi Gemini + màn "đang tạo thẻ" → màn duyệt thẻ AI → giới hạn lượt, màn lỗi mạng / hết
lượt → xem ảnh nguồn (kèm phần còn thiếu ở mặt đáp án của màn ôn). Artboard: xem bảng ở Mục 6.3.

Chụp và OCR chạy hoàn toàn trên máy. Từ màn gọi Gemini trở đi mới cần Firebase thật: lúc đó Thi
phải xác nhận debug token App Check của máy thử đã có trong Firebase Console (App Check → nút ⋮
của app → Manage debug tokens). Thi tự thử trên máy thật ở cuối bước; Claude kiểm tra trên
emulator (camera của emulator chỉ là cảnh ảo, không dùng để đánh giá chất lượng OCR được).

### 6.2 Cách phối hợp

- **Chỉ bắt đầu code khi Thi nói rõ** ("bắt đầu", "làm đi", "làm tiếp"). Thi hay bàn kế hoạch
  bằng câu nghe như lệnh; khi mơ hồ thì hỏi lại một câu ngắn.
- **Thi tự commit, push và tạo pull request** (Android Studio). Luồng nhánh: `feat/...` → pull
  request vào `dev` → pull request `dev` vào `main`. Claude không commit / push trừ khi được yêu cầu.
- **Tạo nhánh mới:** `git switch dev` → `git pull` → `git switch -c feat/<tên>`. KHÔNG tách bằng
  `git switch -c <tên> origin/dev` — cách đó gắn nhánh với `origin/dev`, khiến "Commit and Push"
  đẩy thẳng vào `dev` (đã xảy ra với `feat/deck-ui` ngày 8/10/2026).
- **Làm theo từng màn, có điểm dừng:** xong một màn thì biên dịch, chạy unit test, cài lên
  emulator, chụp ảnh gửi Thi xem rồi mới làm màn kế.
- **Ghi chú trong code:** tiếng Việt, cho người mới học Kotlin / Compose, mức vừa phải — đầu
  file / hàm nói nó làm gì; chỉ chú thích tại chỗ khó đoán; mỗi khái niệm giải thích một lần.
- **Trả lời Thi bằng tiếng Việt**, ngắn gọn, nói rõ việc nào của ai.

### 6.3 Bản thiết kế

Canvas: https://claude.ai/artifact/Xrgp4GyHTjyrgRePczrBPx — 56 màn, hai trang **Light** và
**Dark theme**. Mỗi màn là một file nguồn `project/<Tên>.dc.html` (HTML + style nội tuyến), trong
đó có chính xác kích thước, khoảng cách, màu, cỡ chữ — Claude đọc file này bằng công cụ Artifact
(`read` với `path`) rồi chuyển từng con số sang Compose. Bản dark là `<Tên>Dark.dc.html`.

| Bước | Artboard (tên file bỏ đuôi `.dc.html`) |
| --- | --- |
| 2. Bộ thẻ | `Main` (Home), `HomeEmpty`, `HomeLoading`, `HomeError`, `CreateDeck`, `DeckDetail`, `DeckEmpty`, `ManualCard`, `Splash` |
| 3. Ôn tập | `ReviewQuestion`, `ReviewAnswer`, `ReviewDone` |
| 4. Chụp ảnh + AI | `Capture`, `OcrReview`, `AiGenerating`, `AiSuggestions`, `AiError`, `AiQuota` |
| 5. Cài đặt, widget | `Settings`, `Widget` |
| 6. Tài khoản | `LoginGoogle`, `SettingsAccount`, `DeleteAccount` |
| 7. Thống kê, onboarding, ngôn ngữ | `Stats`, `Onboarding1`–`Onboarding3`, `Language` |

Khác biệt có chủ ý so với design: dữ liệu thật (tên dài bị cắt, danh sách cuộn), thanh trạng
thái / bàn phím của hệ thống, và các nút thuộc bước chưa làm thì báo "sắp có".

### 6.4 Kiểm tra giao diện trên emulator

- Biên dịch + test: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`; đóng gói:
  `./gradlew :app:assembleDebug`. Test có emulator: `./gradlew :app:connectedDebugAndroidTest`
  (lệnh này GỠ app khỏi emulator khi xong — phải cài lại và đăng ký lại debug token App Check).
- adb nằm ở `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`; emulator hay dùng là Pixel 7a.
- Cài và chụp: `adb install -r app/build/outputs/apk/debug/app-debug.apk`, mở app, rồi
  `adb exec-out screencap -p > anh.png`.
- Dữ liệu mẫu giống design: chạy `DemoDataSeeder` (lệnh ghi ở đầu file
  `app/src/androidTest/.../tools/DemoDataSeeder.kt`). Emulator của Thi đang có sẵn 4 bộ thẻ mẫu.
- App trên emulator đang được đặt ngôn ngữ riêng là tiếng Việt
  (`adb shell cmd locale set-app-locales com.ledinhthi.ontaptld --locales vi-VN`).
- Xem dark theme: `adb shell cmd uimode night yes`, chụp xong trả lại `night no`.
- **Đừng xoá dữ liệu app** (`pm clear`, gỡ cài đặt): sẽ mất debug token App Check mà Thi đã đăng
  ký trên Firebase Console, phải lấy token mới trong Logcat và đăng ký lại.
