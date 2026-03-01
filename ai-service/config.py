import os
from dotenv import load_dotenv
from google import genai

load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
TRACKER_SERVICE_URL = os.getenv("TRACKER_SERVICE_URL")
NEWS_SERVICE_URL = os.getenv("NEWS_SERVICE_URL")
NEWS_API_KEY = os.getenv("NEWS_API_KEY")

genai_client = genai.Client(api_key=GEMINI_API_KEY)

MODEL_PRIMARY = "gemini-2.0-flash"
MODEL_BACKUP = "gemini-2.5-flash"
