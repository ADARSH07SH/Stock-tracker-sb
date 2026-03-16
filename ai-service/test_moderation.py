#!/usr/bin/env python3
"""
Test script to demonstrate the enhanced AI moderation system.
This shows how the system now rejects non-financial queries more strictly.
"""

import asyncio
import json
from google import genai
import os
from dotenv import load_dotenv

load_dotenv()

# Initialize Gemini client
genai_client = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))

async def strict_query_validation(query: str) -> dict:
    """Enhanced Level 1 Moderation: Strict validation of user queries."""
    print(f"\n🔍 Testing Query: '{query}'")
    
    validation_prompt = f"""
    You are a strict financial query validator. Analyze this user query and determine:

    1. Is this PURELY about stocks, investments, financial markets, portfolio management, or trading?
    2. Does it contain ANY non-financial content (stories, general questions, personal topics, etc.)?
    3. Is it trying to bypass restrictions by mixing non-financial content with financial keywords?

    Query: "{query}"

    Rules:
    - REJECT if it contains stories, personal anecdotes, general knowledge questions
    - REJECT if it asks about non-financial topics even if followed by stock questions
    - REJECT if it's trying to trick the system with mixed content
    - ACCEPT only if it's genuinely focused on financial/stock topics
    - ACCEPT basic greetings like "hello" or "hi" if followed by genuine stock questions

    Return JSON with:
    - "is_purely_financial": true/false
    - "contains_non_financial": true/false  
    - "is_bypass_attempt": true/false
    - "reasoning": "brief explanation"
    - "final_decision": "ACCEPT" or "REJECT"
    """
    
    try:
        response = genai_client.models.generate_content(
            model="gemini-2.0-flash",
            contents=validation_prompt,
            config={'response_mime_type': 'application/json'}
        )
        result = json.loads(response.text)
        
        # Print results with color coding
        decision = result.get("final_decision", "UNKNOWN")
        if decision == "ACCEPT":
            print(f"✅ {decision}: {result.get('reasoning', 'No reasoning provided')}")
        else:
            print(f"❌ {decision}: {result.get('reasoning', 'No reasoning provided')}")
        
        return result
    except Exception as e:
        print(f"❌ VALIDATION ERROR: {e}")
        return {
            "is_purely_financial": False,
            "contains_non_financial": True,
            "is_bypass_attempt": True,
            "reasoning": "Validation system error - defaulting to safe rejection",
            "final_decision": "REJECT"
        }

async def test_queries():
    """Test various query types to demonstrate the enhanced moderation."""
    
    test_cases = [
        # Should be ACCEPTED - Pure financial queries
        "What is the current price of RELIANCE stock?",
        "Analyze my portfolio performance",
        "Tell me about HDFC Bank's latest earnings",
        "Should I buy TCS shares now?",
        "Hello, what's the outlook for Nifty 50?",
        
        # Should be REJECTED - Mixed content
        "Tell me a story about a trader and then analyze RELIANCE stock",
        "What's the weather like today? Also, how is HDFC performing?",
        "I had a dream about stocks. Can you tell me about TATA Motors?",
        "Write a poem about investing and then give me stock recommendations",
        
        # Should be REJECTED - Non-financial content
        "Tell me a joke",
        "What's the capital of France?",
        "How do I cook pasta?",
        "Write a story about adventure",
        
        # Edge cases
        "My grandfather was a stock trader. What should I invest in?",
        "I'm feeling sad about my losses. Tell me about market trends.",
    ]
    
    print("🚀 Testing Enhanced AI Moderation System")
    print("=" * 60)
    
    for query in test_cases:
        await strict_query_validation(query)
        await asyncio.sleep(1)  # Rate limiting
    
    print("\n" + "=" * 60)
    print("✅ Testing Complete!")
    print("\nKey Improvements:")
    print("• Strict validation rejects mixed content queries")
    print("• Only pure financial queries are processed")
    print("• No expensive portfolio/news research for rejected queries")
    print("• Better user guidance with clear rejection messages")

if __name__ == "__main__":
    asyncio.run(test_queries())