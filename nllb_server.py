"""
NLLB-200-Distilled-600M Translation Server
===========================================
Local Flask server that exposes a /translate endpoint used by the PathBoot PI
Spring Boot application for Amharic <-> English translation.

Prerequisites
-------------
    pip install flask transformers torch sentencepiece sacremoses

Run
---
    python nllb_server.py

The server starts on http://localhost:5000 by default.

Request  (POST /translate)
--------------------------
    {
        "text": "ቀረጥ ምን ያህል ነው?",
        "source_language": "amh_Ethi",
        "target_language": "eng_Latn"
    }

Response
--------
    {
        "translated_text": "How much is the tax?"
    }

NLLB language codes used by PathBoot PI
----------------------------------------
    Amharic  : amh_Ethi
    English  : eng_Latn
    Norwegian: nob_Latn  (not used by this server - handled by Ollama)
"""

import logging
from flask import Flask, request, jsonify
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import torch

# ── Logging ──────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
)
logger = logging.getLogger("nllb_server")

# ── Constants ─────────────────────────────────────────────────────────────────
MODEL_NAME = "facebook/nllb-200-distilled-600M"
SERVER_HOST = "0.0.0.0"
SERVER_PORT = 5000
MAX_LENGTH  = 512

SUPPORTED_LANGUAGE_PAIRS = {
    ("amh_Ethi", "eng_Latn"),
    ("eng_Latn", "amh_Ethi"),
}

# ── Load model and pipeline once at startup ───────────────────────────────────
logger.info("Loading NLLB tokenizer from: %s", MODEL_NAME)
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)

logger.info("Loading NLLB model from: %s", MODEL_NAME)
device = 0 if torch.cuda.is_available() else -1
logger.info("Using device: %s", "GPU (CUDA)" if device == 0 else "CPU")
model = AutoModelForSeq2SeqLM.from_pretrained(MODEL_NAME)
logger.info("Model loaded successfully.")

def _translate_text(text: str, source_language: str, target_language: str) -> str:
    """Translate text using the loaded NLLB model directly."""
    tokenizer.src_lang = source_language
    inputs = tokenizer(text, return_tensors="pt")

    if device == 0:
        inputs = {key: value.to(model.device) for key, value in inputs.items()}

    lang_code_to_id = getattr(tokenizer, "lang_code_to_id", None)
    if isinstance(lang_code_to_id, dict) and target_language in lang_code_to_id:
        forced_bos_token_id = lang_code_to_id[target_language]
    elif hasattr(tokenizer, "get_lang_id"):
        forced_bos_token_id = tokenizer.get_lang_id(target_language)
    else:
        forced_bos_token_id = tokenizer.convert_tokens_to_ids(target_language)

    if forced_bos_token_id is None or forced_bos_token_id < 0:
        raise ValueError(f"Unsupported target language token: {target_language}")

    translated_tokens = model.generate(
        **inputs,
        forced_bos_token_id=forced_bos_token_id,
        max_length=MAX_LENGTH,
    )
    return tokenizer.batch_decode(translated_tokens, skip_special_tokens=True)[0]


logger.info("Direct translation path ready.")

# ── Warm-up: run a tiny inference so the first real call is fast ──────────────
logger.info("Running warm-up inference to pre-JIT the model...")
try:
    _translate_text("hello", "eng_Latn", "amh_Ethi")
    logger.info("Warm-up complete.")
except Exception as _warm_ex:
    logger.warning("Warm-up inference failed (non-fatal): %s", _warm_ex)

# ── Flask app ─────────────────────────────────────────────────────────────────
app = Flask(__name__)


@app.route("/health", methods=["GET"])
def health_check():
    """Simple health-check endpoint."""
    return jsonify({"status": "ok", "model": MODEL_NAME}), 200


@app.route("/warmup", methods=["POST"])
def warmup():
    """
    Runs a tiny translation to ensure the model is hot before the first real request.
    Called by the Spring Boot application at startup (ApplicationReadyEvent).
    """
    try:
        _translate_text("hello", "eng_Latn", "amh_Ethi")
        return jsonify({"status": "warm"}), 200
    except Exception as ex:
        logger.warning("Warmup request failed: %s", ex)
        return jsonify({"status": "warm", "note": str(ex)}), 200


@app.route("/translate", methods=["POST"])
def translate():
    """
    Translates text between supported language pairs using NLLB-200.

    Expected JSON body:
        {
            "text": "...",
            "source_language": "amh_Ethi",
            "target_language": "eng_Latn"
        }
    """
    data = request.get_json(silent=True)
    if not data:
        logger.warning("Empty or invalid JSON payload received.")
        return jsonify({"error": "Request body must be JSON."}), 400

    text            = data.get("text", "").strip()
    source_language = data.get("source_language", "").strip()
    target_language = data.get("target_language", "").strip()

    # ── Validate ──────────────────────────────────────────────────────────────
    if not text:
        return jsonify({"error": "Field 'text' must not be empty."}), 400
    if not source_language or not target_language:
        return jsonify({"error": "'source_language' and 'target_language' are required."}), 400

    language_pair = (source_language, target_language)
    if language_pair not in SUPPORTED_LANGUAGE_PAIRS:
        return jsonify({
            "error": f"Unsupported language pair: {source_language} -> {target_language}. "
                     f"Supported pairs: {list(SUPPORTED_LANGUAGE_PAIRS)}"
        }), 400

    # ── Translate ─────────────────────────────────────────────────────────────
    logger.info("Translating [%s -> %s]: %.80s...", source_language, target_language, text)
    try:
        translated_text = _translate_text(text, source_language, target_language)
        logger.info("Translation complete: %.80s...", translated_text)
        return jsonify({"translated_text": translated_text}), 200

    except Exception as ex:
        logger.error("Translation failed: %s", str(ex), exc_info=True)
        return jsonify({"error": f"Translation error: {str(ex)}"}), 500


# ── Entry point ───────────────────────────────────────────────────────────────
if __name__ == "__main__":
    logger.info("Starting NLLB Translation Server on %s:%d", SERVER_HOST, SERVER_PORT)
    app.run(host=SERVER_HOST, port=SERVER_PORT, debug=False)

