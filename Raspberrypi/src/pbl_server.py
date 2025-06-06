import time
import spidev
import joblib
import json
import socket
import sys
import firebase_admin
from firebase_admin import credentials, db
import netifaces
import numpy as np
import threading

# ----------- Khởi tạo SPI MCP3008 -----------
try:
    spi = spidev.SpiDev()
    spi.open(0, 0)
    spi.max_speed_hz = 1000000
except Exception as e:
    print("Lỗi khởi tạo SPI:", e)
    sys.exit(1)

def read_channel(channel):
    if 0 <= channel <= 7:
        adc = spi.xfer2([1, (8 + channel) << 4, 0])
        return ((adc[1] & 3) << 8) + adc[2]
    return -1

def normalize_and_clamp(values):
    norm_values = [round(val / 1023, 4) for val in values]
    clamped_values = []
    for val in norm_values:
        if val < 0.003:
            clamped_values.append(0)
        else:
            clamped_values.append(val)
    return clamped_values

# ----------- Load model & scaler -----------
try:
    model = joblib.load('/home/pi/PBL/models/sensor_model.pkl')
    scaler = joblib.load('/home/pi/PBL/models/scaler.pkl')
except Exception as e:
    print("Lỗi khi load model/scaler:", e)
    sys.exit(1)

# ----------- Load label map -----------
try:
    with open('/home/pi/PBL/data/label_map.json', 'r', encoding='utf-8') as f:
        label_map = json.load(f)
    label_map = {int(k): v for k, v in label_map.items()}
except Exception as e:
    print("Lỗi khi đọc label_map:", e)
    sys.exit(1)

# ----------- Firebase Admin SDK -----------
try:
    cred = credentials.Certificate('/home/pi/serviceAccountKey.json')
    firebase_admin.initialize_app(cred, {
        'databaseURL': 'https://pbl5-9f417-default-rtdb.firebaseio.com/'
    })
except Exception as e:
    print("Lỗi khởi tạo Firebase:", e)
    sys.exit(1)

def get_local_ip():
    try:
        interfaces = ['wlan0', 'eth0']
        for iface in interfaces:
            addrs = netifaces.ifaddresses(iface)
            if netifaces.AF_INET in addrs:
                ip = addrs[netifaces.AF_INET][0]['addr']
                if ip != '127.0.0.1':
                    return ip
    except Exception as e:
        print("Lỗi lấy IP:", e)
    try:
        hostname = socket.gethostname()
        ip = socket.gethostbyname(hostname)
        return ip
    except:
        return '127.0.0.1'

def update_ip_firebase():
    ref = db.reference('raspberry_pi')
    while True:
        ip = get_local_ip()
        try:
            print("[INFO] Gửi IP lên Firebase:", ip)
            ref.update({'ip': ip})
            print("[SUCCESS] Cập nhật IP thành công")
        except Exception as e:
            print("[ERROR] Không thể cập nhật IP:", e)
        time.sleep(60)

def predict_with_min_max_check(raw_data, scaler, model, label_map, debug=False):
    norm_data = normalize_and_clamp(raw_data)

    scaled_input = scaler.transform([norm_data])
    try:
        proba = model.predict_proba(scaled_input)[0]
        prediction_num = np.argmax(proba)
        confidence = round(proba[prediction_num] * 100, 2)
        prediction_label = label_map.get(prediction_num, "Không xác định")
    except Exception as e:
        if debug:
            print("[ERROR] Lỗi dự đoán:", e)
        return "Không xác định", 0.0

    if debug:
        print(f"[DEBUG] Raw: {raw_data} -> Normalized: {norm_data} -> Label: {prediction_label} ({confidence}%)")

    return prediction_label, confidence

def update_text_firebase():
    text_ref = db.reference('raspberry_pi/text')
    last_prediction = None

    while True:
        try:
            raw_data = [read_channel(i) for i in range(5)]
            if any(val == -1 for val in raw_data):
                print("[ERROR] Dữ liệu cảm biến không hợp lệ:", raw_data)
                time.sleep(2.5)
                continue

            message, confidence = predict_with_min_max_check(raw_data, scaler, model, label_map, debug=True)

            if message != "Không xác định" and message != last_prediction and confidence >= 70:
                text_ref.set(message)
                last_prediction = message
                print("[SUCCESS] Cập nhật text lên Firebase:", message, f"({confidence}%)")
            else:
                print(f"[INFO] Không cập nhật Firebase. Label: {message} | Confidence: {confidence}%")

        except Exception as e:
            print("[ERROR] Không thể cập nhật text:", e)
        time.sleep(2.5)

if __name__ == "__main__":
    try:
        threading.Thread(target=update_ip_firebase, daemon=True).start()
        update_text_firebase()
    except KeyboardInterrupt:
        print("\n[INFO] Đã dừng chương trình.")
    except Exception as e:
        print("[ERROR] Lỗi chương trình:", e)
    finally:
        spi.close()
