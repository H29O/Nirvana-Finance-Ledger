import io
import os
import time
import logging
from typing import List, Tuple
from PIL import Image
import numpy as np

os.environ["PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK"] = "True"

from app.schemas import OcrResponse, OCRTextBlock, BoundingBox

logger = logging.getLogger("ocr_service")

class PaddleOcrEngine:
    def __init__(self):
        logger.info("Initializing PaddleOCR 3.x engine with document orientation & unwarping...")
        start_time = time.time()
        from paddleocr import PaddleOCR
        
        self.ocr = PaddleOCR(
            use_doc_orientation_classify=True,
            use_doc_unwarping=True,
            use_textline_orientation=True,
            lang='en'
        )
        logger.info("PaddleOCR engine initialized in %.2f seconds", time.time() - start_time)

    def process_image_bytes(self, image_bytes: bytes) -> OcrResponse:
        start_time = time.time()
        try:
            pil_image = Image.open(io.BytesIO(image_bytes))
            if pil_image.mode != "RGB":
                pil_image = pil_image.convert("RGB")

            img_width, img_height = pil_image.size
            img_np = np.array(pil_image)

            # Run PaddleOCR 3.x predict pipeline
            predict_results = self.ocr.predict(img_np)
            if not predict_results:
                return OcrResponse(
                    success=True,
                    raw_text="",
                    blocks=[],
                    lines=[],
                    page_width=img_width,
                    page_height=img_height,
                    processing_time_ms=(time.time() - start_time) * 1000.0
                )

            res = predict_results[0]
            rec_texts = res.get('rec_texts', [])
            rec_scores = res.get('rec_scores', [])
            rec_polys = res.get('rec_polys', [])
            rec_boxes = res.get('rec_boxes', [])

            raw_blocks: List[Tuple[float, float, float, float, OCRTextBlock]] = []

            for i, text in enumerate(rec_texts):
                t = str(text).strip()
                if not t:
                    continue

                conf = float(rec_scores[i]) if i < len(rec_scores) else 0.9

                if i < len(rec_polys) and rec_polys[i] is not None:
                    poly = rec_polys[i]
                    pts = [[float(pt[0]), float(pt[1])] for pt in poly]
                elif i < len(rec_boxes) and rec_boxes[i] is not None:
                    b = rec_boxes[i]
                    pts = [
                        [float(b[0]), float(b[1])],
                        [float(b[2]), float(b[1])],
                        [float(b[2]), float(b[3])],
                        [float(b[0]), float(b[3])]
                    ]
                else:
                    pts = [[0.0, 0.0], [0.0, 0.0], [0.0, 0.0], [0.0, 0.0]]

                xs = [p[0] for p in pts]
                ys = [p[1] for p in pts]
                min_x = min(xs)
                max_x = max(xs)
                min_y = min(ys)
                max_y = max(ys)
                center_x = (min_x + max_x) / 2.0
                center_y = (min_y + max_y) / 2.0
                w = max_x - min_x
                h = max_y - min_y

                bbox = BoundingBox(
                    points=pts,
                    top_left=[min_x, min_y],
                    bottom_right=[max_x, max_y],
                    center_x=center_x,
                    center_y=center_y,
                    width=w,
                    height=h
                )

                block = OCRTextBlock(
                    text=t,
                    confidence=conf,
                    box=bbox,
                    center_x=center_x,
                    center_y=center_y,
                    width=w,
                    height=h,
                    line_index=0
                )
                raw_blocks.append((center_y, center_x, h, w, block))

            # Group blocks into reading order lines
            raw_blocks.sort(key=lambda b: b[0])

            grouped_lines: List[List[OCRTextBlock]] = []
            current_line: List[OCRTextBlock] = []
            current_line_y = 0.0
            current_line_h = 0.0

            for cy, cx, h, w, block in raw_blocks:
                if not current_line:
                    current_line = [block]
                    current_line_y = cy
                    current_line_h = max(h, 10.0)
                else:
                    y_diff = abs(cy - current_line_y)
                    threshold = max(current_line_h, h) * 0.55
                    if y_diff <= threshold:
                        current_line.append(block)
                        current_line_y = sum(b.center_y for b in current_line) / len(current_line)
                        current_line_h = max(current_line_h, h)
                    else:
                        grouped_lines.append(current_line)
                        current_line = [block]
                        current_line_y = cy
                        current_line_h = max(h, 10.0)

            if current_line:
                grouped_lines.append(current_line)

            ordered_blocks: List[OCRTextBlock] = []
            text_lines: List[str] = []

            for line_idx, line_blocks in enumerate(grouped_lines):
                line_blocks.sort(key=lambda b: b.center_x)
                line_texts: List[str] = []
                for b in line_blocks:
                    b.line_index = line_idx
                    ordered_blocks.append(b)
                    line_texts.append(b.text)
                text_lines.append(" ".join(line_texts))

            full_raw_text = "\n".join(text_lines)
            elapsed_ms = (time.time() - start_time) * 1000.0

            return OcrResponse(
                success=True,
                raw_text=full_raw_text,
                blocks=ordered_blocks,
                lines=text_lines,
                page_width=img_width,
                page_height=img_height,
                processing_time_ms=elapsed_ms
            )

        except Exception as e:
            logger.exception("Error processing image in PaddleOCR: %s", str(e))
            return OcrResponse(
                success=False,
                raw_text="",
                blocks=[],
                lines=[],
                error=str(e),
                processing_time_ms=(time.time() - start_time) * 1000.0
            )
