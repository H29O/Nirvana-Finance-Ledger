import os
import sys
import unittest

# Add project root to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from fastapi.testclient import TestClient
from app.main import app

class TestPaddleOcrService(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_health_endpoint(self):
        response = self.client.get("/health")
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data.get("status"), "UP")
        self.assertEqual(data.get("engine"), "PaddleOCR 3.x")

    def test_ocr_endpoint_with_photographed_receipt(self):
        receipt_path = os.path.abspath(
            os.path.join(os.path.dirname(__file__), "../../uploads/receipts/2/1c3d1042-bf32-47bc-833d-181fa20e019a.jpeg")
        )
        if not os.path.exists(receipt_path):
            self.skipTest(f"Receipt file not found: {receipt_path}")

        with open(receipt_path, "rb") as f:
            response = self.client.post(
                "/ocr",
                files={"file": ("receipt.jpeg", f, "image/jpeg")}
            )

        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertTrue(data.get("success"), f"OCR failed: {data.get('error')}")
        raw_text = data.get("raw_text", "")
        self.assertTrue(len(raw_text) > 0)
        self.assertTrue(len(data.get("blocks", [])) > 0)
        print("\n--- PADDLEOCR RAW TEXT ---")
        print(raw_text)
        print("--- END PADDLEOCR RAW TEXT ---\n")

if __name__ == "__main__":
    unittest.main()
