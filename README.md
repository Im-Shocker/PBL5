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
