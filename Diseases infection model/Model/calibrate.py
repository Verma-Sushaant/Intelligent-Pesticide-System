import numpy as np
import tensorflow as tf
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from scipy.optimize import minimize
import os

# ================= CONFIG =================
CROPS = ["wheat", "rice", "corn", "potato", "sugarcane"]
BASE_MODEL_DIR = r"D:\Intelligent Pesticide System\Diseases infection model\Model\models"
BASE_DATASET_DIR = r"D:\Intelligent Pesticide System\Diseases infection model\training_data - Copy"
IMG_SIZE = 224
# ==========================================

datagen = ImageDataGenerator(rescale=1./255)

for crop in CROPS:
    print(f"\n🌱 Calibrating temperature for {crop}...")

    MODEL_PATH = os.path.join(BASE_MODEL_DIR, f"{crop}_model.h5")
    DATASET_DIR = os.path.join(BASE_DATASET_DIR, crop, "val")

    # ---- Load model ----
    model = tf.keras.models.load_model(MODEL_PATH)

    # ---- Validation data ----
    val_data = datagen.flow_from_directory(
        DATASET_DIR,
        target_size=(IMG_SIZE, IMG_SIZE),
        batch_size=1,
        class_mode="binary",
        shuffle=False
    )

    # Ground truth labels
    y_true = val_data.classes  # 0 = diseased, 1 = healthy

    # Raw sigmoid outputs (P(class=1) = healthy)
    y_pred = model.predict(val_data, verbose=0).flatten()

    # ---- FIX 1: Numerical stability ----
    eps = 1e-6
    y_pred = np.clip(y_pred, eps, 1 - eps)

    # ---- FIX 2: Convert to disease probability ----
    # disease = 1 - healthy
    y_pred_disease = 1 - y_pred

    # ---- Temperature Scaling ----
    def nll_loss(T):
        T = T[0]
        logits = np.log(y_pred_disease / (1 - y_pred_disease))
        scaled = logits / T
        probs = 1 / (1 + np.exp(-scaled))
        return -np.mean(
            y_true * np.log(probs + eps) +
            (1 - y_true) * np.log(1 - probs + eps)
        )

    res = minimize(
        nll_loss,
        x0=[1.5],
        bounds=[(0.5, 10.0)],
        method="L-BFGS-B"
    )

    temperature = float(res.x[0])

    # ---- Save temperature ----
    np.save(os.path.join(BASE_MODEL_DIR, f"{crop}_temperature.npy"), temperature)
    print(f"✅ {crop} temperature saved:", temperature)
