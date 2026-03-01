import os
import httpx
import json
import urllib.parse
from dotenv import load_dotenv
from google import genai
from config import NEWS_SERVICE_URL, NEWS_API_KEY, GEMINI_API_KEY

load_dotenv()

def log_debug(message):
    print(message)
    with open("debug.log", "a", encoding="utf-8") as f:
        f.write(message + "\n")

_genai_client = None

def get_genai_client():
    global _genai_client
    if _genai_client is None:
        if not GEMINI_API_KEY:
            raise ValueError("GEMINI_API_KEY not found in environment")
        _genai_client = genai.Client(api_key=GEMINI_API_KEY)
    return _genai_client

SYSTEM_INSTRUCTION = """
You are a top-tier financial analyst specializing in the Indian stock market.
Your goal is to provide concise, accurate, and insightful summaries of stock performance, company news, and market trends.
Always prioritize data from the provided internal database. If the database news is empty or outdated, use your internal knowledge to provide a general market outlook but mention that you are using general information.
Keep your tone professional and your answers extremely brief (avoid long introductions or conclusions).
"""

async def extract_entities(prompt):
    client = get_genai_client()
    extraction_prompt = f"""
    Analyze the following user query and extract:
    1. A list of specific stock names or company names to search for in our database.
    2. The primary intent: 'stock_research', 'portfolio_analysis', or 'general_chat'.
    3. Whether the user's portfolio data is needed to answer this query.

    Query: "{prompt}"

    Return the result ONLY as a JSON object with keys: "stocks_to_search", "intent", "needs_portfolio", "planning_reasoning".
    """
    models_to_try = ["gemini-2.0-flash", "gemini-2.5-flash", "gemini-3-flash", "gemini-2.5-flash-lite"]
    for model_name in models_to_try:
        try:
            log_debug(f"[STEP 1] Generating Execution Plan for: '{prompt[:50]}...' using {model_name}")
            response = client.models.generate_content(
                model=model_name,
                contents=extraction_prompt,
                config={'response_mime_type': 'application/json'}
            )
            plan = json.loads(response.text)
            return plan
        except Exception as e:
            log_debug(f"  - [WARNING] Planning with {model_name} failed: {e}")
            continue
    log_debug("  - [ERROR] All planning models failed.")
    return {"stocks_to_search": [], "intent": "general_chat", "needs_portfolio": False, "planning_reasoning": "Fallback to safety."}

async def search_stock_links(query):
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            log_debug(f"[STEP 2] Searching SheetNews for stock: '{query}'")
            response = await client.get(
                f"{NEWS_SERVICE_URL}/api/stock-links/search",
                headers={"x-api-key": NEWS_API_KEY},
                params={"q": query}
            )
            if response.status_code == 200:
                results = response.json().get("data", [])
                return results
            else:
                log_debug(f"  - [ERROR] Search API returned {response.status_code}")
        except Exception as e:
            print(f"  - [ERROR] Search failed for '{query}': {e}")
    return []

async def refine_stock_selection(matched_links, original_prompt):
    if not matched_links:
        return None
    client = get_genai_client()
    refine_prompt = f"""
    Based on the user's original query: "{original_prompt}"
    Select the MOST relevant stock from the following candidate list from our database:
    {json.dumps(matched_links, indent=2)}

    Return ONLY a JSON object with the "name" of the best matching entry.
    """
    try:
        response = client.models.generate_content(
            model="gemini-2.0-flash",
            contents=refine_prompt,
            config={'response_mime_type': 'application/json'}
        )
        result = json.loads(response.text)
        selected_name = result.get("name")
        for link in matched_links:
            if link.get("name") == selected_name:
                return link
        return None
    except Exception as e:
        print(f"  - [ERROR] Refinement failed: {e}")
        return None

async def fetch_stock_news_by_id(spreadsheet_id, gid=None):
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            url = f"{NEWS_SERVICE_URL}/api/spreadsheet-news/{spreadsheet_id}"
            params = {}
            if gid:
                params["gid"] = gid
            response = await client.get(
                url,
                headers={"x-api-key": NEWS_API_KEY},
                params=params
            )
            if response.status_code == 200:
                return response.json().get("data", [])
        except Exception as e:
            print(f"  - [ERROR] News fetch failed for ID '{spreadsheet_id}': {e}")
    return []

async def fetch_stock_news(stock_name):
    encoded_name = urllib.parse.quote(stock_name)
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            url = f"{NEWS_SERVICE_URL}/api/sheet-news/{encoded_name}"
            response = await client.get(
                url,
                headers={"x-api-key": NEWS_API_KEY}
            )
            if response.status_code == 200:
                data = response.json().get("data", {})
                return data.get("data", [])
        except Exception as e:
            print(f"News fetch error for '{stock_name}': {e}")
    return []

def build_prompt(portfolio_data, news_context, user_prompt, intent="stock_research"):
    analysis_instructions = ""
    news_section = ""
    if intent == "general_chat":
        return f"{SYSTEM_INSTRUCTION}\n\nUser Question: {user_prompt}"
    if news_context:
        for stock, news in news_context.items():
            recent_news = news[:10] if isinstance(news, list) else news
            news_section += f"\nINTERNAL DATABASE NEWS FOR {stock}:\n{json.dumps(recent_news, indent=2)}\n"
    else:
        news_section = "NO RECENT DATA FOUND IN INTERNAL DATABASE."

    if intent == "stock_research":
        analysis_instructions = "Summarize the recent news for the stocks mentioned. Provide key sentiments and outlook."
    elif intent == "portfolio_analysis":
        analysis_instructions = f"Analyze the following portfolio data in the context of current news:\n{json.dumps(portfolio_data, indent=2)}"

    final_prompt = f"""
    {SYSTEM_INSTRUCTION}

    CONTEXTUAL DATA:
    {news_section}

    USER QUERY: {user_prompt}

    {analysis_instructions}

    Provide a concise summary.
    """
    log_debug(f"DEBUG: Final Prompt (first 300 chars): {final_prompt[:300]}")
    return final_prompt
