import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, GridSearchCV, cross_val_score
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import confusion_matrix, classification_report
from sklearn.metrics.pairwise import euclidean_distances
from imblearn.over_sampling import SMOTE
from xgboost import XGBClassifier
import joblib
import os

# Hàm đọc và tiền xử lý dữ liệu
def load_and_preprocess_data(data):
    if data.empty:
        raise ValueError("Dữ liệu rỗng hoặc không hợp lệ!")
    
    # Tách đặc trưng và nhãn
    X = data[['sensor_1', 'sensor_2', 'sensor_3', 'sensor_4', 'sensor_5']]
    y = data['label']
    
    # Chuẩn hóa dữ liệu
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    # Lọc các mẫu lạc loài trong từng nhãn
    X_filtered, y_filtered = remove_outliers_per_class(X_scaled, y, threshold=1.5)
    
    # Xử lý mất cân bằng lớp bằng SMOTE
    smote = SMOTE(random_state=42, k_neighbors=5)
    X_resampled, y_resampled = smote.fit_resample(X_filtered, y_filtered)
    
    return X_resampled, y_resampled, scaler


def remove_outliers_per_class(X_scaled, y, threshold=1.5):
    X_clean = []
    y_clean = []
    labels = np.unique(y)

    for label in labels:
        # Lấy các mẫu thuộc nhãn này
        indices = np.where(y == label)[0]
        X_label = X_scaled[indices]
        
        # Tính ma trận khoảng cách Euclid giữa các mẫu trong nhãn
        dists = euclidean_distances(X_label)
        
        # Tính khoảng cách trung bình cho mỗi mẫu
        avg_dists = dists.mean(axis=1)
        
        # Ngưỡng loại bỏ
        mean_dist = np.mean(avg_dists)
        std_dist = np.std(avg_dists)
        upper_limit = mean_dist + threshold * std_dist

        for i, idx in enumerate(indices):
            if avg_dists[i] <= upper_limit:
                X_clean.append(X_scaled[idx])
                y_clean.append(y[idx])
    
    return np.array(X_clean), np.array(y_clean)


# Hàm huấn luyện mô hình
def train_model(X, y):
    # Chia dữ liệu
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    # Định nghĩa tham số cho GridSearchCV
    param_grid = {
        'n_estimators': [50, 100, 200],
        'max_depth': [3, 5, 7],
        'learning_rate': [0.01, 0.1, 0.3]
    }
    
    # Khởi tạo mô hình XGBoost
    model = XGBClassifier(random_state=42, eval_metric='mlogloss')
    
    # Tìm tham số tối ưu
    grid_search = GridSearchCV(model, param_grid, cv=5, scoring='f1_weighted', n_jobs=-1)
    grid_search.fit(X_train, y_train)
    
    # Lấy mô hình tốt nhất
    best_model = grid_search.best_estimator_
    print("Tham số tốt nhất:", grid_search.best_params_)
    
    # Dự đoán trên tập test
    y_pred = best_model.predict(X_test)
    
    # Đánh giá
    print("Ma trận nhầm lẫn:")
    print(confusion_matrix(y_test, y_pred))
    print("\nBáo cáo phân loại:")
    print(classification_report(y_test, y_pred, zero_division=0))
    
    # Kiểm định chéo
    scores = cross_val_score(best_model, X, y, cv=5, scoring='f1_weighted')
    print("F1-score trung bình (5-fold CV):", scores.mean())
    
    return best_model, scaler, X_test, y_test

# Hàm lưu mô hình và scaler
def save_model(model, scaler, model_filename, scaler_filename):
    os.makedirs('models', exist_ok=True)
    joblib.dump(model, model_filename)
    joblib.dump(scaler, scaler_filename)
    print(f"✅ Mô hình đã được lưu vào: {model_filename}")
    print(f"✅ Scaler đã được lưu vào: {scaler_filename}")

# Main
if __name__ == "__main__":
    model_file = 'models/sensor_model.pkl'
    scaler_file = 'models/scaler.pkl'
    print("=== HUẤN LUYỆN MÔ HÌNH PHÂN LOẠI ===")
    try:
        # Đọc dữ liệu
        data = pd.read_csv('data/sensor_data.csv')
        X, y, scaler = load_and_preprocess_data(data)
        model, scaler, X_test, y_test = train_model(X, y)
        save_model(model, scaler, model_file, scaler_file)
        print("✅ Huấn luyện hoàn tất.")
    except Exception as e:
        print(f"Lỗi trong quá trình huấn luyện: {e}")