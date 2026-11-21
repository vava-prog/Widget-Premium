# Danh sách quyền Android

Nguyên tắc: chỉ xin quyền khi tính năng cần nó chạy được ngay, và chỉ ở mức thấp nhất.

| Quyền | Khi nào | Lý do | Giai đoạn |
|---|---|---|---|
| `INTERNET` | Đã dùng | Gọi Open-Meteo để tìm thành phố và lấy dự báo (HTTPS) | 3b |
| `ACCESS_COARSE_LOCATION` (tùy chọn) | Chưa dùng | Có thể thêm sau nút "Dùng vị trí của tôi" khi đang mở app. Hiện người dùng tự nhập thành phố | sau 3b |
| `POST_NOTIFICATIONS` | Khi người dùng bật nhắc việc | Hiện thông báo nhắc | 4 |
| `SCHEDULE_EXACT_ALARM` hoặc `USE_EXACT_ALARM` | Nhắc việc đúng giờ trên Android 12+ | Cân nhắc kỹ chính sách Google Play trước khi dùng | 4 |
| `RECEIVE_BOOT_COMPLETED` | Nhắc việc | Đặt lại báo thức sau khi khởi động | 4 |

Không dùng: vị trí nền, quyền lưu trữ rộng, dịch vụ nền chạy liên tục, quyền root.

## Phase 4: ghi chú và nhắc giờ (tab Lịch Việt)
- `POST_NOTIFICATIONS` (Android 13+): hiện thông báo nhắc. Chỉ xin sau khi người dùng lưu ghi chú có đặt giờ nhắc; từ chối thì ghi chú vẫn lưu, chỉ không hiện thông báo.
- `SCHEDULE_EXACT_ALARM`: nhắc đúng giờ. Nếu Android không cấp, ứng dụng tự dùng báo thức gần đúng (có thể lệch vài phút), không bắt buộc người dùng cấp.
- `RECEIVE_BOOT_COMPLETED`: Android xóa báo thức khi khởi động lại hoặc cập nhật app, nên ứng dụng đặt lại các nhắc còn hiệu lực từ cơ sở dữ liệu trên máy. Không có dịch vụ chạy nền liên tục.
- Ghi chú lưu bằng Room trên máy, không gửi đi đâu (xem PRIVACY.md).
