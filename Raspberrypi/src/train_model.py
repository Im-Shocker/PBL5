import pandas as pd
import numpy as np
import json
from sklearn.model_selection import train_test_split, GridSearchCV
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import confusion_matrix, classification_report
from sklearn.metrics.pairwise import euclidean_distances
from imblearn.over_sampling import SMOTE
from xgboost import XGBClassifier
import joblib
import os
import warnings

warnings.filterwarnings("ignore")

# Hàm đọc và tiền xử lý dữ liệu
def load_and_preprocess_data(data_path, label_map_path):
    data = pd.read_csv(data_path)
    if data.empty:
        raise ValueError("Dữ liệu rỗng hoặc không hợp lệ!")

    X = data[['sensor_1', 'sensor_2', 'sensor_3', 'sensor_4', 'sensor_5']].values
    y = data['label'].values

    # Shuffle trước khi chuẩn hóa và SMOTE
    indices = np.arange(len(y))
    np.random.shuffle(indices)
    X = X[indices]
    y = y[indices]

    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    # ✨ Giới hạn giá trị tối đa mỗi cảm biến về 0.0098 sau chuẩn hóa
    X_scaled = np.clip(X_scaled, None, 0.0098)

    # Loại bỏ outlier từng lớp
    X_filtered, y_filtered = remove_outliers_per_class(X_scaled, y, threshold=1.5)

    # Cân bằng dữ liệu bằng SMOTE
    smote = SMOTE(random_state=42, k_neighbors=5)
    X_resampled, y_resampled = smote.fit_resample(X_filtered, y_filtered)

    # Đọc file ánh xạ nhãn
    with open(label_map_path, 'r', encoding='utf-8') as f:
        label_map = json.load(f)
    label_map = {int(k): v for k, v in label_map.items()}

    return X_resampled, y_resampled, scaler, label_map

# Hàm loại bỏ outliers dựa vào khoảng cách Euclidean trung bình
def remove_outliers_per_class(X_scaled, y, threshold=1.5):
    X_clean, y_clean = [], []
    for label in np.unique(y):
        idx = np.where(y == label)[0]
        X_label = X_scaled[idx]
        dists = euclidean_distances(X_label)
        avg_dists = dists.mean(axis=1)
        mean_dist = avg_dists.mean()
        std_dist = avg_dists.std()
        upper_limit = mean_dist + threshold * std_dist

        for i, original_idx in enumerate(idx):
            if avg_dists[i] <= upper_limit:
                X_clean.append(X_scaled[original_idx])
                y_clean.append(y[original_idx])

    return np.array(X_clean), np.array(y_clean)

# Huấn luyện mô hình với GridSearchCV và đánh giá
def train_model(X, y):
    X_train, X_val, y_train, y_val = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

    param_grid = {
        'n_estimators': [100, 200],
        'max_depth': [3, 5],
        'learning_rate': [0.05, 0.1],
        'subsample': [0.8],
        'colsample_bytree': [0.8],
        'reg_lambda': [1.0, 3.0],
        'reg_alpha': [0.0, 0.5]
    }

    model = XGBClassifier(random_state=42, eval_metric='mlogloss')
    grid_search = GridSearchCV(model, param_grid, cv=3, scoring='f1_weighted', n_jobs=-1, verbose=0)
    grid_search.fit(X_train, y_train)

    best_model = grid_search.best_estimator_
    print("✅ Tham số tốt nhất:", grid_search.best_params_)

    y_pred = best_model.predict(X_val)
    print("📊 Ma trận nhầm lẫn:")
    print(confusion_matrix(y_val, y_pred))
    print("\n📄 Báo cáo phân loại:")
    print(classification_report(y_val, y_pred, zero_division=0))

    return best_model, X_val, y_val

# Lưu mô hình và scaler
def save_model(model, scaler, model_filename, scaler_filename):
    os.makedirs('models', exist_ok=True)
    joblib.dump(model, model_filename)
    joblib.dump(scaler, scaler_filename)
    print(f"💾 Mô hình đã được lưu vào: {model_filename}")
    print(f"💾 Scaler đã được lưu vào: {scaler_filename}")

# Main
if __name__ == "__main__":
    data_path = 'data/sensor_data.csv'
    label_map_path = 'data/label_map.json'
    model_file = 'models/sensor_model.pkl'
    scaler_file = 'models/scaler.pkl'

    print("🔍 Bắt đầu huấn luyện mô hình...")
    try:
        X, y, scaler, label_map = load_and_preprocess_data(data_path, label_map_path)
        model, X_val, y_val = train_model(X, y)
        save_model(model, scaler, model_file, scaler_file)
        print("✅ Huấn luyện và lưu mô hình hoàn tất.")
    except Exception as e:
        print(f"❌ Lỗi trong quá trình huấn luyện: {e}")
