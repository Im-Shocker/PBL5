import time
import spidev
import joblib
import json
import socket
import sys
import firebase_admin
from firebase_admin import credentials, db
import netifaces

# ----------- Khởi tạo SPI MCP3008 -----------  
spi = spidev.SpiDev()
spi.open(0, 0)
spi.max_speed_hz = 1000000

def read_channel(channel):
    if 0 <= channel <= 7:
        adc = spi.xfer2([1, (8 + channel) << 4, 0])
        return ((adc[1] & 3) << 8) + adc[2]
    return -1

def normalize(values):
    return [round(val / 1023, 4) for val in values]

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
cred = credentials.Certificate('/home/pi/serviceAccountKey.json')
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://pbl5-9f417-default-rtdb.firebaseio.com/'
})

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

def predict(debug=False):
    raw_data = [read_channel(i) for i in range(5)]
    norm_data = normalize(raw_data)
    scaled_input = scaler.transform([norm_data])
    prediction_num = model.predict(scaled_input)[0]
    prediction_label = label_map.get(prediction_num, "Không xác định")
    if debug:
        print("[DEBUG] Raw:", raw_data, "-> Normalized:", norm_data, "-> Label:", prediction_label)
    return prediction_label

def update_text_firebase():
    text_ref = db.reference('raspberry_pi/text')
    while True:
        try:
            message = predict(debug=True)
            text_ref.set(message)
            print("[SUCCESS] Cập nhật text lên Firebase:", message)
        except Exception as e:
            print("[ERROR] Không thể cập nhật text:", e)
        time.sleep(2)

if __name__ == "__main__":
    try:
        # Chạy thread cập nhật IP
        import threading
        threading.Thread(target=update_ip_firebase, daemon=True).start()

        # Chạy thread cập nhật text
        update_text_firebase()
    except KeyboardInterrupt:
        print("\n[INFO] Đã dừng chương trình.")
    finally:
        spi.close()
