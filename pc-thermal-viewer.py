#!/usr/bin/env python3
"""
PC Thermal Viewer for Topdon TC001 Thermal Camera
Connects to Android app's NetworkServer to receive thermal frames
"""

import socket
import json
import base64
import cv2
import numpy as np
from PIL import Image
import io
import threading
import time


class ThermalViewer:
    def __init__(self, android_ip="192.168.1.2", android_port=8080):
        self.android_ip = android_ip
        self.android_port = android_port
        self.socket = None
        self.running = False
        self.frame_count = 0

    def connect(self):
        """Connect to Android app's NetworkServer"""
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((self.android_ip, self.android_port))
            print(f"✅ Connected to thermal camera at {self.android_ip}:{self.android_port}")
            return True
        except Exception as e:
            print(f"❌ Failed to connect: {e}")
            return False

    def receive_thermal_frames(self):
        """Receive and process thermal frames from Android app"""
        if not self.socket:
            print("❌ Not connected to Android app")
            return

        self.running = True

        while self.running:
            try:
                
                header = self.socket.recv(4)
                if not header or len(header) < 4:
                    print("❌ Connection closed by Android app or invalid header")
                    break

                msg_len = int.from_bytes(header, 'big')

                
                data = b""
                while len(data) < msg_len:
                    packet = self.socket.recv(msg_len - len(data))
                    if not packet:
                        break  
                    data += packet

                if len(data) == msg_len:
                    self.process_message(data.decode('utf-8'))
                else:
                    print(f"❌ Incomplete message received. Expected {msg_len}, got {len(data)}")
                    break

            except Exception as e:
                print(f"❌ Error receiving data: {e}")
                break

        self.running = False

    def process_message(self, message_str):
        """Process received thermal frame message"""
        try:
            message = json.loads(message_str)

            if message.get('type') == 'thermal_frame':
                self.frame_count += 1

                
                frame_num = message.get('frame_number', 0)
                sensor_id = message.get('sensor_id', 'unknown')
                min_temp = message.get('min_temp_c', 'N/A')
                max_temp = message.get('max_temp_c', 'N/A')
                avg_temp = message.get('avg_temp_c', 'N/A')
                center_temp = message.get('center_temp_c', 'N/A')
                simulation = message.get('simulation_mode', True)

                print(
                    f"🌡️  Frame #{frame_num} | Temp: {min_temp}°C - {max_temp}°C (avg: {avg_temp}°C) | "
                    f"Center: {center_temp}°C | {'SIM' if simulation else 'REAL'}")

                
                if 'image_jpeg_base64' in message:
                    self.display_thermal_image(message['image_jpeg_base64'], frame_num)

        except json.JSONDecodeError as e:
            print(f"❌ Failed to parse JSON: {e}")
        except Exception as e:
            print(f"❌ Error processing message: {e}")

    def display_thermal_image(self, base64_image, frame_num):
        """Display thermal image using OpenCV"""
        try:
            
            image_bytes = base64.b64decode(base64_image)
            image = Image.open(io.BytesIO(image_bytes))

            
            cv_image = cv2.cvtColor(np.array(image), cv2.COLOR_RGB2BGR)

            
            cv2.putText(cv_image, f"Frame #{frame_num}", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)

            
            cv2.imshow('Topdon TC001 Thermal Camera', cv_image)

            
            if cv2.waitKey(1) & 0xFF == ord('q'):
                print("👋 User requested quit")
                self.stop()

        except Exception as e:
            print(f"❌ Error displaying image: {e}")

    def start(self):
        """Start thermal viewer"""
        print("🚀 Starting PC Thermal Viewer for Topdon TC001")
        print(f"📱 Connecting to Android app at {self.android_ip}:{self.android_port}")

        if self.connect():
            
            receive_thread = threading.Thread(target=self.receive_thermal_frames)
            receive_thread.daemon = True
            receive_thread.start()

            print("🔥 Thermal viewer active - press 'q' in image window to quit")
            print("📊 Waiting for thermal frames...")

            try:
                while self.running:
                    time.sleep(0.1)
            except KeyboardInterrupt:
                print("\n👋 Keyboard interrupt - stopping viewer")

            self.stop()

    def stop(self):
        """Stop thermal viewer"""
        self.running = False
        if self.socket:
            self.socket.close()
        cv2.destroyAllWindows()
        print(f"✅ Thermal viewer stopped. Received {self.frame_count} frames total.")


def main():
    print("=" * 60)
    print("PC Thermal Viewer for Topdon TC001 Thermal Camera")
    print("=" * 60)

    
    android_ip = input("Enter Android device IP address (default: 192.168.1.2): ").strip()
    if not android_ip:
        android_ip = "192.168.1.2"

    
    viewer = ThermalViewer(android_ip=android_ip)
    viewer.start()


if __name__ == "__main__":
    main()
