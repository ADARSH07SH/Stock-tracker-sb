from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel
import httpx
import jwt
import os
from dotenv import load_dotenv
from google import genai
from openai import OpenAI

from utils import extract_user_id, fetch_portfolio
from rag_engine import extract_entities, search_stock_links, refine_stock_selection, fetch_stock_news_by_id, build_prompt
from config import GROQ_API_KEY, GROQ_FALLBACK_MODELS

from fastapi.middleware.cors import CORSMiddleware
import uvicorn

load_dotenv()

def log_debug(message):
    print(message)
    with open("debug.log", "a", encoding="utf-8") as f:
        f.write(message + "\n")

app = FastAPI()


app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

genai_client = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))
MODELS = ["gemini-2.0-flash", "gemini-2.5-flash", "gemini-3-flash", "gemini-2.5-flash-lite"]

groq_client = OpenAI(
    api_key=GROQ_API_KEY or "your_groq_api_key",
    base_url="https://api.groq.com/openai/v1",
)

openrouter_client = OpenAI(
    base_url="https://openrouter.ai/api/v1",
    api_key=os.getenv("OPENROUTER_API_KEY")
)


async def check_output_relevance(output: str) -> str:
    """Level 2 Moderation: Checks if the generated output is truly about stocks/finance."""
    log_debug("\n[PHASE 4] Level 2 Moderation (Output Check)")
    
    moderation_prompt = f"""
    Evaluate if the following AI response is about stocks, investments, financial news, or portfolio management.
    If it is relevant, return 'PASS'. 
    If it is NOT relevant or attempts to answer off-topic questions, return 'FAIL'.

    AI Response: "{output[:500]}"
    
    Return ONLY 'PASS' or 'FAIL'.
    """
    
    try:
        # Use the fastest model for moderation
        response = genai_client.models.generate_content(
            model="gemini-2.0-flash",
            contents=moderation_prompt
        )
        result = response.text.strip().upper()
        log_debug(f" -> Level 2 Result: {result}")
        
        if "PASS" in result:
            return output
        else:
            return "I cannot answer this. I can only help with questions related to stock assets and financial markets."
    except Exception as e:
        log_debug(f" -> [WARNING] Level 2 Moderation failed: {e}. Defaulting to PASS for availability.")
        return output


class ChatRequest(BaseModel):
    prompt: str


class ImageGenerationRequest(BaseModel):
    prompt: str
    title: str = None


@app.post("/chat")
async def chat(request: ChatRequest, authorization: str = Header(None)):
    log_debug("\n" + "="*80)
    log_debug(f"ORCHESTRATOR: New request received: '{request.prompt[:60]}...'")
    log_debug("="*80)

    
    log_debug("\n[PHASE 1] LLM-Driven Planning & Security")
    user_id = extract_user_id(authorization)
    log_debug(f" -> Authenticated User ID: {user_id}")
    headers = {"Authorization": authorization}
    
    execution_plan = await extract_entities(request.prompt)
    log_debug(f" -> Plan: {execution_plan}")
    
    is_relevant = execution_plan.get("is_relevant", True)
    is_safe = execution_plan.get("is_safe", True)
    
    if not is_relevant or not is_safe:
        log_debug(f" -> [REJECTED] Level 1 Moderation: relevant={is_relevant}, safe={is_safe}")
        return {
            "answer": "I cannot answer this. I can only help with questions related to stock assets and financial markets.",
            "model": "moderator_v1",
            "researched_stocks": []
        }

    intent = execution_plan.get("intent", "general_chat")
    stocks_to_search = execution_plan.get("stocks_to_search", [])
    needs_portfolio = execution_plan.get("needs_portfolio", False)
    
    
    if intent == "portfolio_analysis":
        needs_portfolio = True
    
    
    log_debug("\n[PHASE 2] Executing Intelligence Plan")
    portfolio_data = None
    if needs_portfolio:
        log_debug(" -> Action: Fetching user portfolio data...")
        portfolio_data = await fetch_portfolio(headers, user_id)
        if portfolio_data and isinstance(portfolio_data, list):
            total_stocks = sum(len(p.get('stocks', [])) for p in portfolio_data)
            log_debug(f" -> Result: Loaded {total_stocks} stocks across {len(portfolio_data)} accounts.")
            if len(portfolio_data) > 0:
                log_debug(f" -> DEBUG: Sample Account Name: {portfolio_data[0].get('accountName')}")
                log_debug(f" -> DEBUG: Sample Account Stocks Count: {len(portfolio_data[0].get('stocks', []))}")
        elif portfolio_data == []:
            log_debug(f" -> Result: portfolio_data is an EMPTY LIST []. User has no accounts.")
        elif portfolio_data is None:
            log_debug(f" -> Result: Failed to fetch portfolio data. Continuing without it.")
        else:
            log_debug(f" -> Result: portfolio_data is NOT a list: {type(portfolio_data)}")

    news_context = {}
    if stocks_to_search:
        log_debug(f" -> Action: Researching news for {len(stocks_to_search)} stocks...")
        for query in stocks_to_search:
            log_debug(f"    - Searching for: '{query}'")
            candidates = await search_stock_links(query)
            if candidates:
                log_debug(f"    - Found {len(candidates)} candidate sheets. Refining...")
                best_match = await refine_stock_selection(candidates, request.prompt)
                if best_match:
                    name = best_match.get("name")
                    log_debug(f"    - Best Sheet Selected: '{name}'")
                    news_rows = await fetch_stock_news_by_id(
                        best_match.get("spreadsheetId"),
                        best_match.get("gid")
                    )
                    news_context[name] = news_rows
                else:
                    log_debug(f"    - No sheet deemed relevant by LLM for '{query}'.")
            else:
                log_debug(f"    - No candidates found in database for '{query}'.")
    else:
        log_debug(" -> Action: No news research required for this intent.")

    
    log_debug("\n[PHASE 3] Response Synthesis & Delivery")
    context = build_prompt(
        portfolio_data,
        news_context,
        request.prompt,
        intent=intent
    )

    
    for model in MODELS:
        try:
            log_debug(f" -> Attempting generation with {model}...")
            response = genai_client.models.generate_content(
                model=model,
                contents=context
            )
            # Level 2 Moderation: Post-generation check
            moderated_answer = await check_output_relevance(response.text)
            
            log_debug(" -> [SUCCESS] Response delivered.")
            log_debug("="*80)
            return {
                "answer": moderated_answer,
                "model": model,
                "researched_stocks": list(news_context.keys())
            }
        except Exception as e:
            log_debug(f" -> [WARNING] {model} failed: {e}")
            continue

    log_debug(" -> [WARNING] All Gemini models failed. Trying Groq fallback models...")
    for model in GROQ_FALLBACK_MODELS:
        try:
            log_debug(f" -> Attempting generation with Groq model {model}...")
            response = groq_client.chat.completions.create(
                model=model,
                messages=[
                    {"role": "user", "content": context}
                ]
            )
            # Level 2 Moderation: Post-generation check
            moderated_answer = await check_output_relevance(response.choices[0].message.content)

            log_debug(f" -> [SUCCESS] Groq Response delivered using {model}.")
            log_debug("="*80)
            return {
                "answer": moderated_answer,
                "model": model,
                "researched_stocks": list(news_context.keys())
            }
        except Exception as e:
            log_debug(f" -> [WARNING] Groq {model} failed: {e}")
            continue

    log_debug(" -> [CRITICAL] All synthesis models failed.")
    log_debug("="*80)
    raise HTTPException(status_code=500, detail="Orchestration failed at synthesis phase.")


@app.get("/health")
async def health():
    return {"status": "healthy"}


@app.post("/generate-image")
async def generate_image(request: ImageGenerationRequest, authorization: str = Header(None)):
    
    log_debug("\n" + "="*80)
    log_debug(f"IMAGE GENERATION: New request received")
    log_debug(f" -> Prompt: '{request.prompt}'")
    if request.title:
        log_debug(f" -> Article Title: '{request.title}'")
    log_debug("="*80)
    
    try:
        user_id = extract_user_id(authorization)
        log_debug(f" -> Authenticated User ID: {user_id}")
    except Exception as e:
        log_debug(f" -> [ERROR] Authentication failed: {str(e)}")
        raise HTTPException(status_code=401, detail="Unauthorized")
    
    openrouter_key = os.getenv("OPENROUTER_API_KEY")
    if not openrouter_key or openrouter_key == "your_openrouter_api_key_here":
        log_debug(" -> [WARNING] OpenRouter API key not configured, using fallback")

        import hashlib
        seed = hashlib.md5(request.prompt.encode()).hexdigest()[:10]
        image_url = f"https://picsum.photos/seed/{seed}/1600/900"
        
        log_debug(f" -> Fallback Image URL: {image_url}")
        log_debug("="*80)
        
        return {
            "success": True,
            "imageUrl": image_url,
            "prompt": request.prompt,
            "fallback": True
        }
    
    try:

        enhanced_prompt = request.prompt
        if request.title:
            enhanced_prompt = f"Create a professional, high-quality image for a news article titled '{request.title}'. {request.prompt}. Style: professional, modern, suitable for financial news."
        
        log_debug(f" -> Enhanced Prompt: '{enhanced_prompt}'")
        log_debug(" -> Calling OpenRouter API for AI image generation...")
        
        response = openrouter_client.chat.completions.create(
            model="sourceful/riverflow-v2-pro",
            messages=[
                {
                    "role": "user",
                    "content": enhanced_prompt
                }
            ],
            extra_body={
                "modalities": ["image"]
            }
        )
        

        message = response.choices[0].message
        
        log_debug(f" -> Response received, checking for images...")
        
        if hasattr(message, 'images') and message.images:

            image_data = message.images[0]
            

            if isinstance(image_data, dict):
                image_url = image_data.get('image_url', {}).get('url')
            else:
                image_url = image_data.image_url.url if hasattr(image_data, 'image_url') else None
            
            if image_url:
                log_debug(f" -> [SUCCESS] AI Image generated: {image_url[:100]}...")
                log_debug("="*80)
                
                return {
                    "success": True,
                    "imageUrl": image_url,
                    "prompt": enhanced_prompt,
                    "fallback": False
                }
        
        log_debug(" -> [ERROR] No image in response")
        log_debug(f" -> Response message: {message}")
        log_debug("="*80)
        raise HTTPException(status_code=500, detail="No image generated by AI")
            
    except Exception as e:
        log_debug(f" -> [ERROR] AI image generation failed: {str(e)}")
        log_debug(f" -> Error type: {type(e).__name__}")
        log_debug("="*80)
        

        import hashlib
        seed = hashlib.md5(request.prompt.encode()).hexdigest()[:10]
        image_url = f"https://picsum.photos/seed/{seed}/1600/900"
        
        log_debug(f" -> Using fallback image: {image_url}")
        
        return {
            "success": True,
            "imageUrl": image_url,
            "prompt": request.prompt,
            "fallback": True,
            "error": str(e)
        }


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8084, reload=True)
