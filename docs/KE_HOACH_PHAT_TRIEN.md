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

**Claude**
- [ ] Thêm token màu mới theo design vào `Color.kt` (cam đậm, xanh đậm, đỏ đậm cho chữ trên nền nhạt; các tông dark tương ứng)
- [ ] Component dùng chung: nút chính / phụ, thẻ, badge "Thủ công" / "AI", top bar, trạng thái rỗng / lỗi / đang tải
- [ ] Thêm bảng `sync_queue` vào schema Room
- [ ] Đưa chuỗi vào `strings.xml` (vi / en) ngay từ đầu
- [ ] Splash vào thẳng Home; gỡ màn Login email khỏi luồng điều hướng
- [ ] Nếu đã có `google-services.json`: gắn plugin Firebase vào Gradle, khởi tạo App Check; đưa file vào `.gitignore`

**Thi**
- [ ] Review, merge
- [ ] Cho phép Claude tải font Nunito Sans, hoặc tự thả file `.ttf` vào `res/font/`

### Bước 2 — Bộ thẻ

**Claude**
- [ ] Home: thẻ "Cần ôn hôm nay", hai lối tạo thẻ (chụp ghi chú / gõ tay), danh sách bộ thẻ
- [ ] Chi tiết bộ thẻ: thống kê, lọc Tất cả / Thủ công / AI, vuốt để sửa / xoá
- [ ] Thêm / sửa thẻ thủ công (có "lưu xong thêm thẻ tiếp")
- [ ] Tạo bộ thẻ: bottom sheet tên + màu nhận diện
- [ ] Splash theo design
- [ ] Đủ 3 trạng thái Empty / Loading / Error cho mọi màn danh sách

**Thi**
- [ ] Chạy thử trên máy, so với design, báo chỗ lệch

### Bước 3 — Ôn tập

**Claude**
- [ ] Màn câu hỏi, màn đáp án + 4 mức (Quên / Khó / Dễ / Rất dễ), màn hoàn thành phiên
- [ ] Lấy thẻ đến hạn theo toàn bộ hoặc theo từng bộ thẻ
- [ ] Unit test cho luồng ôn

**Thi**
- [ ] Chạy thử một phiên ôn đầy đủ

### Bước 4 — Chụp ảnh + AI *(cần bước 0)*

**Claude**
- [ ] CameraX + crop vùng chữ
- [ ] OCR bằng ML Kit, màn sửa văn bản
- [ ] Gọi Gemini qua Firebase AI Logic, màn "đang tạo thẻ"
- [ ] Màn duyệt thẻ AI đề xuất (chọn / sửa / xoá / thêm, nút báo cáo nội dung AI)
- [ ] Giới hạn lượt AI mỗi ngày; màn lỗi mạng và màn hết lượt
- [ ] Xem ảnh nguồn của thẻ AI (tô sáng đúng vùng `sourceBox`)

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
