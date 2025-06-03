import spidev
import time
import joblib
import numpy as np
import os

# === Khởi tạo SPI cho MCP3008 ===
spi = spidev.SpiDev()
spi.open(0, 0)
spi.max_speed_hz = 1000000

# === Hàm đọc giá trị từ MCP3008 ===
def read_channel(channel):
    if channel < 0 or channel > 7:
        return -1
    adc = spi.xfer2([1, (8 + channel) << 4, 0])
    data = ((adc[1] & 3) << 8) + adc[2]
    return data

# === Hàm chuẩn hóa giá trị về 0-1 ===
def normalize(values):
    return [round(val / 1023, 4) for val in values]

# === Main ===
if __name__ == "__main__":
    print("=== DỰ ĐOÁN NHÃN TỪ CẢM BIẾN ===")

    try:
        # Load mô hình và scaler đã lưu
        model = joblib.load('models/sensor_model.pkl')
        scaler = joblib.load('models/scaler.pkl')

        while True:
            # Đọc 5 giá trị từ 5 kênh cảm biến
            raw_data = [read_channel(i) for i in range(5)]
            norm_data = normalize(raw_data)
            print(f"Giá trị cảm biến (chuẩn hóa): {norm_data}")

            # Chuẩn hóa theo scaler (dạng [1, 5] → [1, 5] → (1,5))
            scaled_input = scaler.transform([norm_data])

            # Dự đoán nhãn
            prediction = model.predict(scaled_input)
            print(f"🔮 Dự đoán: {prediction[0]}")
            print("-------------------------------")

            time.sleep(1)

    except KeyboardInterrupt:
        print("\n⛔ Dừng dự đoán thủ công.")

    except Exception as e:
        print(f"❌ Lỗi: {e}")

    finally:
        spi.close()
