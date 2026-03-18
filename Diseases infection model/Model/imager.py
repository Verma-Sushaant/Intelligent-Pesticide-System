# import sys
# import cv2
# import os
# from PyQt5.QtWidgets import (
#     QApplication, QWidget, QPushButton, QLabel,
#     QFileDialog, QVBoxLayout, QHBoxLayout
# )
# from PyQt5.QtGui import QPixmap, QImage
# from PyQt5.QtCore import Qt

# IMAGE_PATH = "latest.jpg"

# class ImageUI(QWidget):
#     def __init__(self):
#         super().__init__()
#         self.setWindowTitle("Intelligent Pesticide System - Image Input")
#         self.image_ready = False

#         self.image_label = QLabel("No Image Selected")
#         self.image_label.setAlignment(Qt.AlignCenter)
#         self.image_label.setFixedSize(400, 300)

#         self.capture_btn = QPushButton("📸 Capture Image")
#         self.select_btn = QPushButton("🖼 Select Image")
#         self.send_btn = QPushButton("🚀 Send")

#         self.send_btn.setEnabled(False)

#         self.capture_btn.clicked.connect(self.capture_image)
#         self.select_btn.clicked.connect(self.select_image)
#         self.send_btn.clicked.connect(self.send_image)

#         btn_layout = QHBoxLayout()
#         btn_layout.addWidget(self.capture_btn)
#         btn_layout.addWidget(self.select_btn)

#         layout = QVBoxLayout()
#         layout.addWidget(self.image_label)
#         layout.addLayout(btn_layout)
#         layout.addWidget(self.send_btn)

#         self.setLayout(layout)

#     # -------- Capture using webcam --------
#     def capture_image(self):
#         cap = cv2.VideoCapture(0)
#         if not cap.isOpened():
#             return

#         while True:
#             ret, frame = cap.read()
#             if not ret:
#                 break

#             cv2.imshow("Press SPACE to Capture | ESC to Cancel", frame)
#             key = cv2.waitKey(1)

#             if key == 32:  # SPACE
#                 cv2.imwrite(IMAGE_PATH, frame)
#                 break
#             elif key == 27:  # ESC
#                 cap.release()
#                 cv2.destroyAllWindows()
#                 return

#         cap.release()
#         cv2.destroyAllWindows()
#         self.load_preview(IMAGE_PATH)

#     # -------- Select image from disk --------
#     def select_image(self):
#         path, _ = QFileDialog.getOpenFileName(
#             self, "Select Image",
#             "", "Images (*.png *.jpg *.jpeg)"
#         )
#         if path:
#             img = cv2.imread(path)
#             cv2.imwrite(IMAGE_PATH, img)
#             self.load_preview(IMAGE_PATH)

#     # -------- Load preview --------
#     def load_preview(self, path):
#         pixmap = QPixmap(path)
#         self.image_label.setPixmap(
#             pixmap.scaled(
#                 self.image_label.width(),
#                 self.image_label.height(),
#                 Qt.KeepAspectRatio
#             )
#         )
#         self.image_ready = True
#         self.send_btn.setEnabled(True)

#     # -------- Send button --------
#     def send_image(self):
#         self.close()

# def get_image_from_user():
#     """
#     Called by controller.py
#     Returns image path or None
#     """
#     app = QApplication(sys.argv)
#     ui = ImageUI()
#     ui.show()
#     app.exec_()

#     if ui.image_ready and os.path.exists(IMAGE_PATH):
#         return IMAGE_PATH
#     return None

import sys
import time
import cv2
import os
from PyQt5.QtWidgets import (
    QApplication, QWidget, QPushButton, QLabel,
    QFileDialog, QVBoxLayout, QHBoxLayout
)
from PyQt5.QtGui import QPixmap
from PyQt5.QtCore import Qt

def auto_capture_image():
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        return None
    
    time.sleep(0.5)
    ret, frame = cap.read()
    cap.release()

    if not ret:
        return None
    
    cv2.imwrite(IMAGE_PATH, frame)
    return IMAGE_PATH

IMAGE_PATH = "latest.jpg"


class ImageUI(QWidget):
    def __init__(self, result_text=None):
        super().__init__()
        self.setWindowTitle("Intelligent Pesticide System - Image Input")
        self.image_ready = False

        self.image_label = QLabel("No Image Selected")
        self.image_label.setAlignment(Qt.AlignCenter)
        self.image_label.setFixedSize(400, 300)

        self.result_label = QLabel("")
        self.result_label.setAlignment(Qt.AlignCenter)
        self.result_label.setStyleSheet("font-size: 14px; font-weight: bold;")

        if result_text:
            self.result_label.setText(result_text)

        self.capture_btn = QPushButton("📸 Capture Image")
        self.select_btn = QPushButton("🖼 Select Image")
        self.send_btn = QPushButton("🚀 Send")

        self.send_btn.setEnabled(False)

        self.capture_btn.clicked.connect(self.capture_image)
        self.select_btn.clicked.connect(self.select_image)
        self.send_btn.clicked.connect(self.send_image)

        btn_layout = QHBoxLayout()
        btn_layout.addWidget(self.capture_btn)
        btn_layout.addWidget(self.select_btn)

        layout = QVBoxLayout()
        layout.addWidget(self.image_label)
        layout.addLayout(btn_layout)
        layout.addWidget(self.send_btn)
        layout.addWidget(self.result_label)

        self.setLayout(layout)

    def capture_image(self):
        cap = cv2.VideoCapture(0)
        if not cap.isOpened():
            return

        while True:
            ret, frame = cap.read()
            if not ret:
                break

            cv2.imshow("Press SPACE to Capture | ESC to Cancel", frame)
            key = cv2.waitKey(1)

            if key == 32:
                cv2.imwrite(IMAGE_PATH, frame)
                break
            elif key == 27:
                cap.release()
                cv2.destroyAllWindows()
                return

        cap.release()
        cv2.destroyAllWindows()
        self.load_preview(IMAGE_PATH)

    def select_image(self):
        path, _ = QFileDialog.getOpenFileName(
            self, "Select Image",
            "", "Images (*.png *.jpg *.jpeg)"
        )
        if path:
            img = cv2.imread(path)
            cv2.imwrite(IMAGE_PATH, img)
            self.load_preview(IMAGE_PATH)

    def load_preview(self, path):
        pixmap = QPixmap(path)
        self.image_label.setPixmap(
            pixmap.scaled(
                self.image_label.width(),
                self.image_label.height(),
                Qt.KeepAspectRatio
            )
        )
        self.image_ready = True
        self.send_btn.setEnabled(True)

    def send_image(self):
        QApplication.quit()


def get_image_from_user(result_text=None, auto_capture=False):
    if auto_capture:
        return auto_capture_image()
    
    app = QApplication(sys.argv)
    ui = ImageUI(result_text)
    ui.show()
    app.exec_()

    if ui.image_ready and os.path.exists(IMAGE_PATH):
        return IMAGE_PATH
    return None
