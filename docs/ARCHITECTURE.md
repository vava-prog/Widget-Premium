# Kiến trúc Widget Premium

Trạng thái: giai đoạn 1, 2, 3a (lịch Âm, thông số máy) và 3b (thời tiết). Dự báo theo giờ chưa làm. Các phần ghi "dự kiến" chưa được xây.

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
| `WeatherApi`, `WeatherParsers`, `WeatherStore`, `WeatherSection` | Tìm thành phố, lấy dự báo hiện tại và 7 ngày từ Open-Meteo, lưu bản mới nhất để dùng khi mất mạng, hiện trong app và trên widget | Có (3b) |
| `DeviceStats` + `InfoWidgetProvider` | Pin, nhiệt độ pin, RAM, bộ nhớ trong; mục nào máy không có thì ẩn. Làm mới theo lịch hệ thống (~30 phút) và khi đổi ngày, đổi giờ, cắm/rút sạc, pin yếu | Có (3a) |
| `Notes.kt` (Room), `NoteRepository` | Ghi chú theo ngày, đánh dấu hoàn thành, giờ nhắc; lưu trên máy | Có (4) |
| `Reminders.kt` (`ReminderScheduler`, `ReminderReceiver`, `ReminderRestoreReceiver`) | AlarmManager đặt nhắc (báo thức chính xác nếu được cấp, không thì báo thức kiểu đồng hồ), thông báo, đặt lại sau khi khởi động lại hoặc cập nhật app | Có (4) |
| `VietCalendar`, `CalendarScreen` | Tab Lịch Việt: lưới tháng vuốt đổi tháng, chi tiết ngày (can chi, Hoàng đạo, giờ Hoàng đạo, tiết khí, Trực, Nguyệt kỵ, Tam nương), ngày lễ, ghi chú | Có (4) |
| `WeatherWorker` (WorkManager) | Làm mới thời tiết mỗi 30 phút khi có mạng, rồi cập nhật widget | Có (3b) |
| `WidgetStyle`, `WidgetBackground`, `StyleSection` | Standard và Kính mờ, màu chủ đạo, độ đậm nền, sáng/tối (tự động theo mặt trời hoặc cố định); nền vẽ thành ảnh theo kích thước widget | Có (5) |

## Cách widget hiển thị đồng hồ
Widget dùng `TextClock` bên trong RemoteViews: hệ thống tự cập nhật giờ và ngày, không cần dịch vụ nền hay `updatePeriodMillis`. Widget không thể tự chạy mã từng giây, nên chỉ các số liệu cần dữ liệu ngoài (thời tiết, pin, RAM) mới phải làm mới theo lịch hoặc theo sự kiện.

## Quy ước dữ liệu
- Ghi chú và cài đặt lưu cục bộ, `allowBackup=false`.
- Dữ liệu gửi ra ngoài duy nhất: tọa độ hoặc tên thành phố gửi tới Open-Meteo (giai đoạn 3).

## Build
GitHub Actions: job `build` dựng APK debug và đăng lên pre-release `latest-debug`; job `test` chạy unit test song song; job `quality` chạy lint và dựng bản release (có R8 thu nhỏ mã) để bắt lỗi trước khi phát hành.

## Bổ sung sau Phase 7
| Thành phần | Việc làm | Ghi chú |
|---|---|---|
| `WeatherSky`, `SkyArt` | Chọn và vẽ bầu trời theo thời tiết đã lưu cho giao diện "Theo thời tiết" | Logic chọn loại trời và màu là thuần, có test; việc vẽ bằng Canvas, tự thiết kế |
| `WeekWidgetProvider` | Widget Lịch tuần 4×2: giờ, ngày âm dương, thẻ thời tiết, dải 7 ngày | Dải tuần hiện ngày âm hoặc thời tiết 7 ngày (`WeekStrip`, `VietCalendar.weekDays`) |
| `DeviceLocation` | Nút "Dùng vị trí của máy" | Quyền vị trí gần đúng, chỉ khi bấm, lấy một lần, làm tròn ~1 km, không chạy nền |
| `Diagnostics`, `Age` | Thẻ "Số liệu hiện tại" và tình trạng nhắc việc | Đọc trực tiếp từ máy, không gửi đi đâu |
| `WidgetStyle` (mở rộng) | Viền, cỡ đồng hồ, giây, tên tự nhập, màu chữ (mỗi giao diện một màu), độ mờ/nổi khối kính, tùy chọn hiển thị | Lưu trong SharedPreferences "style"; hàm tính màu và định dạng là thuần, có test |
