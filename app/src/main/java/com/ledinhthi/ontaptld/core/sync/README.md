core/sync/ — đồng bộ Local ⇄ Firestore (docs_tld Mục 8.5, docs kiến trúc Mục 20).

Đã có: `SyncQueueEntity` + `SyncQueueDao` (bảng `sync_queue`, schema Room v2).
Chưa có (bước 6 của docs/KE_HOACH_PHAT_TRIEN.md): `SyncScheduler` (WorkManager),
`PushSyncUseCase`, `PullSyncUseCase`.
