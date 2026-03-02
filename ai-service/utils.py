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
    """
    Fetch user portfolio data from tracker service.
    Returns None if fetch fails instead of raising exception.
    """
    async with httpx.AsyncClient(timeout=60.0) as client:
        try:
            response = await client.get(
                f"{os.getenv('TRACKER_SERVICE_URL')}/api/portfolio/all",
                headers=headers,
                params={"userId": user_id}
            )
            
            if response.status_code == 200:
                return response.json()
            else:
                print(f"Portfolio fetch failed with status {response.status_code}: {response.text}")
                return None
                
        except httpx.ReadTimeout:
            print("Portfolio service timeout")
            return None
        except httpx.ConnectError:
            print("Portfolio service unavailable")
            return None
        except Exception as e:
            print(f"Portfolio fetch error: {str(e)}")
            return None