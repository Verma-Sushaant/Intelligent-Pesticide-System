# # import serial
# # import json
# # import time
# # import os

# # from predict import predict_image
# # from imager import get_image_from_user

# # # ================= CONFIG =================
# # SERIAL_PORT = "COM5"      # Change as needed
# # BAUD_RATE = 115200

# # AUTO_CAPTURE = False     # 🔁 future ESP camera mode
# # TEST_MODE = True
# # IMAGE_PATH = "latest.jpg"
# # # =========================================

# # def send_json(ser, data):
# #     ser.write((json.dumps(data) + "\n").encode())

# # def main():
# #     try:
# #         ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=5)
# #         print("✅ Controller started, waiting for ESP32...")

# #         while True:
# #             if TEST_MODE :
# #                 esp_data = {
# #                     "crop": "wheat",
# #                     "temperature": 25.5,
# #                     "humidity": 60.0,
# #                     "soil": 20
# #                 }
# #             else :
# #                 if not ser.in_waiting:
# #                     time.sleep(0.2)
# #                     continue

# #                 try:
# #                     incoming = ser.readline().decode().strip()
# #                     esp_data = json.loads(incoming)
# #                 except Exception:
# #                     send_json(ser, {"error": "Invalid JSON from ESP"})
# #                     continue

# #                 # Extract ESP data
# #                 crop = esp_data.get("crop")
# #                 temp = esp_data.get("temperature")
# #                 humidity = esp_data.get("humidity")
# #                 soil = esp_data.get("soil")

# #             # -------- Image acquisition --------
# #             if not AUTO_CAPTURE:
# #                 image_path = get_image_from_user()
# #             else:
# #                 image_path = IMAGE_PATH  # future ESP camera

# #             if not image_path or not os.path.exists(image_path):
# #                 send_json(ser, {"error": "Image not available"})
# #                 continue

# #             # -------- AI Prediction --------
# #             try:
# #                 prob, severity = predict_image(
# #                     image_path, crop, temp, humidity, soil
# #                 )

# #                 risk = "Low" if prob <= 30 else "Medium" if prob <= 60 else "High"

# #                 response = {
# #                     "infection_probability": prob,
# #                     "severity": severity,
# #                     "risk_level": risk
# #                 }

# #                 send_json(ser, response)

# #             except Exception as e:
# #                 send_json(ser, {"error": str(e)})

# #     except serial.SerialException as e:
# #         print("Could not open port: ", e)
# #         return

# # if __name__ == "__main__":
# #     main()

# import serial
# import json
# import time
# import os

# from predict import predict_image
# from imager import get_image_from_user

# SERIAL_PORT = "COM5"
# BAUD_RATE = 115200

# AUTO_CAPTURE = False
# TEST_MODE = True
# IMAGE_PATH = "latest.jpg"


# def send_json(ser, data):
#     ser.write((json.dumps(data) + "\n").encode())


# def main():
#     try:
#         ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=5)
#         print("✅ Controller started")

#         while True:
#             if TEST_MODE:
#                 esp_data = {
#                     "crop": "wheat",
#                     "temperature": 25.5,
#                     "humidity": 60.0,
#                     "soil": 20
#                 }
#             else:
#                 if not ser.in_waiting:
#                     time.sleep(0.2)
#                     continue
#                 esp_data = json.loads(ser.readline().decode().strip())

#             crop = esp_data["crop"]
#             temp = esp_data["temperature"]
#             humidity = esp_data["humidity"]
#             soil = esp_data["soil"]

#             image_path = get_image_from_user()
#             if not image_path:
#                 continue

#             prob, severity = predict_image(
#                 image_path, crop, temp, humidity, soil
#             )

#             risk = "Low" if prob <= 30 else "Medium" if prob <= 60 else "High"

#             result_text = (
#                 f"Infection Probability: {prob}%\n"
#                 f"Severity: {severity}\n"
#                 f"Risk Level: {risk}"
#             )

#             # Show result in UI
#             get_image_from_user(result_text)

#             if not TEST_MODE:
#                 send_json(ser, {
#                     "infection_probability": prob,
#                     "severity": severity,
#                     "risk_level": risk
#                 })

#     except serial.SerialException as e:
#         print("Serial Error:", e)


# if __name__ == "__main__":
#     main()

import serial
import json
import time
import os

from predict import predict_image
from imager import get_image_from_user

SERIAL_PORT = "COM5"
BAUD_RATE = 115200

AUTO_CAPTURE = False
TEST_MODE = True

def send_json(ser, data):
    try:
        ser.write((json.dumps(data) + "\n").encode())
    except Exception as e:
        print("⚠️ Failed to send JSON:", e)

def main():
    # try:    
    #     ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=5)
    #     print("✅ Controller started")

    #     while True:
    #         if TEST_MODE:
    #             esp_data = {
    #                 "crop": "rice",
    #                 "temperature": 25.5,
    #                 "humidity": 60.0,
    #                 "soil": 20,
    #                 "autoCapture": False
    #             }
    #         else:
    #             if not ser.in_waiting:
    #                 time.sleep(0.2)
    #                 continue
    #             esp_data = json.loads(ser.readline().decode().strip())

    #         crop = esp_data["crop"]
    #         temp = esp_data["temperature"]
    #         humidity = esp_data["humidity"]
    #         soil = esp_data["soil"]
    #         # AUTO_CAPTURE = esp_data["autoCapture"]

    #         image_path = get_image_from_user(auto_capture = AUTO_CAPTURE)
    #         if not image_path:
    #             continue

    #         risk_prob, inf_prob, severity = predict_image(
    #             image_path, crop, temp, humidity, soil
    #         )

    #         risk = "Low" if risk_prob <= 30 else "Medium" if risk_prob <= 60 else "High"

    #         print("\n--- AI RESULT ---")
    #         print(f"Probability: {inf_prob}%")
    #         print(f"Severity: {severity}")
    #         print(f"Risk: {risk}")

    #         if not TEST_MODE:
    #             send_json(ser, {
    #                 "infection_probability": inf_prob,
    #                 "severity": severity,
    #                 "risk_level": risk
    #             })

    # except serial.SerialException as e:
    #     print("Serial Error:", e)
    try:
        ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=5)
        print("✅ Controller started")

        while True:
            # --- Step 1: Get ESP data or fallback ---
            if TEST_MODE:
                esp_data = {
                    "crop": "rice",
                    "temperature": 25.5,
                    "humidity": 60.0,
                    "soil": 20,
                    "autoCapture": False
                }
            else:
                incoming = ser.readline().decode(errors="ignore").strip()
                if not incoming:
                    # fallback if ESP silent
                    esp_data = {
                        "crop": "rice",
                        "temperature": 18,
                        "humidity": 68.0,
                        "soil": 0,
                        "autoCapture": False
                    }
                else:
                    try:
                        esp_data = json.loads(incoming)
                    except json.JSONDecodeError:
                        print(f"⚠️ Invalid JSON from ESP: {incoming}")
                        continue
                
                # --- Step 2: Safely extract fields ---
            crop = esp_data.get("crop", "Unknown")
            temp = esp_data.get("temperature", 0.0)
            humidity = esp_data.get("humidity", 0.0)
            soil = esp_data.get("soil", 0)
            auto_capture = esp_data.get("autoCapture", False)

            # --- Step 3: Open UI / capture image ---
            image_path = get_image_from_user(auto_capture=auto_capture)
            if not image_path or not os.path.exists(image_path):
                print("⚠️ No image selected or file missing")
                continue

            # --- Step 4: Run AI prediction ---
            try:
                risk_prob, inf_prob, severity = predict_image(
                    image_path, crop, temp, humidity, soil
                )
            except Exception as e:
                print("⚠️ Prediction error:", e)
                continue

            risk = "Low" if risk_prob <= 30 else "Medium" if risk_prob <= 60 else "High"

            print("\n--- AI RESULT ---")
            print(f"Probability: {inf_prob}%")
            print(f"Severity: {severity}")
            print(f"Risk: {risk}")

            # --- Step 5: Send back to ESP ---
            if not TEST_MODE:
                send_json(ser, {
                    "infection_probability": inf_prob,
                    "severity": severity,
                    "risk_level": risk
                })
            

    except serial.SerialException as e:
        print("❌ Serial Error:", e)

if __name__ == "__main__":
    main()

# import serial
# import json
# import time
# import os

# from predict import predict_image
# from imager import get_image_from_user

# SERIAL_PORT = "COM5"
# BAUD_RATE = 115200

# TEST_MODE = False   # set True for offline testing

# def send_json(ser, data):
#     try:
#         ser.write((json.dumps(data) + "\n").encode())
#     except Exception as e:
#         print("⚠️ Failed to send JSON:", e)

# def main():
#     try:
#         ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=5)
#         print("✅ Controller started")

#         while True:
#             # --- Step 1: Get ESP data or fallback ---
#             if TEST_MODE:
#                 esp_data = {
#                     "crop": "rice",
#                     "temperature": 25.5,
#                     "humidity": 60.0,
#                     "soil": 20,
#                     "autoCapture": False
#                 }
#             else:
#                 incoming = ser.readline().decode(errors="ignore").strip()
#                 if not incoming:
#                     # fallback if ESP silent
#                     esp_data = {
#                         "crop": "rice",
#                         "temperature": 25.5,
#                         "humidity": 60.0,
#                         "soil": 20,
#                         "autoCapture": False
#                     }
#                 else:
#                     try:
#                         esp_data = json.loads(incoming)
#                     except json.JSONDecodeError:
#                         print(f"⚠️ Invalid JSON from ESP: {incoming}")
#                         continue
                
#                 # --- Step 2: Safely extract fields ---
#             crop = esp_data.get("crop", "Unknown")
#             temp = esp_data.get("temperature", 0.0)
#             humidity = esp_data.get("humidity", 0.0)
#             soil = esp_data.get("soil", 0)
#             auto_capture = esp_data.get("autoCapture", False)

#             # --- Step 3: Open UI / capture image ---
#             image_path = get_image_from_user(auto_capture=auto_capture)
#             if not image_path or not os.path.exists(image_path):
#                 print("⚠️ No image selected or file missing")
#                 continue

#             # --- Step 4: Run AI prediction ---
#             try:
#                 risk_prob, inf_prob, severity = predict_image(
#                     image_path, crop, temp, humidity, soil
#                 )
#             except Exception as e:
#                 print("⚠️ Prediction error:", e)
#                 continue

#             risk = "Low" if risk_prob <= 30 else "Medium" if risk_prob <= 60 else "High"

#             print("\n--- AI RESULT ---")
#             print(f"Probability: {inf_prob}%")
#             print(f"Severity: {severity}")
#             print(f"Risk: {risk}")

#             # --- Step 5: Send back to ESP ---
#             if not TEST_MODE:
#                 send_json(ser, {
#                     "infection_probability": inf_prob,
#                     "severity": severity,
#                     "risk_level": risk
#                 })
            

#     except serial.SerialException as e:
#         print("❌ Serial Error:", e)

# if __name__ == "__main__":
#     main()
