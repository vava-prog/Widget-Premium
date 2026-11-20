# Widget Premium

Ứng dụng widget Android gom những thứ bạn nhìn mỗi ngày vào một màn hình chính gọn gàng: giờ, ngày âm dương, thời tiết và tình trạng máy.

> **Trạng thái: đang lên kế hoạch.** Repo hiện chưa có mã nguồn. Các mục dưới đây là tính năng dự kiến, chưa phải tính năng đã hoàn thành.

## Tính năng dự kiến

### Widget trên màn hình chính
- **Đồng hồ lớn** dễ đọc từ xa, bật/tắt giây, chọn kiểu 12 hoặc 24 giờ.
- **Ngày dương và âm lịch** trên cùng một widget, kèm dải 7 ngày.
- **Thời tiết** theo vị trí hoặc thành phố bạn chọn: hiện tại, theo giờ và 7 ngày tới.
- **Tình trạng máy:** mức pin và trạng thái sạc, nhiệt độ, RAM, dung lượng lưu trữ.
- Cảnh báo nhẹ khi pin yếu hoặc máy nóng.
- Kéo to nhỏ widget thì chữ và bố cục tự co giãn. Chạm vào từng phần để mở ứng dụng liên quan.

### Lịch Việt trong ứng dụng
- Xem ngày âm, can chi, tiết khí.
- Ghi chú theo ngày và nhắc việc.
- Lịch âm được tính trực tiếp trên máy bằng thuật toán, không cần mạng.

### Giao diện
- **Chuẩn:** sạch, đơn giản, tự chuyển sáng/tối theo hệ thống.
- **Kính mờ:** nền trong suốt, bo góc mềm.
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
5. **Giao diện:** hai phong cách Standard và Kính mờ (Liquid Glass), tùy chỉnh màu và độ trong suốt, tự chuyển sáng/tối.
6. **Kiểm thử, tối ưu hiệu năng, sửa lỗi.**
7. **Đóng gói:** APK, README, tài liệu kiến trúc và quyền, hướng dẫn thêm widget.

## Giấy phép

MIT, xem [LICENSE](LICENSE).
