# Widget Premium: hướng dẫn làm việc cho Claude

Ứng dụng widget Android (Kotlin + Jetpack Compose + Glance): đồng hồ, lịch âm dương, thời tiết, pin/RAM/bộ nhớ, tab Lịch Việt.
Mô tả đầy đủ và lộ trình nằm trong `README.md`. Người dùng nói tiếng Việt: trả lời bằng tiếng Việt, ngắn gọn.

## Nguyên tắc chung
- Làm từng bước theo lộ trình trong README. Mỗi bước phải build xanh trước khi sang bước sau.
- Việc rõ ràng thì làm luôn, không hỏi lại. Chỉ hỏi khi một quyết định tốn kém để làm lại hoặc không hoàn tác được.
- Không sao chép mã, hình, chữ hay giá từ ứng dụng của bên khác. Chỉ lấy ý tưởng chung và tự viết, tự thiết kế.
- Không thu thập dữ liệu người dùng. Tính năng cần mạng (thời tiết) phải nói rõ trong app và `PRIVACY.md`.
- Giấy phép dự án: MIT (xem `LICENSE`). Thư viện bên thứ ba ghi trong `LICENSE-NOTICES.md`.

## Yêu cầu kỹ thuật cố định
- Kotlin, Material 3, Glance hoặc AppWidget API, Room, WorkManager, AlarmManager, Coroutines, MVVM, Open-Meteo. Mã chia module.
- Không quyền vị trí nền, không root, không máy chủ cấp phép, không dịch vụ nền liên tục không cần thiết.
- Địa điểm thời tiết nhập tay được. Mất mạng thì dùng dữ liệu đã lưu. Cảm biến không có thì ẩn, không hiện số giả.
- Không làm giao diện minh họa: mỗi chức năng phải chạy thật. Không bỏ qua lỗi build hay runtime.
- Giới hạn của Android (cập nhật widget, chạy nền) phải được giải thích và có phương án thay thế.
- Tài liệu tham khảo bên ngoài chỉ dùng để hiểu chức năng và trải nghiệm. Không dịch ngược, không dùng tài nguyên, khóa hay cơ chế máy chủ của ứng dụng khác.

## Quy trình mỗi lần thay đổi
1. Đọc mã liên quan trước khi sửa. Sửa nhỏ, đúng chỗ.
2. Chuỗi giao diện: thêm vào cả `values/strings.xml` (tiếng Việt) và `values-en/strings.xml` (tiếng Anh). Chạy script kiểm tra: hai file đủ khóa như nhau, không khóa thừa, không `R.string.x` thiếu. Dấu nháy đơn trong chuỗi tiếng Anh phải viết `\'`.
3. Commit rõ nghĩa, rồi: `git fetch origin main && git push origin HEAD:main`.
4. Chờ CI và đọc kết quả, không đoán:
   - Theo dõi: `gh run list --repo <chủ>/<repo> --limit 1 --json databaseId,status,conclusion,headSha`
   - Đợi tối đa ~9 phút mỗi lần gọi (lệnh quá 10 phút sẽ bị ngắt). Hết thì kiểm tra lại, đừng chạy lại cả quy trình.
   - Lỗi: `gh run view <id> --log-failed | grep -E "e: |FAILED|AssertionError"`
5. Sửa lỗi gốc rồi push lại. Chỉ báo "xong" khi CI xanh ở đúng commit cuối.

## Ràng buộc môi trường
- Không biên dịch Kotlin trên máy được: CI GitHub Actions là trình biên dịch. Vì vậy hãy viết Kotlin cẩn thận, tránh các lỗi phổ biến:
  - Thuộc tính `foo` có `private set` cùng hàm `setFoo` sẽ trùng chữ ký JVM: đặt tên `selectX`, `changeX`, `updateX`.
  - Đặt `private set` ngay dưới đúng thuộc tính của nó khi chèn thêm mã vào giữa.
  - `assertEquals` giữa Long và Double cần ép kiểu hoặc dùng cùng kiểu.
- Bash không có mạng để cài gói (pip, npm bị chặn). Dùng những gì đã có.
- `gh release ...` và các lệnh GraphQL bị chặn: dùng `gh api` REST (ví dụ `repos/<chủ>/<repo>/releases/tags/latest-debug`).
- Không tải được artifact CI từ shell: cung cấp link trang Releases cho người dùng.
- Đo hiệu năng và hành vi thật phải thử trên điện thoại. Hãy nói rõ phần nào chưa kiểm chứng trên máy thật.

## Tối ưu thời gian chạy
- Gom nhiều thay đổi liên quan vào một commit để một lần CI kiểm tra tất cả, nhưng không gộp việc không liên quan.
- Chạy song song các lệnh độc lập (đọc file, kiểm tra chuỗi) trong cùng một lượt.
- Viết unit test cho logic thuần (lịch âm, định dạng, trạng thái dùng thử) để CI bắt lỗi sớm, không cần chạy app.
- Giữ workflow build: `build` (APK debug, đẩy lên pre-release `latest-debug`) và `test` chạy song song. Bản phát hành có chữ ký chỉ chạy tay, dùng secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Bật cache Gradle bằng `gradle/actions/setup-gradle`. Không thêm bước không cần thiết.

## Kết thúc mỗi bước
Báo ngắn: đã làm gì, commit nào, CI xanh chưa, phần nào chưa thử trên máy thật, và một bước tiếp theo hợp lý.

## Gói dùng thử/trả phí (nếu làm)
Làm sẵn nhưng tắt bằng một công tắc hằng số (`ENABLED = false`), logic thuần có test, không xử lý giao dịch. Chỉ bật khi đã nối thanh toán thật.
