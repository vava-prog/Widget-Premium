# Widget Premium

Ứng dụng widget Android gom những thứ bạn nhìn mỗi ngày vào một màn hình chính gọn gàng: giờ, ngày âm dương, thời tiết và tình trạng máy.

> **Trạng thái: giai đoạn 3b.** Đã có widget đồng hồ + ngày và widget thông tin (lịch âm, thời tiết, pin, nhiệt độ, RAM, bộ nhớ). Thời tiết dùng Open-Meteo, chọn thành phố trong app; dự báo theo giờ chưa làm. Các tính năng còn lại bên dưới là dự kiến, chưa hoàn thành. Xem [kiến trúc](docs/ARCHITECTURE.md) và [danh sách quyền](docs/PERMISSIONS.md).

**Tải APK thử nghiệm (bản debug mới nhất):** https://github.com/vava-prog/Widget-Premium/releases/tag/latest-debug

## Tính năng dự kiến

### Widget trên màn hình chính
- **Đồng hồ lớn** dễ đọc từ xa, bật/tắt giây, chọn kiểu 12 hoặc 24 giờ.
- **Ngày dương và âm lịch** trên cùng một widget, kèm dải 7 ngày.
- **Thời tiết** theo vị trí hoặc thành phố bạn chọn: hiện tại, theo giờ và 7 ngày tới.
- **Tình trạng máy:** mức pin và trạng thái sạc, nhiệt độ, RAM, dung lượng lưu trữ.
- Cảnh báo nhẹ khi pin yếu hoặc máy nóng.
- Kéo to nhỏ widget thì chữ và bố cục tự co giãn. Chạm vào từng phần để mở ứng dụng liên quan. Chạm dòng thời tiết để tải lại ngay (cần mạng; chưa chọn thành phố thì không làm gì). Có thêm widget "Lịch tuần" 4×2: giờ, ngày âm dương, thời tiết và dải bảy ngày kèm ngày âm.

### Lịch Việt trong ứng dụng
- Xem ngày âm, can chi, tiết khí.
- Ghi chú theo ngày và nhắc việc.
- Lịch âm được tính trực tiếp trên máy bằng thuật toán, không cần mạng.

### Giao diện
- **Chuẩn:** sạch, đơn giản, tự chuyển sáng/tối theo hệ thống.
- **Kính mờ:** nền trong suốt, bo góc mềm.
- **Hiển thị:** bật/tắt ngày dương, âm lịch, thời tiết, chạm mở app; kiểu giờ hệ thống/24/12; dải tuần hiện ngày âm, thời tiết 7 ngày hoặc dự báo 7 giờ tới (giờ, biểu tượng ngày/đêm, nhiệt độ).
- **Chi tiết thời tiết:** dòng thấp/cao và khả năng mưa trên widget thông tin (tùy chọn, mặc định tắt); màu nổi cho hôm nay trên dải tuần.
- **Số liệu hiện tại:** thẻ ở trang chủ hiện pin, nhiệt độ, RAM, bộ nhớ, lần cập nhật widget và thời tiết, tình trạng quyền nhắc việc (thông báo, báo thức chính xác, hạn chế pin).
- **Kính mờ:** chỉnh độ mờ (lớp sương) và độ nổi khối (viền, mép sáng).
- **Màu chữ riêng từng giao diện:** Standard, Kính mờ và Theo thời tiết mỗi loại nhớ màu chữ của mình.
- **Vị trí:** nút "Dùng vị trí của máy" (xin quyền khi bấm, lấy một lần, không chạy nền).
- **Theo thời tiết:** nền là bầu trời vẽ theo dữ liệu thời tiết đã lưu (trời xanh, mây, sương mù, mưa, tuyết, giông; ban đêm có sao và trăng). Chưa có dữ liệu thì dùng nền Standard.
- **Hiệu ứng thời tiết bán động:** trên giao diện Theo thời tiết, mưa là cửa kính ướt (hậu cảnh nhòe cây xanh, toà nhà, ánh đèn ấm; thẻ kính viền sáng; hạt nước li ti, giọt hình giọt nước rơi nhanh dần như có trọng lực và để lại vệt nước, giọt mới ngưng tụ ngẫu nhiên và trải đều) chạy một lượt đúng 4 giây rồi đứng yên mỗi khi màn hình chính hiện ra hoặc widget được cập nhật; nắng (tia sáng), sao, mây, tuyết và sương chạy khoảng 1 phút (nhiều lớp chuyển động lệch nhịp cho tự nhiên). Widget Lịch tuần (chế độ theo giờ) có thẻ thời tiết bo tròn, dải 7 giờ trong khung kính có vạch ngăn và icon lịch cạnh ngày âm. Có công tắc bật/tắt; tự tắt khi bật tiết kiệm pin hoặc tắt hoạt ảnh hệ thống. Cách làm và giới hạn: xem `docs/ARCHITECTURE.md`.
- **Tùy chỉnh:** độ đậm nền, bo góc, viền (0 = tự động), cỡ đồng hồ (60–130%), hiện giây, tên tự nhập, màu chữ (mặc định, trắng, tối hoặc tùy chọn), nút "Đặt lại giao diện".
- Tùy chỉnh màu, biểu tượng và độ trong suốt.

## Công nghệ

Kotlin, Gradle, Material Design 3, Jetpack Glance (hoặc AppWidget API khi cần), Room (ghi chú, nhắc việc), WorkManager (làm mới định kỳ), AlarmManager (nhắc việc đúng giờ), Coroutines, MVVM, Open-Meteo (thời tiết). Mã chia module để dễ bảo trì và kiểm thử. Hỗ trợ Android 8.0 trở lên.

## Quyền riêng tư và hiệu năng

Mục tiêu thiết kế:
- Không tài khoản, không quảng cáo, không thu thập dữ liệu, không cần máy chủ cấp phép, không cần root.
- Ghi chú và cài đặt lưu trên máy.
- **Không dùng quyền vị trí nền.** Người dùng nhập địa điểm thủ công, hoặc dùng vị trí thô khi đang mở ứng dụng. Chỉ phần thời tiết cần gửi tọa độ hoặc tên thành phố tới Open-Meteo, và được nêu rõ trong ứng dụng.
- Không chạy dịch vụ nền liên tục nếu không cần.
- Mất mạng thì hiện dữ liệu đã lưu kèm thời điểm cập nhật, không bịa số liệu. Cảm biến không hỗ trợ (ví dụ nhiệt độ pin) thì ẩn mục đó, không hiện giá trị giả.

### Giới hạn của Android cần lưu ý
- Widget không thể cập nhật từng giây bằng mã tự chạy. Đồng hồ dùng thành phần đồng hồ của hệ thống, còn số liệu pin, RAM, bộ nhớ được làm mới theo lịch, theo sự kiện hệ thống cho phép và khi người dùng chạm làm mới.
- Từ Android 8.0, nhiều sự kiện hệ thống không đăng ký trong manifest được. Cần đăng ký động hoặc dùng WorkManager (khoảng cách tối thiểu 15 phút).
- Nhắc việc đúng giờ cần quyền báo thức chính xác trên Android 12+, và nhiều hãng máy giới hạn chạy nền nên cần hướng dẫn người dùng.


## Lộ trình (mỗi giai đoạn phải build xanh và kiểm tra rồi mới sang giai đoạn sau)

1. **Phân tích và thiết kế:** kiến trúc, màn hình, danh sách quyền.
2. **Nền ứng dụng và hệ thống widget:** khung dự án, CI, widget đồng hồ.
3. **Dữ liệu:** lịch Âm – Dương, thời tiết, pin, nhiệt độ, RAM, bộ nhớ.
4. **Ghi chú và nhắc việc:** Room, thông báo đúng giờ.
5. **Giao diện (đã làm):** hai phong cách Standard và Kính mờ (Liquid Glass) tự vẽ theo kích thước widget, chọn màu chủ đạo, độ đậm của nền, chế độ sáng/tối (tự động theo mặt trời hoặc ép cố định). Chọn trong màn hình Trang chủ, áp dụng ngay. Khung xem trước được ghim phía trên khi cuộn các thanh chỉnh và là chính widget thật (cùng layout, cùng dữ liệu: ngày âm, thời tiết, dải theo giờ hoặc theo tuần, pin/RAM/bộ nhớ, hiệu ứng mưa), chọn xem Đồng hồ, Thông tin hoặc Lịch tuần; chạm vào khung không kích hoạt gì.
6. **Kiểm thử, tối ưu hiệu năng, sửa lỗi.**
7. **Đóng gói (đã làm):** quy trình phát hành APK có chữ ký chạy tay ([docs/RELEASE.md](docs/RELEASE.md)), README, tài liệu kiến trúc và quyền, hướng dẫn thêm widget.

## Cài đặt
1. Mở trang [Releases](../../releases), tải file `.apk` mới nhất (bản `latest-debug` là bản thử nghiệm, bản có số phiên bản là bản có chữ ký).
2. Mở file trên điện thoại, cho phép cài từ nguồn này nếu Android hỏi.
3. Mở Widget Premium, bấm "Thêm widget đồng hồ" hoặc "Thêm widget thông tin", hoặc nhấn giữ màn hình chính > Widget > Widget Premium.
4. Vào mục thời tiết để nhập thành phố, vào "Giao diện widget" để chọn phong cách và màu.
5. Nhắc việc: máy Xiaomi, Oppo, Vivo, Huawei... hay chặn chạy nền; nếu nhắc không hiện, bật tự khởi động và đặt tiết kiệm pin "Không hạn chế" cho ứng dụng.

## Giấy phép

MIT, xem [LICENSE](LICENSE).
