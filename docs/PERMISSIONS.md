# Danh sách quyền Android

Nguyên tắc: chỉ xin quyền khi tính năng cần nó chạy được ngay, và chỉ ở mức thấp nhất.

| Quyền | Khi nào | Lý do | Giai đoạn |
|---|---|---|---|
| (không có) | Hiện tại | Widget đồng hồ không cần quyền | 2 |
| `INTERNET` | Thời tiết | Gọi Open-Meteo | 3 |
| `ACCESS_COARSE_LOCATION` (tùy chọn) | Khi người dùng bấm "Dùng vị trí của tôi" lúc đang mở app | Thời tiết theo vị trí. Có thể thay bằng nhập thành phố thủ công | 3 |
| `POST_NOTIFICATIONS` | Khi người dùng bật nhắc việc | Hiện thông báo nhắc | 4 |
| `SCHEDULE_EXACT_ALARM` hoặc `USE_EXACT_ALARM` | Nhắc việc đúng giờ trên Android 12+ | Cân nhắc kỹ chính sách Google Play trước khi dùng | 4 |
| `RECEIVE_BOOT_COMPLETED` | Nhắc việc | Đặt lại báo thức sau khi khởi động | 4 |

Không dùng: vị trí nền, quyền lưu trữ rộng, dịch vụ nền chạy liên tục, quyền root.
