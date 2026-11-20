# Kiến trúc Widget Premium

Trạng thái: giai đoạn 1, 2 và 3a (lịch Âm, thông số máy). Thời tiết (3b) chưa làm. Các phần ghi "dự kiến" chưa được xây.

## Nguyên tắc
- Một module `app` ở giai đoạn đầu. Khi mã lớn lên sẽ tách module theo ranh giới dưới đây, không tách sớm.
- Logic thuần (lịch âm, định dạng, trạng thái) viết riêng khỏi Android để unit test được trên CI.
- Mất mạng hay thiếu cảm biến thì ẩn hoặc dùng dữ liệu đã lưu, không bao giờ hiện số giả.

## Thành phần
| Thành phần | Vai trò | Trạng thái |
|---|---|---|
| `MainActivity` (Compose) | Trang chủ, nút ghim widget, hướng dẫn thêm widget | Có |
| `ClockWidgetProvider` | Widget đồng hồ + ngày bằng RemoteViews/TextClock, chạm để mở app | Có |
| `LunarCalendar` (thuần Kotlin, có test) | Đổi dương sang âm theo UTC+7, tháng nhuận, can chi của năm. Tiết khí: dự kiến | Có (3a) |
| `weather` | Gọi Open-Meteo, lưu bản mới nhất để dùng khi mất mạng | Dự kiến, giai đoạn 3b |
| `DeviceStats` + `InfoWidgetProvider` | Pin, nhiệt độ pin, RAM, bộ nhớ trong; mục nào máy không có thì ẩn. Làm mới theo lịch hệ thống (~30 phút) và khi đổi ngày, đổi giờ, cắm/rút sạc, pin yếu | Có (3a) |
| `data` (Room) | Ghi chú theo ngày, công việc, nhắc việc | Dự kiến, giai đoạn 4 |
| `reminder` | AlarmManager đặt nhắc đúng giờ, khôi phục sau khi khởi động lại | Dự kiến, giai đoạn 4 |
| `refresh` (WorkManager) | Làm mới thời tiết theo lịch | Dự kiến, giai đoạn 3b |
| `theme` | Standard và Liquid Glass, màu, độ trong suốt, sáng/tối | Dự kiến, giai đoạn 5 |

## Cách widget hiển thị đồng hồ
Widget dùng `TextClock` bên trong RemoteViews: hệ thống tự cập nhật giờ và ngày, không cần dịch vụ nền hay `updatePeriodMillis`. Widget không thể tự chạy mã từng giây, nên chỉ các số liệu cần dữ liệu ngoài (thời tiết, pin, RAM) mới phải làm mới theo lịch hoặc theo sự kiện.

## Quy ước dữ liệu
- Ghi chú và cài đặt lưu cục bộ, `allowBackup=false`.
- Dữ liệu gửi ra ngoài duy nhất: tọa độ hoặc tên thành phố gửi tới Open-Meteo (giai đoạn 3).

## Build
GitHub Actions: job `build` dựng APK debug và đăng lên pre-release `latest-debug`; job `test` chạy unit test song song.
