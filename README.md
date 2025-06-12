1.Phần cứng
  - Sơ đồ lắp mạch 
  ![image](https://github.com/user-attachments/assets/8a2ae072-0843-43e0-b7d2-65ec83f7e347)
  - Cấu hình wifi : -	Cập nhật ssid và password trong code để phù hợp với mạng WiFi cục bộ.
  - Cài đặt môi trường lập trình: Cài đặt các thư viện: spidev, joblib, xgboost, firebase-admin, netifaces.
  - Kiểm tra phần cứng :
    *Lưu ý :  •	Nguồn điện: Sử dụng nguồn 5V ổn định, tránh nhiễu từ servo hoặc quạt.
              •	Kết nối mạng: đảm bảo wifi ổn định.
              •	Bảo trì: bảo đảm kiểm tra định kỳ kết nối chân, cảm biến và thiết bị tránh hư hỏng.
Thu thập dữ liệu :
    - Chạy  file "collect_data.py" và thu thập dữ liệu của ký hiệu từ găng tay.
    - Dữ liệu thu thập được lưu vào "sensor_data.cvs"
    - "label_map.json" đổi các ký hiệu thành các chữ , từ .
Xử lý dữ liệu và huấn luyện mô hình ( bằng XGBoost - Extreme Gradient Boosting)
    - chạy file "train_model.py" để xử lý dữ liệu và huấn luyện mô hình"
    - 
2.Phần mềm.
    - Ứng dụng Android "AppAdroid"
    - Mục tiêu của ứng dụng :
      •	  Kết nối với server qua HTTPS (REST API) để nhận dữ liệu ký hiệu.
      •	  Hiển thị nội dung ký hiệu dưới dạng văn bản trên giao diện người dùng.
      •	  Đọc to nội dung ký hiệu bằng tiếng Việt để người nghe có thể hiểu.
      •	  Giao diện thân thiện, dễ sử dụng cho cả người bình thường và người khiếm thính/ngôn.

    - Khởi tạo dự án :
        Tạo project với cấu trúc chuẩn theo mô hình MVVM.
        Cấu hình minSdk và targetSdk phù hợp với thiết bị sử dụng thực tế (API 30 trở lên).
        Cài đặt các thư viện cần thiết.
    -Thiết kế giao diện : 	Sử dụng ConstraintLayout để tạo giao diện linh hoạt.
    -Cấu hình và sử dụng HTTPS (REST API):
        Sử dụng thư viện firebase_admin để thiết lập và duy trì kết nối HTTPS (REST API).
        Dữ liệu ký hiệu được nhận từ server dưới dạng chuỗi JSON, sau đó được giải mã bằng Gson.
    -Xử lý dữ liệu và đọc văn bản thành giọng nói:
        Sử dụng TextToSpeech để chuyển nội dung nhận dạng sang giọng nói tiếng Việt.


        
