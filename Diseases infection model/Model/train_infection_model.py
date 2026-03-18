# import tensorflow as tf
# from tensorflow.keras.preprocessing.image import ImageDataGenerator
# from tensorflow.keras.models import Sequential
# from tensorflow.keras.layers import Conv2D, MaxPooling2D, Flatten, Dense, Dropout
# from tensorflow.keras.callbacks import EarlyStopping
# import os

# IMG_SIZE = 96
# BATCH_SIZE = 32
# EPOCHS = 15

# CROPS = ["wheat", "rice", "corn", "potato", "sugarcane"]
# BASE_DATA_DIR = r"D:\Intelligent Pesticide System\Diseases infection model\training_data"
# MODEL_DIR = r"D:\Intelligent Pesticide System\Diseases infection model\Model\models"
# os.makedirs(MODEL_DIR, exist_ok=True)

# early_stop = EarlyStopping(
#     monitor="val_loss",
#     patience=5,
#     restore_best_weights=True
# )

# datagen = ImageDataGenerator(
#     rescale=1./255,
#     validation_split=0.2,
#     rotation_range=15,
#     zoom_range=0.1,
#     horizontal_flip=True
# )

# for crop in CROPS:
#     print(f"\n🚀 Training model for {crop}...")

#     DATA_DIR = os.path.join(BASE_DATA_DIR, crop)

#     # Generators
#     train_gen = datagen.flow_from_directory(
#         DATA_DIR,
#         target_size=(IMG_SIZE, IMG_SIZE),
#         batch_size=BATCH_SIZE,
#         class_mode="binary",
#         subset="training"
#     )

#     val_gen = datagen.flow_from_directory(
#         DATA_DIR,
#         target_size=(IMG_SIZE, IMG_SIZE),
#         batch_size=BATCH_SIZE,
#         class_mode="binary",
#         subset="validation"
#     )

#     # Model definition (fresh for each crop)
#     model = Sequential([
#         Conv2D(16, (3,3), activation="relu", input_shape=(IMG_SIZE, IMG_SIZE, 3)),
#         MaxPooling2D(2,2),

#         Conv2D(32, (3,3), activation="relu"),
#         MaxPooling2D(2,2),

#         Flatten(),
#         Dense(64, activation="relu"),
#         Dropout(0.3),
#         Dense(1, activation="sigmoid")
#     ])

#     model.compile(
#         optimizer="adam",
#         loss="binary_crossentropy",
#         metrics=["accuracy"]
#     )

#     # Train
#     model.fit(
#         train_gen,
#         validation_data=val_gen,
#         epochs=EPOCHS,
#         callbacks=[early_stop]
#     )

#     # Save
#     model.save(f"{MODEL_DIR}/{crop}_model.keras")
#     print(f"✅ {crop} model trained and saved")

import tensorflow as tf
from tensorflow.keras.applications import EfficientNetB0
from tensorflow.keras.layers import Dense, GlobalAveragePooling2D, Dropout
from tensorflow.keras.models import Model
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.optimizers import Adam
import os

CROPS = ["wheat", "rice", "corn", "potato", "sugarcane"]
BASE_DATASET_DIR = r"D:\Intelligent Pesticide System\Diseases infection model\training_data - Copy"
IMG_SIZE = 224
BATCH_SIZE = 16
EPOCHS = 20
MODEL_DIR = r"D:\Intelligent Pesticide System\Diseases infection model\Model\models"
os.makedirs(MODEL_DIR, exist_ok=True)

datagen = ImageDataGenerator(
    rescale=1./255,
    rotation_range=30,
    zoom_range=0.25,
    brightness_range=(0.7, 1.3),
    horizontal_flip=True,
    validation_split=0.2
)

for crop in CROPS:
    print(f"\n🚀 Training model for {crop}...")

    DATASET_DIR = os.path.join(BASE_DATASET_DIR, f"{crop}", "train")
    MODEL_OUT = os.path.join(MODEL_DIR, f"{crop}_model.h5")

    # ---- Data generators ----
    train_data = datagen.flow_from_directory(
        DATASET_DIR,
        target_size=(IMG_SIZE, IMG_SIZE),
        batch_size=BATCH_SIZE,
        class_mode="binary",
        subset="training"
    )

    val_data = datagen.flow_from_directory(
        DATASET_DIR,
        target_size=(IMG_SIZE, IMG_SIZE),
        batch_size=BATCH_SIZE,
        class_mode="binary",
        subset="validation"
    )

    # ---- Model ----
    base = EfficientNetB0(weights="imagenet", include_top=False, input_shape=(IMG_SIZE, IMG_SIZE, 3))
    for layer in base.layers:
        layer.trainable = False   # freeze base model

    x = base.output
    x = GlobalAveragePooling2D()(x)
    x = Dropout(0.4)(x)
    out = Dense(1, activation="sigmoid")(x)

    model = Model(inputs=base.input, outputs=out)
    model.compile(optimizer=Adam(1e-4), loss="binary_crossentropy", metrics=["accuracy"])

    # ---- Train ----
    model.fit(train_data, validation_data=val_data, epochs=EPOCHS)

    # ---- Save ----
    model.save(MODEL_OUT)
    print(f"✅ Model for {crop} saved at {MODEL_OUT}")
    print("Class mapping:", train_data.class_indices)
