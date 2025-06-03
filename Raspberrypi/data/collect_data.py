import spidev
import time
import csv
import os

# Khởi tạo SPI
spi = spidev.SpiDev()
spi.open(0, 0)
spi.max_speed_hz = 1000000

# Hàm đọc 1 kênh MCP3008 (0-7)
def read_channel(channel):
    if channel < 0 or channel > 7:
        return -1
    adc = spi.xfer2([1, (8 + channel) << 4, 0])
    data = ((adc[1] & 3) << 8) + adc[2]
    return data

# Hàm chuẩn hóa giá trị về 0-1 (giả định max 1023)
def normalize(values):
    return [round(val / 1023, 4) for val in values]

# Hàm lưu vào CSV
def save_to_csv(filename, data_row):
    header = ['sensor_1', 'sensor_2', 'sensor_3', 'sensor_4', 'sensor_5', 'label']
    file_exists = os.path.isfile(filename)
    with open(filename, mode='a', newline='') as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow(header)
        writer.writerow(data_row)

# Main
if __name__ == "__main__":
    filename = 'data/sensor_data.csv'
    os.makedirs('data', exist_ok=True)

    print("=== THU THẬP DỮ LIỆU ===")
    label = input("Nhập nhãn (ví dụ: A, B, G, H): ").strip().upper()

    print(f"Bắt đầu thu thập 100 mẫu cho nhãn: {label} ...")

    try:
        for i in range(100):
            raw = [read_channel(i) for i in range(5)]
            norm = normalize(raw)
            data_row = norm + [label]
            print(f"Mẫu {i+1}/100: {data_row}")
            save_to_csv(filename, data_row)
            time.sleep(0.3)  # Thời gian chờ để ổn định tín hiệu

        print("✅ Đã thu thập xong 50 mẫu.")

    except Exception as e:
        print(f"Lỗi trong quá trình thu thập: {e}")

    finally:
        spi.close()