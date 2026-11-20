# Kiến trúc Widget Premium

Trạng thái: giai đoạn 1 (thiết kế) và 2 (khung dự án + widget đồng hồ). Các phần ghi "dự kiến" chưa được xây.

## Nguyên tắc
- Một module `app` ở giai đoạn đầu. Khi mã lớn lên sẽ tách module theo ranh giới dưới đây, không tách sớm.
- Logic thuần (lịch âm, định dạng, trạng thái) viết riêng khỏi Android để unit test được trên CI.
- Mất mạng hay thiếu cảm biến thì ẩn hoặc dùng dữ liệu đã lưu, không bao giờ hiện số giả.

## Thành phần
| Thành phần | Vai trò | Trạng thái |
|---|---|---|
| `MainActivity` (Compose) | Trang chủ, nút ghim widget, hướng dẫn thêm widget | Có |
| `ClockWidgetProvider` | Widget đồng hồ + ngày bằng RemoteViews/TextClock, chạm để mở app | Có |
| `lunar` (thuần Kotlin) | Tính lịch Âm – Dương, can chi, tiết khí | Dự kiến, giai đoạn 3 |
| `weather` | Gọi Open-Meteo, lưu bản mới nhất để dùng khi mất mạng | Dự kiến, giai đoạn 3 |
| `device` | Pin, nhiệt độ pin, RAM, bộ nhớ trong; mục nào máy không có thì ẩn | Dự kiến, giai đoạn 3 |
| `data` (Room) | Ghi chú theo ngày, công việc, nhắc việc | Dự kiến, giai đoạn 4 |
| `reminder` | AlarmManager đặt nhắc đúng giờ, khôi phục sau khi khởi động lại | Dự kiến, giai đoạn 4 |
| `refresh` (WorkManager) | Làm mới thời tiết và số liệu theo lịch | Dự kiến, giai đoạn 3 |
| `theme` | Standard và Liquid Glass, màu, độ trong suốt, sáng/tối | Dự kiến, giai đoạn 5 |

## Cách widget hiển thị đồng hồ
Widget dùng `TextClock` bên trong RemoteViews: hệ thống tự cập nhật giờ và ngày, không cần dịch vụ nền hay `updatePeriodMillis`. Widget không thể tự chạy mã từng giây, nên chỉ các số liệu cần dữ liệu ngoài (thời tiết, pin, RAM) mới phải làm mới theo lịch hoặc theo sự kiện.

## Quy ước dữ liệu
- Ghi chú và cài đặt lưu cục bộ, `allowBackup=false`.
- Dữ liệu gửi ra ngoài duy nhất: tọa độ hoặc tên thành phố gửi tới Open-Meteo (giai đoạn 3).

## Build
GitHub Actions: job `build` dựng APK debug và đăng lên pre-release `latest-debug`; job `test` chạy unit test song song.
