import jwt
import httpx
import os
from dotenv import load_dotenv
from fastapi import HTTPException

load_dotenv()

def extract_user_id(authorization: str):
    if not authorization:
        raise HTTPException(status_code=401, detail="Missing Authorization header")

    token = authorization.replace("Bearer ", "")
    decoded = jwt.decode(token, options={"verify_signature": False})
    user_id = decoded.get("sub") or decoded.get("userId") or decoded.get("id")
    if not user_id:
        raise HTTPException(status_code=401, detail="Invalid token")

    return user_id


async def fetch_portfolio(headers, user_id):
    async with httpx.AsyncClient(timeout=60.0) as client:
        try:
            response = await client.get(
                f"{os.getenv('TRACKER_SERVICE_URL')}/api/portfolio/all",
                headers=headers,
                params={"userId": user_id}
            )
        except httpx.ReadTimeout:
            raise HTTPException(status_code=504, detail="Portfolio service timeout")
        except httpx.ConnectError:
            raise HTTPException(status_code=503, detail="Portfolio service unavailable")

    if response.status_code != 200:
        raise HTTPException(status_code=response.status_code, detail=f"Portfolio fetch failed: {response.text}")

    return response.json()