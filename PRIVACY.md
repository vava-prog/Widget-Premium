# Chính sách quyền riêng tư / Privacy Policy

**Tóm tắt: ứng dụng không thu thập dữ liệu người dùng và không có máy chủ của dự án.**

- Không tài khoản, không quảng cáo, không phân tích, không báo cáo lỗi tự động.
- Cài đặt, thành phố đã chọn và bản dự báo gần nhất chỉ lưu trên máy bạn. Sao lưu đám mây của Android bị tắt (`allowBackup=false`).
- Lịch Âm – Dương, thông số pin, nhiệt độ pin, RAM và bộ nhớ được tính hoặc đọc ngay trên máy, không gửi đi đâu.
- **Thời tiết là tính năng duy nhất dùng mạng.** Khi bạn tìm thành phố, tên bạn gõ được gửi tới `geocoding-api.open-meteo.com`.
  Khi lấy dự báo, tọa độ của thành phố đã chọn được gửi tới `api.open-meteo.com`. Cả hai qua HTTPS.
  Open-Meteo có thể ghi nhận địa chỉ IP của bạn theo chính sách của họ. Ứng dụng không gửi gì khác.
- Vị trí là tùy chọn. Mặc định bạn tự chọn thành phố. Chỉ khi bạn bấm "Dùng vị trí của máy", ứng dụng xin quyền vị trí gần đúng (`ACCESS_COARSE_LOCATION`), lấy vị trí một lần, làm tròn khoảng 1 km rồi dùng làm tọa độ dự báo (gửi tới Open-Meteo như tọa độ thành phố). Không theo dõi vị trí, không lấy vị trí nền, không dùng GPS chính xác. Tên nơi được dịch bằng bộ dịch tọa độ của hệ thống nếu máy có. Từ chối quyền thì mọi thứ vẫn dùng được.
- Quyền `INTERNET` chỉ phục vụ việc trên.

**Summary: this app does not collect user data and runs no project servers.** Weather is the only feature that uses the
network: the city name you search for and the chosen city's coordinates are sent over HTTPS to Open-Meteo. The optional "Use my device location" button asks for approximate location only when tapped, reads it once (rounded to about 1 km), and never tracks or runs in the background.

Dữ liệu thời tiết: [Open-Meteo.com](https://open-meteo.com/) (giấy phép CC BY 4.0).

## Ghi chú và nhắc việc
Ghi chú, việc cần làm và giờ nhắc được lưu trong cơ sở dữ liệu trên máy (Room), không gửi đi đâu, không sao lưu lên đám mây (`allowBackup=false`). Thông báo nhắc do chính ứng dụng tạo trên máy. Xóa dữ liệu ứng dụng hoặc gỡ cài đặt sẽ xóa toàn bộ ghi chú.
