# Widget-Pro

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

## Quyền riêng tư

Mục tiêu thiết kế: không tài khoản, không quảng cáo, không thu thập dữ liệu. Ghi chú và cài đặt lưu trên máy. Chỉ phần thời tiết cần gửi vị trí hoặc tên thành phố tới dịch vụ thời tiết bạn chọn, và sẽ được nêu rõ trong ứng dụng.

## Lộ trình

1. Khung dự án Android (Kotlin + Jetpack Compose + Glance) và build bằng GitHub Actions.
2. Widget đồng hồ, pin, RAM, bộ nhớ.
3. Widget thời tiết.
4. Tab Lịch Việt: lịch âm, ghi chú, nhắc việc.
5. Hai giao diện Chuẩn và Kính mờ, tùy chỉnh màu.
6. Kiểm thử và phát hành bản APK.

## Giấy phép

MIT, xem [LICENSE](LICENSE).
