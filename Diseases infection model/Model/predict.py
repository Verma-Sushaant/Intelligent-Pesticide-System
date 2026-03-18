import cv2
import numpy as np
import tensorflow as tf
import os
from risk_engine import calculate_infection_risk

MODEL_DIR = r"D:\Intelligent Pesticide System\Diseases infection model\Model\models"
_temperature_cache = {}
_loaded_models = {}

def get_temperature(crop):
    if crop not in _temperature_cache:
        path = os.path.join(MODEL_DIR, f"{crop}_temperature.npy")
        _temperature_cache[crop] = np.load(path)
    return _temperature_cache[crop]

def get_model(crop):
    if crop not in _loaded_models:
        model_path = os.path.join(MODEL_DIR, f"{crop}_model.h5")
        _loaded_models[crop] = tf.keras.models.load_model(model_path)
    return _loaded_models[crop]

def predict_image(img_path, crop, temp, humidity, soil):
    model = get_model(crop)

    img = cv2.imread(img_path)
    img = cv2.resize(img, (224,224))
    img = img / 255.0
    img = np.expand_dims(img, axis=0)

    raw = float(model.predict(img)[0][0])
    disease_prob = 1.0 - raw

    T = get_temperature(crop)
    logits = np.log(disease_prob / (1 - disease_prob))
    scaled = logits / T
    calibrated = 1 / (1 + np.exp(-scaled))

    base_prob = (1-calibrated) * 100

    final_prob, severity = calculate_infection_risk(
        base_prob, crop, temp, humidity, soil
    )

    return int(final_prob), int(base_prob), severity
