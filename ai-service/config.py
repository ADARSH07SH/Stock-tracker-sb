import os
from dotenv import load_dotenv
from google import genai

load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
TRACKER_SERVICE_URL = os.getenv("TRACKER_SERVICE_URL")
NEWS_SERVICE_URL = os.getenv("NEWS_SERVICE_URL")
NEWS_API_KEY = os.getenv("NEWS_API_KEY")
GROQ_API_KEY = os.getenv("GROQ_API_KEY")

genai_client = genai.Client(api_key=GEMINI_API_KEY)

MODEL_PRIMARY = "gemini-2.0-flash"
MODEL_BACKUP = "gemini-2.5-flash"
GROQ_FALLBACK_MODELS = [
    "llama-3.3-70b-versatile",
    "qwen/qwen3-32b",
    "openai/gpt-oss-120b",
    "meta-llama/llama-4-scout-17b-16e-instruct",
    "meta-llama/llama-4-maverick-17b-128e-instruct",
    "openai/gpt-oss-20b",
    "groq/compound",
    "moonshotai/kimi-k2-instruct",
    "llama-3.1-8b-instant",
    "allam-2-7b"
]
