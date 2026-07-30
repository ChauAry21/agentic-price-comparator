from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from app.models import FetchRequest, FetchResponse, ProxyStatus
from app.rotator import ProxyRotator
from app.fetcher import ProxyFetcher
from dotenv import load_dotenv
import logging

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="PriceHawk Proxy Rotator", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

rotator = ProxyRotator()
fetcher = ProxyFetcher(rotator)


@app.get("/health")
async def health():
    return {"status": "ok", "proxies_available": rotator.available_count()}


@app.post("/fetch", response_model=FetchResponse)
async def fetch(request: FetchRequest):
    logger.info(f"Fetch request for: {request.url}")
    result = await fetcher.fetch(request)
    if not result.success:
        raise HTTPException(status_code=502, detail=result.error)
    return result


@app.get("/proxies/status", response_model=list[ProxyStatus])
async def proxy_status():
    return rotator.get_status()


@app.post("/proxies/reload")
async def reload_proxies():
    rotator.reload()
    return {"message": "Proxies reloaded", "count": rotator.available_count()}