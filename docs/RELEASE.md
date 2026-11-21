# Phát hành bản có chữ ký

Bản debug (pre-release `latest-debug`) chỉ để thử nghiệm. Bản phát hành chính thức phải ký bằng khóa của bạn. Khóa này là bí mật: mất khóa thì không cập nhật được app đã cài, lộ khóa thì người khác có thể giả mạo bản cập nhật.

## 1. Tạo keystore (làm một lần, trên máy của bạn)
```
keytool -genkeypair -v -keystore widget-premium.jks -alias widgetpremium \
  -keyalg RSA -keysize 4096 -validity 10000
```
Ghi nhớ mật khẩu. Sao lưu file `.jks` ở nơi an toàn, **không** đưa vào Git.

## 2. Thêm secrets vào GitHub
Vào Settings > Secrets and variables > Actions của repo, tạo 4 secrets:

| Tên | Giá trị |
|---|---|
| `KEYSTORE_BASE64` | nội dung file `.jks` mã hóa base64 (`base64 -w0 widget-premium.jks`) |
| `KEYSTORE_PASSWORD` | mật khẩu keystore |
| `KEY_ALIAS` | `widgetpremium` (hoặc alias bạn đã đặt) |
| `KEY_PASSWORD` | mật khẩu khóa |

## 3. Chạy phát hành
Actions > **Release APK (signed)** > Run workflow, nhập số phiên bản (ví dụ `0.7.0`). Quy trình sẽ kiểm tra đủ secrets, dựng bản release có R8, kiểm tra chữ ký bằng `apksigner`, rồi đăng lên Releases kèm `SHA256SUMS.txt`. Mã phiên bản (`versionCode`) lấy theo số lần chạy workflow nên luôn tăng.

## 4. Người dùng cài
Tải `Widget-Premium-<phiên bản>.apk` ở trang Releases, mở file trên điện thoại và cho phép cài từ nguồn này nếu Android hỏi. Đối chiếu `SHA256SUMS.txt` nếu muốn kiểm tra file không bị sửa.

Bản debug và bản release có chữ ký khác nhau, nên không cài đè lên nhau được: gỡ bản này trước khi cài bản kia (ghi chú trên máy sẽ mất).
