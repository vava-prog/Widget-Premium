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
| `WeekWidgetProvider` | Widget Lịch tuần 4×2: giờ, ngày âm dương, thẻ thời tiết, dải 7 ngày | Dải tuần hiện ngày âm, thời tiết 7 ngày hoặc dự báo theo giờ (`WeekStrip`, `VietCalendar.weekDays`; dải giờ lấy từ `Weather.hourly`, có test) |
| `DeviceLocation` | Nút "Dùng vị trí của máy" | Quyền vị trí gần đúng, chỉ khi bấm, lấy một lần, làm tròn ~1 km, không chạy nền |
| `Diagnostics`, `Age` | Thẻ "Số liệu hiện tại" và tình trạng nhắc việc | Đọc trực tiếp từ máy, không gửi đi đâu |
| `WidgetStyle` (mở rộng) | Viền, cỡ đồng hồ, giây, tên tự nhập, màu chữ (mỗi giao diện một màu), độ mờ/nổi khối kính, tùy chọn hiển thị | Lưu trong SharedPreferences "style"; hàm tính màu và định dạng là thuần, có test |
| `WeatherMotion`, `WidgetMotion`, `res/drawable/fx_*`, `res/animator/fx_*` | Hiệu ứng thời tiết bán động trên nền bầu trời | Logic chọn hiệu ứng là thuần, có test; tài nguyên do `tools/gen_weather_fx.py` sinh ra và có test kiểm tra thời lượng |

## Hiệu ứng thời tiết bán động: cách làm và giới hạn
- **Giới hạn của Android:** widget là RemoteViews do launcher dựng, không chạy được mã của app, không phát GIF và không có vòng lặp vẽ liên tục. Cập nhật widget theo giây hoặc dựng sẵn nhiều ảnh động đều tốn pin và vượt giới hạn bộ nhớ bitmap của RemoteViews.
- **Cách làm:** mỗi hiệu ứng là một `AnimatedVectorDrawable` hữu hạn (vài chục KB XML, không bitmap) đặt làm `indeterminateDrawable` của một `ProgressBar`. `ProgressBar` là lớp RemoteViews cho phép, và tự chạy hoạt ảnh mỗi khi view hiện ra hoặc được gắn lại, nên hiệu ứng chạy lại khi màn hình chính hiện ra lại (quay về từ app khác, bật màn hình) hoặc khi widget được cập nhật. Chạy đủ 60 giây thì dừng ở khung hình tĩnh, không còn tốn tài nguyên. Launcher không báo cho app khi người dùng vuốt giữa các trang, nên không bắt được "vừa vuốt tới" để chạy lại ngay; thời lượng dài (khoảng 1 phút) giúp vẫn thấy chuyển động sau vài lần vuốt, và `WidgetMotion` ẩn rồi hiện lại view trong mỗi lần cập nhật widget để hiệu ứng chạy lại từ đầu.
- **Xem trước trong app dùng đúng widget thật:** `ClockWidgetProvider`, `InfoWidgetProvider` và `WeekWidgetProvider` đều có `buildViews(context, w, h)` nhận kích thước dp; widget ngoài màn hình lấy kích thước từ launcher, còn `StylePreview` truyền kích thước khung rồi `RemoteViews.apply` vào một `FrameLayout` (chặn chạm). Nhờ vậy khung xem trước luôn khớp widget thật, kể cả ngày âm, dự báo và hiệu ứng mưa, không phải vẽ lại bằng Compose.
- **Chỉ các lớp cần thiết hiện:** `WidgetMotion` bật `ProgressBar` của hiệu ứng theo thời tiết (giông bật hai lớp: mưa kính và chớp sáng, xem `WeatherMotion.layers`), các cái khác `GONE` nên không chạy. Mưa là giọt nước bám kính vẽ bằng vector có gradient: giọt hình giọt (thân tròn, đuôi nhọn nghiêng ngẫu nhiên; đỉnh hơi tối, đáy sáng, điểm phản chiếu, bóng đổ). 37 "giọt mẫu" được dùng lại nhiều lần: mỗi giọt mẫu có một animator keyframe 60 s gồm nhiều sự kiện nối nhau (hiện ra ở chỗ mới khi đang ẩn, ở lại, trượt, tan), nên khoảng 50-60 giọt mới ngưng tụ ngẫu nhiên trong một phút. Vị trí chọn theo kiểu "ứng viên tốt nhất" (best-candidate) để trải đều, tránh chồng lên giọt đang có. Giọt trượt rơi nhanh dần (nội suy `fx_fall` ~ t², rồi `fx_brake` hãm lại; giọt dãn dài khi tuột, co lại và nảy nhẹ khi dừng, nhỏ dần vì để lại nước), kèm vệt nước mảnh kéo dài theo giọt rồi mỏng đi và biến mất. Ba nội suy riêng nằm ở `res/interpolator/fx_*` nên không phụ thuộc mức API. Cuối chu kỳ mọi giọt về đúng trạng thái ban đầu (test kiểm tra khung cuối = khung đầu). `tools/preview_weather_fx.py` mô phỏng hiệu ứng bằng SVG để xem trước trên máy (không phải bộ vẽ của Android); hiệu năng vẽ khoảng 140 path (37 giọt, 13 vệt, hạt li ti) chưa đo trên máy thật. Lớp này được đệm 0,3 lần độ bo góc để không tràn ra ngoài mép bo.
- **Phương án thay thế:** nếu launcher không chạy hoạt ảnh (hoặc người dùng tắt, bật tiết kiệm pin, tắt hoạt ảnh hệ thống), widget vẫn hiện nền bầu trời tĩnh như trước, không lỗi.
- **Chưa kiểm chứng trên máy thật:** hiệu ứng có chạy lại mỗi lần quay về màn hình chính trên launcher Xiaomi hay không (đã thấy chạy được một lần), và hình dáng hiệu ứng trên các cỡ widget khác nhau. Đổi chế độ tiết kiệm pin chỉ có hiệu lực ở lần cập nhật widget kế tiếp.
