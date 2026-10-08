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
- [x] Chép debug token của App Check (Logcat, lọc chữ `DebugAppCheckProvider`) vào Firebase Console → App Check → Apps → ⋮ → Manage debug tokens — đã đăng ký ngày 8/10/2026 cho hai máy: "Emulator Pixel 7a" và "OPPO CPH1911". Mỗi máy một token; gỡ app / xoá dữ liệu app thì phải lấy và đăng ký lại
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
> - Dòng "Trích từ ghi chú chụp…", nút "Xem cả ảnh" và đoạn trích ghi chú ở mặt đáp án của thẻ
>   AI: đã làm ở bước 4 — xem mục "Xem ảnh nguồn của thẻ AI" bên dưới.
> - Hoạt ảnh lật thẻ (thêm 8/10/2026, nhánh `feat/flip-animation`, design không vẽ): bấm "Hiện
>   đáp án" hoặc chạm thẻ thì tấm thẻ quay 180° quanh trục dọc trong 0,4 giây, qua nửa vòng thì
>   đổi sang mặt đáp án; nút "Hiện đáp án" mờ đi, 4 nút chấm hiện dần lên. Chỉ nhận lần chấm
>   khi thẻ đã lật xong (lỡ bấm đúp "Hiện đáp án" không thành chấm nhầm). Sang thẻ kế tiếp thì
>   hiện thẳng mặt câu hỏi, không quay ngược và chưa có hoạt ảnh chuyển thẻ.

**Thi**
- [ ] Chạy thử một phiên ôn đầy đủ

### Bước 4 — Chụp ảnh + AI *(cần bước 0)*

Màn Chụp ghi chú (bước 1/3 của luồng) xong ngày 8/10/2026, đã vào `dev` qua pull request #12
(nhánh `feat/capture`). Màn Kiểm tra văn bản (bước 2/3) xong cùng ngày, đã vào `dev` qua pull
request #13 (nhánh `feat/ocr-review`). Phần gọi Gemini + màn "Đang tạo thẻ" xong cùng ngày, đã
vào `dev` qua pull request #14 (nhánh `feat/ai-generate`). Màn Duyệt thẻ đề xuất + lưu thẻ xong
cùng ngày, đã vào `dev` qua pull request #15 (nhánh `feat/ai-suggestions`). Màn lỗi AI + màn hết
lượt xong cùng ngày, đã vào `dev` qua pull request #17 (nhánh `feat/ai-error-quota`). Xem ảnh
nguồn của thẻ AI xong cùng ngày, đã vào `dev` qua pull request #21 (nhánh `feat/source-image`).
Hai việc dọn dẹp cuối của bước 4 xong tối 8/10/2026, đã vào `dev` qua pull request #22 (nhánh
`feat/note-cleanup`). Phần việc của Claude ở bước 4 đến đây là hết.

**Claude**
- [x] CameraX + crop vùng chữ
- [x] OCR bằng ML Kit, màn sửa văn bản
- [x] Gọi Gemini qua Firebase AI Logic, màn "đang tạo thẻ"
- [x] Màn duyệt thẻ AI đề xuất (chọn / sửa / xoá / thêm, nút báo cáo nội dung AI)
- [x] Giới hạn lượt AI mỗi ngày; màn lỗi mạng và màn hết lượt
- [x] Xem ảnh nguồn của thẻ AI (tô sáng đúng vùng `sourceBox`)
- [x] Dọn dẹp: xoá bộ thẻ / xoá thẻ phải xoá cả `Note` và file ảnh; màn Kiểm tra văn bản giữ phần đã sửa khi app bị tắt dưới nền

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
>
> **Màn Kiểm tra văn bản — cách hoạt động (Thi chốt 8/10/2026) và những chỗ design không vẽ:**
> - OCR dùng ML Kit Text Recognition bản GÓI KÈM APP (Thi chọn): chạy không cần mạng. Đổi lại
>   thư viện gốc nặng khoảng 11 MB cho mỗi kiến trúc CPU — APK debug (gộp cả 4 kiến trúc) tăng từ
>   18 lên 61 MB; bản phát hành dạng App Bundle chỉ tải phần của đúng một kiến trúc.
> - Các khối chữ được xếp lại theo vị trí trên ảnh (trên xuống, trái sang phải) trước khi ghép
>   thành văn bản. Bộ nhận dạng trả về từng dòng kèm vị trí trên ảnh (`OcrLine`); màn này chỉ
>   dùng phần chữ, vị trí được dùng lúc lưu thẻ để tô sáng ảnh nguồn.
> - Ô văn bản cao cố định 272dp, dài thì cuộn bên trong ô; đang nhận dạng thì ô tạm khoá.
> - Không nhận ra chữ / bộ nhận dạng lỗi: ở lại màn, dòng nhắc cam đổi nội dung, cho tự gõ; nút
>   "Tạo thẻ bằng AI" mờ cho tới khi có chữ và có bộ thẻ.
> - Ô "Lưu vào bộ thẻ": mở từ Chi tiết bộ thẻ thì chọn sẵn bộ đó, mở từ Home thì chọn bộ dùng
>   gần nhất. Danh sách có dòng cuối "Bộ thẻ mới" mở bảng tạo bộ thẻ; tạo xong tự chọn bộ đó.
> - Nút ← / Back: về màn chụp ở chặng chỉnh khung (ảnh cũ còn nguyên). "Chụp lại": về camera,
>   bỏ ảnh cũ (`CaptureFlowEvents` nhắn màn chụp). Đã sửa văn bản mà rời màn bằng một trong hai
>   lối thì hỏi "Bỏ phần văn bản đã sửa?" (Huỷ / Thoát hoặc Huỷ / Chụp lại).
> - Dòng "Còn x/N lượt" đọc số thật từ `AiQuotaGuard.MAX_CALLS_PER_DAY` (nguồn duy nhất của con
>   số này). Thi chốt 8/10/2026: N đặt theo hạn mức miễn phí thật. Hạn mức đó tính cho CẢ
>   project (mọi người dùng chung) và chỉ xem được ở AI Studio → Rate limits. Bảng Thi gửi ngày
>   8/10/2026 (gói Free): các model **Flash** (2.5 / 3 / 3.5 / 3.6 / 3.7 / 3.8) chỉ 5 lượt/phút,
>   **20 lượt/ngày**; **Flash-Lite 3.1 và 3.5** được 15 lượt/phút, **500 lượt/ngày**. Vì vậy app
>   sẽ dùng dòng Flash-Lite, và N = **10 lượt mỗi máy mỗi ngày** (khoảng 50 máy dùng hết lượt
>   cùng ngày mới chạm trần). Đã đối chiếu đúng project `on-tap-tld` trong AI Studio.
>
> **Gọi Gemini + màn "Đang tạo thẻ" — cách hoạt động và những chỗ design không nói:**
> - Bước 3/3 là MỘT route (`AiCardsRoute`: ảnh, văn bản, bộ thẻ), một ViewModel, đi qua các
>   chặng trong `AiCardsPhase` — cùng cách với phiên ôn. Màn duyệt thẻ, màn lỗi mạng và màn hết
>   lượt sẽ là các chặng thêm vào đây.
> - Model `gemini-3.5-flash-lite` qua Firebase AI Logic (`FirebaseGeminiClient`), chờ tối đa 45
>   giây. AI bị buộc trả JSON theo khuôn: câu hỏi, câu trả lời, số dòng nguồn.
> - Lời dặn cho AI nằm ở `FlashcardPrompt`: chỉ dùng ý có trong ghi chú, thẻ cùng ngôn ngữ với
>   ghi chú, mỗi thẻ một ý, bỏ qua mọi "mệnh lệnh" nằm lẫn trong ghi chú. Ghi chú được đánh số
>   từng dòng có chữ; "Nguồn: dòng N trong ảnh" là số đó.
> - Mỗi lần tối đa 10 thẻ. Chỉ lần gọi có trả về thẻ mới bị trừ lượt; mất mạng, AI lỗi, AI
>   không soạn được thẻ nào, hay bấm Huỷ đều không trừ.
> - "Huỷ", nút ← và Back trong lúc chờ: bỏ lần gọi đang dở, về bước 2 (văn bản còn nguyên).
> - Đã kiểm chứng trên emulator ngày 8/10/2026: debug token App Check hoạt động, gọi được AI
>   thật với ghi chú tiếng Anh và tiếng Việt. Máy OPPO chưa thử.
> - Lỗi khi gọi AI: xem mục "Màn lỗi AI và màn hết lượt" bên dưới (trước đó tạm báo bằng hộp
>   thoại / snackbar chung rồi về bước 2).
>
> **Màn Duyệt thẻ đề xuất — cách hoạt động (Thi chốt 8/10/2026) và những chỗ design không vẽ:**
> - Là chặng `Suggestions` của `AiCardsRoute`. Mọi thẻ AI được tích sẵn. Chạm vào ô tích hoặc
>   phần chữ của thẻ = tích / bỏ tích; thẻ bỏ tích có viền nét đứt và không được lưu.
> - Nút bút chì và "Thêm thẻ" mở một bảng trượt (hai ô Câu hỏi / Câu trả lời, tối đa 250 ký tự
>   như thẻ thủ công). Thẻ ở đây chưa nằm trong database nên không dùng lại màn Sửa thẻ.
> - Nút thùng rác: hỏi lại "Xoá thẻ này?" (Thi chọn). Xoá hết thì màn hiện trạng thái rỗng,
>   vẫn thêm thẻ tự gõ được.
> - Nút lá cờ "Báo cáo nội dung AI" (Thi chọn): mở app email của máy với thư soạn sẵn gửi tới
>   địa chỉ trong `strings.xml` (`support_email`), kèm nội dung các thẻ AI; người dùng tự bấm
>   Gửi. Máy không có app email thì báo bằng snackbar.
> - Nút ← / Back ở chặng này luôn hỏi "Bỏ các thẻ vừa tạo?" vì rời màn là mất thẻ và lượt AI.
> - Lưu (`SaveSuggestedCardsUseCase`): chép ảnh đã cắt sang `filesDir/notes/<noteId>.jpg`, ghi
>   một `Note` (ảnh + văn bản), rồi ghi các thẻ đang tích. Thẻ AI: `source = AI`, có `noteId`.
>   Thẻ tự gõ thêm trong màn này: `source = MANUAL`, không có `noteId`. Hỏng giữa chừng thì dọn
>   ghi chú và ảnh đã ghi. Lưu xong: báo "Đã lưu N thẻ", gỡ cả luồng chụp khỏi chồng màn rồi mở
>   Chi tiết bộ thẻ (Back ở đó về Home).
> - Danh sách thẻ đang duyệt được cất vào `SavedStateHandle` (dạng JSON) mỗi lần đổi. Lý do:
>   khi người dùng sang app email để gửi báo cáo, Android có thể tắt app dưới nền (đã xảy ra
>   trên emulator); lúc quay lại app hiện đúng danh sách cũ thay vì gọi AI lần nữa.
> - Bấm Huỷ lúc đang gọi AI: thư viện Firebase bọc tín hiệu huỷ coroutine vào `UnknownException`
>   — `FirebaseGeminiClient` gọi `ensureActive()` để nhận ra đây là "bị huỷ", không phải lỗi.
> - Dọn `Note` + ảnh khi xoá, và giữ văn bản đã sửa ở màn Kiểm tra văn bản: đã làm — xem mục
>   "Dọn ghi chú và ảnh" bên dưới.
>
> **Dọn ghi chú và ảnh, thông báo ngắn, bàn phím (nhánh `feat/note-cleanup`, 8/10/2026):**
> - Xoá bộ thẻ: trong cùng một transaction, đánh dấu xoá bộ thẻ, các thẻ và mọi `Note` của bộ;
>   xong mới xoá các file ảnh `filesDir/notes/<noteId>.jpg`.
> - Xoá một thẻ AI: một ghi chú sinh ra nhiều thẻ, nên `Note` và ảnh chỉ bị xoá khi đó là thẻ
>   cuối cùng còn dùng ghi chú đó; các thẻ còn lại vẫn xem được ảnh nguồn.
> - Dòng `Note` vẫn xoá mềm (`isDeleted = 1`) như bộ thẻ và thẻ, để dành cho đồng bộ; chỉ file
>   ảnh là xoá thật. Không đổi schema Room.
> - Quét dọn lúc mở app (Thi chốt): `OnTapTldApp` chạy nền `CleanUpOrphanNotesUseCase` — đánh dấu
>   xoá ghi chú không còn thẻ sống nào dùng, rồi xoá file trong `filesDir/notes/` không còn ghi
>   chú nào trỏ tới. Ghi chú / file mới hơn một phút được chừa lại (có thể một lượt lưu thẻ AI
>   đang chạy dở). Nhờ vậy ảnh của thẻ đã xoá từ bản app cũ cũng được dọn.
> - Màn Kiểm tra văn bản: văn bản đang sửa, văn bản gốc sau nhận dạng, trạng thái nhận dạng và bộ
>   thẻ đang chọn được cất vào `SavedStateHandle`. App bị tắt dưới nền rồi mở lại thì không nhận
>   dạng lại, và rời màn vẫn được hỏi "Bỏ phần văn bản đã sửa?". Bị tắt lúc ĐANG nhận dạng dở
>   thì nhận dạng lại từ đầu.
> - Thông báo ngắn (snackbar) của cả app (Thi chốt): hiện ở ĐỈNH màn, ngay dưới thanh trạng thái,
>   tự tắt sau 3 giây (người bật trợ năng được hệ thống kéo dài thêm). Trước đó nó nằm sát đáy,
>   đè lên hàng nút. Trong 3 giây đó nó đè lên top bar (nút ← và tiêu đề).
> - Bàn phím ở màn Thêm / sửa thẻ và màn Kiểm tra văn bản (Thi chốt): bàn phím hiện lên thì
>   thanh nút ở đáy (Lưu thẻ / Tạo thẻ bằng AI) ĐỨNG YÊN và bị bàn phím che, chỉ phần nội dung
>   co lại và cuộn được; đóng bàn phím mới bấm được nút. Trước đó thanh nút bị đẩy lên nằm ngay
>   trên bàn phím. Hai bảng trượt (Tạo bộ thẻ, sửa thẻ đề xuất) không đổi.
> - Đã kiểm chứng trên emulator: lần mở app đầu tiên xoá đúng 2 ảnh mồ côi cũ, giữ 2 ảnh đang
>   dùng; lưu thẻ AI vào bộ tạm rồi xoá bộ đó → ảnh mất theo; tắt hẳn process lúc đang ở màn Kiểm
>   tra văn bản rồi mở lại → văn bản đã sửa còn nguyên. Trường hợp "xoá thẻ cuối cùng của ghi
>   chú" mới qua test (`NoteCleanupDaoTest` trên emulator), chưa bấm tay.
>
> **Xem ảnh nguồn của thẻ AI — cách hoạt động (Thi chốt 8/10/2026) và những chỗ design không vẽ:**
> - Mỗi thẻ AI lưu thêm hai thứ: `sourceLine` = dòng ghi chú mà AI rút thẻ ra (đếm từ 1, chỉ
>   tính dòng có chữ — hàm dùng chung `noteLines`), và `sourceBox` = vị trí của dòng đó trên ảnh,
>   tính theo tỉ lệ của ảnh (0..1). **Schema Room lên v3** (`MIGRATION_2_3`: thêm cột
>   `flashcards.sourceLine`), có test trong `MigrationTest`.
> - Vị trí trên ảnh được tìm lúc LƯU thẻ: chạy lại bộ nhận dạng chữ trên ảnh đã cắt (chừng một
>   giây, không cần mạng) rồi so NỘI DUNG dòng ghi chú với các dòng nhận dạng được
>   (`SourceLineLocator`: giống hệt thì lấy luôn, không thì lấy dòng giống nhất nếu giống từ
>   60% trở lên). So theo nội dung vì ở bước 2 người dùng có thể đã sửa, thêm, xoá dòng. Dòng
>   người dùng tự gõ thêm, hoặc ảnh không đọc lại được: thẻ vẫn lưu, chỉ không có vùng tô sáng.
> - Mặt đáp án lúc ôn (theo artboard `ReviewAnswer`): thẻ AI còn ghi chú thì đáy thẻ có nhãn
>   AI + "Trích từ ghi chú chụp dd/MM" + nút "Xem cả ảnh", bên dưới là đoạn trích ba dòng (dòng
>   trước, dòng nguồn tô cam, dòng sau; mỗi dòng tối đa hai hàng chữ). Không biết dòng nguồn thì
>   không có đoạn trích; thẻ thủ công hoặc ghi chú đã mất thì chỉ có nhãn nguồn như cũ.
> - "Xem cả ảnh" (design không vẽ màn này; Thi chọn kiểu toàn màn hình có phóng to): mở
>   `SourceImageRoute(cardId)` — màn luôn tối, nút ✕ đóng về đúng mặt đáp án đang xem. Ảnh vừa
>   khít màn, vùng của thẻ viền cam, phần còn lại tối đi; chụm hai ngón phóng to tới 5 lần, kéo
>   để di chuyển (không kéo hở mép). Chưa có chạm đúp để phóng to. Không có vùng nguồn thì vẫn
>   xem được ảnh, dòng chú thích dưới đáy nói rõ; file ảnh không còn thì báo ngay trong màn.
> - Chỉ mở được màn này từ mặt đáp án lúc ôn (đúng design). Màn Chi tiết bộ thẻ vẫn chỉ ghi
>   nhãn "Có ảnh nguồn", chưa có lối xem ảnh.
> - Thẻ AI lưu TRƯỚC bản này không có `sourceLine` / `sourceBox`: vẫn có "Trích từ ghi chú
>   chụp…" và xem được cả ảnh, chỉ thiếu đoạn trích và viền tô sáng.
> - Đã kiểm chứng trên emulator ngày 8/10/2026 bằng một ảnh ghi chú có chữ in (xem Mục 6.4):
>   đoạn trích đúng dòng, viền cam ôm đúng dòng trên ảnh, sáng và tối. CHƯA thử được thao tác
>   chụm hai ngón (adb không gửi được cử chỉ hai ngón) — Thi thử bằng tay.
>
> **Màn lỗi AI và màn hết lượt — cách hoạt động (Thi giao Claude tự chốt 8/10/2026) và những chỗ
> design không vẽ:**
> - Là hai chặng mới của `AiCardsRoute`: `Failed` (artboard `AiError`) và `QuotaExceeded`
>   (artboard `AiQuota`). Gọi AI lỗi thì ở lại bước 3 và hiện màn tương ứng, không còn hộp thoại
>   / snackbar chung, không tự lùi về bước 2.
> - MỌI lỗi gọi AI (trừ hết lượt trong ngày) dùng chung màn "Chưa tạo được thẻ", chỉ đổi dòng mô
>   tả và icon: mất mạng / quá 45 giây (icon đám mây gạch chéo, đúng design); các lỗi còn lại
>   dùng icon cảnh báo — AI quá tải (Google trả 429, tức hết hạn mức của cả project), AI không
>   soạn được thẻ nào, nội dung bị chặn, lỗi App Check, bản build chưa bật AI, lỗi khác. Lỗi nào
>   cũng có đủ hai nút "Thử lại" và "Tự gõ thẻ từ văn bản này".
> - "Thử lại": ở lại route, về chặng "Đang tạo thẻ" và gọi lại với đúng văn bản cũ; lần thất bại
>   không bị trừ lượt (lượt chỉ trừ khi AI trả về thẻ).
> - Màn hết lượt: app chặn trước khi gọi mạng (bộ đếm trên máy đã đủ 10). Chỉ có một nút "Tự gõ
>   thẻ từ văn bản này" (đã bỏ "Lưu ghi chú, tạo thẻ sau" theo Mục 4). Ở bước 2, hết lượt vẫn
>   bấm được "Tạo thẻ bằng AI" và vào thẳng màn này.
> - "Tự gõ thẻ từ văn bản này": mở màn Thêm thẻ của bộ đã chọn (`ManualCardRoute` có thêm
>   `noteText`), phía trên có khung "Văn bản ghi chú" CHỈ ĐỌC — design không vẽ khung này. Khung
>   đứng yên khi cuộn các ô nhập, cao tối đa 120dp (dài thì cuộn bên trong), chữ bôi đen để chép
>   được. Thẻ lưu ở đây là thẻ thủ công bình thường (không kèm ảnh nguồn).
> - Nút ← / Back ở hai màn này: về bước 2, không hỏi lại (không có gì để mất). Back ở màn Thêm
>   thẻ: quay lại đúng màn lỗi / hết lượt (chồng màn giữ nguyên, vẫn thử lại AI được).
> - Đang đứng ở màn lỗi / hết lượt mà app bị hệ thống tắt dưới nền: mở lại thấy đúng màn đó,
>   không tự gọi AI (lý do lỗi được cất trong `SavedStateHandle`).
> - Đã kiểm chứng trên emulator: mất mạng (chế độ máy bay) → màn lỗi, sáng và tối; bật lại mạng
>   → "Thử lại" ra thẻ; bộ đếm đủ 10 → màn hết lượt; "Tự gõ thẻ" mở màn Thêm thẻ kèm văn bản.
>   Các loại lỗi khác (AI quá tải, nội dung bị chặn…) chỉ mới qua unit test, chưa dựng được
>   tình huống thật.
>
> **Emulator bị xoá trắng (8/10/2026, khoảng 16:18):** emulator Pixel 7a được khởi động lại với
> dữ liệu trống — app chưa cài, thư viện ảnh trống, mất 5 bộ thẻ mẫu. Hệ quả: debug token App
> Check của emulator là token MỚI, phải đăng ký lại trên Firebase Console (token cũ "Emulator
> Pixel 7a" không còn dùng được); ngôn ngữ riêng của app phải đặt lại `vi-VN`; muốn có lại 4 bộ
> thẻ mẫu thì chạy `DemoDataSeeder`. Lỗi App Check khi token chưa đăng ký đã được kiểm chứng:
> app báo "Lỗi xác thực ứng dụng…" và không trừ lượt. Thi đã đăng ký token mới cùng ngày và
> lệnh gọi AI trên emulator chạy lại bình thường; 4 bộ thẻ mẫu CHƯA được nạp lại.

**Thi**
- [ ] Thử trên máy thật với vở viết tay và chữ in có dấu
- [ ] Kiểm tra App Check hoạt động

### Bước 5 — Cài đặt, nhắc ôn, widget

Màn Cài đặt + màn Ngôn ngữ xong tối 8/10/2026, đã vào `dev` qua pull request #23 (nhánh
`feat/settings`). Thông báo nhắc ôn hằng ngày xong cùng tối, đã vào `dev` qua pull request #24
(nhánh `feat/reminder`). Widget để sau khi nộp.

**Claude**
- [x] Cài đặt: giao diện (theo máy / sáng / tối), nhắc ôn + giờ nhắc, lượt AI hôm nay, xoá dữ liệu trên máy, giới thiệu, chọn ngôn ngữ
- [x] Thông báo nhắc ôn hằng ngày (WorkManager)
- [ ] Widget (Glance): tổng quan 4×2, số thẻ 2×2, ôn nhanh 4×2

> **Màn Cài đặt và màn Ngôn ngữ — cách hoạt động (Thi chốt 8/10/2026) và những chỗ design không vẽ:**
> - Bố cục theo artboard `Settings`, thêm một mục **Ngôn ngữ** (Thi chọn: mục riêng, nằm giữa
>   Giao diện và Nhắc ôn tập) với dòng "Ngôn ngữ giao diện · Tiếng Việt ›" mở màn Ngôn ngữ theo
>   artboard `Language`. Màn Cài đặt cuộn được (máy thấp, cỡ chữ lớn).
> - Giao diện: chọn là đổi ngay, không dựng lại màn. Icon thanh trạng thái / thanh điều hướng
>   đổi màu theo giao diện của APP (`AppSystemBars`), không theo chế độ của máy. Giao diện đã
>   lưu được đọc trước khung hình đầu tiên nên mở app không bị loé. CÒN THIẾU: màn chào của HỆ
>   THỐNG lúc mở app (trước khi app kịp vẽ) vẫn theo chế độ của máy.
> - Ngôn ngữ: chọn là áp dụng ngay, không có nút Lưu; người dùng ở lại màn Ngôn ngữ. Dùng
>   `AppCompatDelegate.setApplicationLocales` (thêm thư viện AppCompat, `MainActivity` kế thừa
>   `AppCompatActivity`, theme gốc đổi sang `Theme.AppCompat.DayNight.NoActionBar`), chạy từ
>   Android 7; Android 13+ còn hiện app trong mục "Ngôn ngữ ứng dụng" của máy
>   (`res/xml/locales_config.xml`). Chưa chọn gì thì app theo ngôn ngữ của máy: máy tiếng Anh
>   → tiếng Anh, còn lại → tiếng Việt. Chuỗi phát từ ViewModel (`StringProvider`) cũng theo ngôn
>   ngữ đã chọn. Màn biết "đang nói tiếng gì" nhờ chuỗi `app_language_tag` (vi / en).
> - Nhắc ôn: mặc định TẮT (thông báo là thứ người dùng tự chọn nhận), giờ gợi ý 20:00. Tắt thì
>   dòng "Giờ nhắc" mờ và không bấm được. Bấm "Giờ nhắc" mở đồng hồ chọn giờ của Material, kiểu
>   12 / 24 giờ theo máy. Màn này MỚI CHỈ LƯU cài đặt (`AppPreferences.reminder`); việc xin quyền
>   thông báo và hẹn giờ bằng WorkManager làm ở phần kế tiếp.
> - Lượt AI: "đã dùng / tổng" đọc từ cùng bộ đếm với màn Kiểm tra văn bản.
> - Đăng nhập Google: chỉ là dòng giới thiệu kèm nhãn "Sắp có", chưa bấm được (bước 6).
> - "Chính sách quyền riêng tư": tạm báo "Tính năng này sắp có." (Thi chọn). "Giới thiệu On Tap
>   TLD": chỉ hiện số phiên bản, không bấm được (Thi chọn). `versionName` đổi thành `1.0.0` cho
>   khớp design.
> - "Xoá toàn bộ dữ liệu trên máy" (Thi chọn phạm vi): hỏi lại, rồi xoá HẲN (không phải đánh dấu
>   xoá) mọi bộ thẻ, thẻ, ghi chú, lịch sử ôn, hàng đợi đồng bộ, ảnh ghi chú và ảnh tạm của luồng
>   chụp. GIỮ giao diện, ngôn ngữ, cài đặt nhắc ôn và bộ đếm lượt AI. Xong thì báo "Đã xoá toàn
>   bộ dữ liệu." và ở lại màn Cài đặt.
> - Tắt việc Material tự pha màu cam vào nền các bề mặt "nổi" (`surfaceTint`): hộp chọn giờ bị
>   ám nâu ở giao diện tối. Từ nay hộp thoại, menu giữ đúng màu nền thẻ.
> - Đã kiểm chứng trên emulator: đổi Việt ⇄ Anh, giao diện Tối khi máy đang sáng (icon thanh
>   trạng thái trắng), bật nhắc + đổi giờ + mở lại app còn nguyên, snackbar "sắp có", hộp thoại
>   xoá (chỉ bấm Huỷ vì emulator đang có bộ thẻ của Thi). Việc xoá thật mới qua test
>   `DeleteAllLocalDataTest` (database trong bộ nhớ) — Thi tự bấm thử khi tiện.
> - Lưu ý khi lên bản này: lần chạy đầu, AppCompat đồng bộ kho ngôn ngữ của nó với hệ thống và
>   XOÁ lựa chọn ngôn ngữ đặt bằng `adb shell cmd locale set-app-locales` trước đó (app về theo
>   máy). Chỉ xảy ra một lần; người dùng thật chưa từng có lựa chọn nào nên không bị ảnh hưởng.

> **Thông báo nhắc ôn hằng ngày — cách hoạt động và những chỗ design không vẽ (nhánh `feat/reminder`):**
> - Tới giờ nhắc, nếu hôm nay CÒN thẻ đến hạn thì hiện thông báo "Đến giờ ôn tập — Bạn có N thẻ
>   cần ôn hôm nay."; không còn thẻ nào thì im lặng. N đếm theo cùng mốc "cần ôn hôm nay" với
>   Home (`dueCutoffMillis`). Mỗi ngày tối đa một thông báo, thông báo mới thay thông báo cũ.
> - Bấm thông báo = mở app như bấm icon ngoài màn hình chính (vào Home, hoặc trở lại màn đang
>   dở); thông báo tự biến mất. Chưa mở thẳng vào phiên ôn.
> - Hẹn giờ bằng WorkManager: mỗi lần hẹn MỘT việc cho giờ nhắc gần nhất sắp tới
>   (`ReviewReminderWorker`); việc đó chạy xong thì tự hẹn lần của ngày hôm sau. Lịch sống qua
>   lần khởi động lại máy. Giờ chạy KHÔNG chính xác tới từng giây: máy đang tiết kiệm pin có thể
>   lùi vài phút (đổi lại không cần xin quyền "báo thức chính xác").
> - `OnTapTldApp` nghe cài đặt nhắc ôn suốt đời app (`KeepReminderScheduledUseCase`): bật / đổi
>   giờ thì hẹn lại, tắt thì huỷ lịch. Màn Cài đặt chỉ việc lưu cài đặt.
> - Quyền thông báo: bật công tắc "Nhắc ôn hằng ngày" trên Android 13+ thì hệ thống hỏi quyền.
>   Đồng ý → công tắc bật. Từ chối, hoặc thông báo của app đang bị tắt trong Cài đặt của máy →
>   công tắc vẫn tắt và hiện hộp thoại "Chưa bật được nhắc ôn" có nút "Mở Cài đặt". Nếu về sau
>   người dùng tự tắt quyền trong Cài đặt của máy thì tới giờ app không gửi gì; công tắc trong
>   app CHƯA tự tắt theo.
> - Kênh thông báo "Nhắc ôn tập" (mức thường: có âm báo, không bật lên che màn hình). Icon nhỏ
>   dùng `ic_cards`, màu cam.
> - Ngôn ngữ của thông báo theo ngôn ngữ đã chọn trong app, kể cả lúc app đang đóng: lựa chọn ở
>   màn Ngôn ngữ được ghi thêm một bản sao vào `AppPreferences.languageTag` cho việc nền đọc
>   (cần cho Android cũ hơn 13).
> - Đã kiểm chứng trên emulator: bật công tắc → hộp xin quyền của hệ thống → lịch được hẹn cho
>   20:00 hôm sau; ép lịch chạy ngay → thông báo đúng nội dung, đúng 9 thẻ, và lịch kế tiếp tự
>   được hẹn; bấm thông báo → về đúng màn đang mở; tắt công tắc → lịch bị huỷ. CHƯA thử: đợi
>   tới đúng giờ thật, khởi động lại máy, nhánh từ chối quyền (mới qua đọc code), máy OPPO.
> - Cách ép lịch chạy ngay để thử: `adb shell dumpsys jobscheduler | grep ontaptld` lấy số job
>   (dạng `…:u0a213/2`), rồi
>   `adb shell cmd jobscheduler run -f -n androidx.work.systemjobscheduler com.ledinhthi.ontaptld <số>`.

**Thi**
- [ ] Thử thông báo trên máy thật (để tới đúng giờ nhắc, cả khi app đang đóng)

### Bước 6 — Tài khoản và đồng bộ *(cần bước 0, phần Auth + Firestore)*

**Claude**
- [x] Đăng nhập Google tuỳ chọn, vào từ Cài đặt
- [ ] Đồng bộ hai chiều Local ⇄ Firestore (push qua `sync_queue`, pull theo `updatedAt`, Last-Write-Wins)
- [ ] Gán `userId` cho dữ liệu cũ khi đăng nhập lần đầu
- [x] Cài đặt khi đã đăng nhập: thẻ tài khoản, đăng xuất (trạng thái đồng bộ để cùng phần đồng bộ)
- [ ] Xoá tài khoản (xác nhận bằng cách nhập "XÓA", đăng nhập lại nếu cần)

> **Màn Đăng nhập Google — cách hoạt động (Thi chốt 9/10/2026) và những chỗ khác design (nhánh
> `feat/google-sign-in`):**
> - Trước khi nộp chỉ làm ĐĂNG NHẬP; đồng bộ Firestore và xoá tài khoản để sau. Vì vậy lời trên
>   màn được viết lại cho đúng thực tế (artboard `LoginGoogle` hứa "đồng bộ", "dùng trên nhiều
>   thiết bị", "xoá tài khoản bất cứ lúc nào"): tiêu đề "Đăng nhập bằng Google", ba ý "thẻ vẫn lưu
>   trên máy này / ảnh không tải lên / đăng xuất bất cứ lúc nào", và dòng cuối "chỉ dùng tên và
>   địa chỉ email" thay cho câu đồng ý với Chính sách quyền riêng tư (trang đó chưa có). Bố cục,
>   kích thước, màu giữ đúng artboard. Khi làm đồng bộ thì đổi lời lại theo design.
> - Mở từ dòng "Đăng nhập Google" ở màn Cài đặt (route `GoogleSignInRoute`). Nút ←, Back và "Để
>   sau" đều về Cài đặt.
> - "Tiếp tục với Google" mở hộp chọn tài khoản của hệ thống (Credential Manager, kiểu "Sign in
>   with Google"); app không thấy mật khẩu, chỉ nhận một ID token rồi đưa cho Firebase Auth. Nút
>   dùng logo "G" bốn màu chuẩn (`ic_google_logo.xml`) thay chữ "G" tạm của design.
> - Đóng hộp chọn tài khoản: ở lại màn, không báo gì. Đăng nhập xong: báo "Đã đăng nhập bằng
>   <email>" rồi về Cài đặt. Lỗi (mất mạng, máy chưa có tài khoản Google, lỗi khác): báo bằng
>   thông báo ngắn, ở lại màn.
> - Firebase tự nhớ phiên đăng nhập; mở lại app vẫn còn đăng nhập. Đăng nhập KHÔNG đụng tới dữ
>   liệu trên máy (chưa gán `userId` — việc đó thuộc phần đồng bộ).
> - Bản build không có `google-services.json` vẫn biên dịch được: web client ID được tìm theo tên
>   lúc chạy, thiếu thì bấm nút sẽ báo "Đăng nhập Google chưa được bật trong bản build này."
> - Màn email / mật khẩu cũ (`feature/auth/presentation/login`) và các chuỗi `login_*` đã xoá.
>   README còn nhắc `LoginScreen` / `LoginRoute` làm ví dụ — sửa khi viết lại README.
> - Đã kiểm chứng trên emulator (Pixel 8a): màn sáng / tối, bấm nút ra hộp chọn tài khoản, đóng
>   hộp thì im lặng, "Để sau" về Cài đặt; Thi tự chọn tài khoản và đăng nhập thật thành công
>   (9/10/2026, tài khoản hiện trong Firebase Console → Authentication). CHƯA kiểm chứng: các
>   nhánh lỗi (mới qua unit test).
>
> **Cài đặt khi đã đăng nhập — cách hoạt động (Thi chốt 9/10/2026) và những chỗ khác design:**
> - KHÔNG dựng lại màn theo artboard `SettingsAccount` (artboard gom Giao diện / Ngôn ngữ / Nhắc
>   ôn thành một danh sách gọn). Giữ bố cục Cài đặt hiện có, chỉ đổi hai chỗ khi đã đăng nhập:
>   (1) ở mục "Đồng bộ", thẻ tài khoản thế chỗ dòng "Đăng nhập Google"; (2) thêm mục "Tài
>   khoản" ở cuối màn với dòng "Đăng xuất" và câu "Đăng xuất không xoá thẻ đang có trên máy này."
> - Thẻ tài khoản: ô tròn mang chữ cái đầu của tên (không tải ảnh đại diện Google), tên, email.
>   Tài khoản không có tên thì email lên làm dòng chính. Hàng dưới của design ("Đã đồng bộ · 2
>   phút trước" + "Đồng bộ ngay") tạm thay bằng "Đồng bộ đám mây · Sắp có" cho tới khi làm đồng bộ.
> - Dòng "Xoá tài khoản" và "Thống kê ôn tập" của artboard CHƯA hiện (chưa làm).
> - "Đăng xuất" hỏi lại ("Đăng xuất?" — Huỷ / Đăng xuất). Xác nhận thì đăng xuất khỏi Firebase,
>   báo Credential Manager quên tài khoản vừa dùng (lần sau được chọn lại tài khoản), báo "Đã
>   đăng xuất." và ở lại Cài đặt; dữ liệu trên máy giữ nguyên.
> - Màn Cài đặt nghe "ai đang đăng nhập" (`ObserveAuthUserUseCase`), nên đăng nhập / đăng xuất
>   xong là giao diện đổi ngay, và cài lại app (không xoá dữ liệu) vẫn còn đăng nhập.
> - Đã kiểm chứng trên emulator: sau khi Thi đăng nhập, thẻ tài khoản hiện đúng tên + email,
>   sáng và tối; cài đè bản mới vẫn còn đăng nhập; bấm "Đăng xuất" ra hộp hỏi lại, Huỷ thì giữ
>   nguyên. CHƯA kiểm chứng: bấm "Đăng xuất" thật rồi đăng nhập lại (Thi tự bấm; mới qua unit test).

**Thi**
- [ ] Đặt Firestore Security Rules ([`docs_tld.md`](docs_tld.md) Mục 7.2)
- [ ] Thử đồng bộ trên hai máy

### Bước 7 — Thống kê, onboarding, ngôn ngữ

**Claude**
- [ ] Màn thống kê: chuỗi ngày ôn, số thẻ ôn mỗi ngày, tỉ lệ nhớ
- [ ] Ba màn onboarding
- [ ] Chọn ngôn ngữ, bản dịch tiếng Anh (màn chọn ngôn ngữ đã làm ở bước 5; còn soát bản dịch)

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
- Icon ứng dụng: ĐÃ LÀM ngày 8/10/2026 (nhánh `feat/launcher-icon`) — nền cam `#F24E1E`, chữ "TLD" trắng. Ba chữ cái được vẽ lại thành hình trong `drawable/ic_launcher_foreground.xml` (vector drawable không chứa được chữ; dáng gần với Nunito Sans ExtraBold nhưng không phải lấy từ font). Android 8+ dùng icon adaptive; Android 7.x dùng bộ PNG trong `mipmap-*dpi` (sinh từ cùng hình đó). Màn chào của hệ thống trên Android 12+ (`values-v31/themes.xml`) hiện đúng ô logo 96dp của màn Splash thay cho icon bị cắt tròn; logo ở màn Splash của app nằm cao hơn khoảng 40dp vì còn tên app bên dưới, nên lúc nối hai màn logo nhích lên một chút. Tên dưới icon (`app_name`) đã đổi từ "OnTapTLD" thành "On Tap TLD" (Thi chốt 8/10/2026, làm kèm nhánh `feat/source-image`).
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

Đang làm **bước 4 — Chụp ảnh + AI**, mỗi màn một nhánh. Màn Chụp ghi chú đã vào `dev` (pull
request #12), màn Kiểm tra văn bản (#13), phần gọi Gemini + màn "Đang tạo thẻ" (#14) và màn duyệt
thẻ AI + lưu thẻ (#15), màn lỗi AI + màn hết lượt (#17), xem ảnh nguồn của thẻ AI (#21) cũng vậy.
Hai việc dọn dẹp cuối (#22) cũng vậy — bước 4 xong.

Đang làm **bước 5 — Cài đặt, nhắc ôn, widget**. Màn Cài đặt + màn Ngôn ngữ (nhánh
`feat/settings`) đã vào `dev` (#23). Thông báo nhắc ôn hằng ngày (nhánh `feat/reminder`) đã vào
`dev` (#24). Artboard: xem bảng ở Mục 6.3.

Đang làm **bước 6 — chỉ phần đăng nhập Google** trên nhánh `feat/google-sign-in` (9/10/2026):
màn Đăng nhập Google và Cài đặt khi đã đăng nhập (thẻ tài khoản, Đăng xuất) đã code xong, chưa
commit — chờ Thi thử Đăng xuất rồi commit / tạo pull request. Việc kế tiếp: nạp 4 bộ thẻ mẫu và
viết README.

**Emulator đổi sang Pixel 8a API 37.1 (9/10/2026):** dữ liệu trắng, chưa có bộ thẻ nào, chưa có
ảnh `ontap_demo_note.jpg`, đã thêm một tài khoản Google. Ngôn ngữ riêng của app đã đặt `vi`.
Debug token App Check của máy ảo này là token mới — phải đăng ký trên Firebase Console thì "Tạo
thẻ bằng AI" mới chạy (đăng nhập Google không cần, vì Auth không Enforce App Check).

**Thi chốt tối 8/10/2026 — để SAU khi nộp:** widget (Glance), và việc soát giao diện trên máy
nhỏ / tablet / xoay ngang / cỡ chữ lớn. Việc còn lại trước khi nộp, theo thứ tự: đăng nhập
Google + màn Cài đặt khi đã đăng nhập → nạp 4 bộ thẻ mẫu và viết README.

**Phạm vi trước khi nộp hồ sơ Fresher Android (Braly, hạn 15/10/2026 — Thi chốt 8/10/2026):**
làm tới hết đăng nhập Google ở bước 6 (đăng nhập + màn Cài đặt khi đã đăng nhập); đồng bộ
Firestore và xoá tài khoản để sau khi nộp. Thứ tự: hoạt ảnh lật thẻ (đã vào `dev`, pull request
#18) → icon launcher (đã vào `dev`, pull request #20) → phần còn lại của bước 4 → bước 5 → đăng
nhập Google. Tin tuyển dụng nêu rõ animation và giao diện cho nhiều cỡ màn hình; việc soát app
trên máy nhỏ / tablet / xoay ngang / cỡ chữ lớn Thi đã chốt để sau khi nộp (xem đoạn trên).

Chụp và OCR chạy hoàn toàn trên máy; gọi Gemini cần mạng và App Check. Debug token của emulator
Pixel 7a đã được kiểm chứng bằng lần gọi AI thật ngày 8/10/2026; token của máy OPPO CPH1911 đã
đăng ký nhưng chưa thử. Thi tự thử trên máy thật ở cuối bước; Claude kiểm tra trên emulator
(camera của emulator chỉ là cảnh ảo, không dùng để đánh giá chất lượng OCR được). Emulator đang
thiếu bộ nhớ: adb hay rớt kết nối và có lần hệ thống tự tắt app (LOW_MEMORY) — không phải app
crash.

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
  `app/src/androidTest/.../tools/DemoDataSeeder.kt`). Từ lần emulator bị xoá trắng ngày
  8/10/2026, 4 bộ thẻ mẫu CHƯA được nạp lại.
- Chạy riêng một lớp test có emulator mà KHÔNG gỡ app (vd `MigrationTest` sau khi đổi schema):
  `./gradlew :app:assembleDebugAndroidTest`, `adb install -r -t <…androidTest.apk>`, rồi
  `adb shell am instrument -w -e class com.ledinhthi.ontaptld.core.data.local.db.MigrationTest
  com.ledinhthi.ontaptld.test/androidx.test.runner.AndroidJUnitRunner`. Cùng cách đó cho
  `com.ledinhthi.ontaptld.feature.deck.NoteCleanupDaoTest` (SQL dọn ghi chú).
- Giả lập "app bị hệ thống tắt dưới nền": bấm Home (`adb shell input keyevent 3`), rồi
  `adb shell run-as com.ledinhthi.ontaptld kill <pid>` (pid lấy bằng `adb shell pidof …`), rồi mở
  lại app. `am kill` không tắt được process trên emulator này.
- Camera của emulator không có chữ. Để thử OCR / ảnh nguồn, thư viện ảnh của emulator có sẵn
  `Pictures/ontap_demo_note.jpg` (một trang ghi chú chữ in, Claude tạo ngày 8/10/2026): vào màn
  chụp, bấm "Thư viện" rồi chọn ảnh này.
- App trên emulator đang được đặt ngôn ngữ riêng là tiếng Việt (máy ảo để tiếng Anh). Đổi
  bằng màn Cài đặt → Ngôn ngữ của app, hoặc
  `adb shell cmd locale set-app-locales com.ledinhthi.ontaptld --locales vi`; xem bằng
  `… get-app-locales …`.
- Xem dark theme: `adb shell cmd uimode night yes`, chụp xong trả lại `night no`.
- **Đừng xoá dữ liệu app** (`pm clear`, gỡ cài đặt): sẽ mất debug token App Check mà Thi đã đăng
  ký trên Firebase Console, phải lấy token mới trong Logcat và đăng ký lại.
