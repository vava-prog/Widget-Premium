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
