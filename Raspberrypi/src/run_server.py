import socket
import threading
import time
import spidev
import joblib
import json

# Khởi tạo SPI MCP3008
spi = spidev.SpiDev()
spi.open(0, 0)
spi.max_speed_hz = 1000000

def read_channel(channel):
    if channel < 0 or channel > 7:
        return -1
    adc = spi.xfer2([1, (8 + channel) << 4, 0])
    data = ((adc[1] & 3) << 8) + adc[2]
    return data

def normalize(values):
    return [round(val / 1023, 4) for val in values]

# Load model và scaler (đường dẫn đúng)
model = joblib.load('models/sensor_model.pkl')
scaler = joblib.load('models/scaler.pkl')

# Load label_map từ file JSON
with open('data/label_map.json', 'r', encoding='utf-8') as f:
    label_map = json.load(f)

# key trong file JSON là chuỗi, ta convert sang int nếu cần
label_map = {int(k): v for k, v in label_map.items()}

def predict(debug=False):
    raw_data = [read_channel(i) for i in range(5)]
    norm_data = normalize(raw_data)
    scaled_input = scaler.transform([norm_data])
    prediction_num = model.predict(scaled_input)[0]
    prediction_label = label_map.get(prediction_num, "Không xác định")
    if debug:
        print("Raw data:", raw_data)
        print("Normalized:", norm_data)
        print("Scaled input:", scaled_input)
        print("Predicted label:", prediction_label)
    return prediction_label


def tcp_server():
    server_ip = ''  # Bind mọi địa chỉ hoặc bạn có thể bind đúng IP Pi nếu muốn
    server_port = 12345

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((server_ip, server_port))
    server.listen(1)
    print(f"TCP Server listening on port {server_port}")

    while True:
        client, addr = server.accept()
        print(f"Client connected from {addr}")
        try:
            while True:
                message = str(predict(debug=True)) + "\n"
                client.send(message.encode())
                time.sleep(2)
        except Exception as e:
            print("Client disconnected:", e)
            client.close()

if __name__ == "__main__":
    try:
        tcp_server()
    finally:
        spi.close()
