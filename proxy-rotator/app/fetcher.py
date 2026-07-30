import asyncio
import logging
import random
from typing import Optional
import httpx
from app.models import FetchRequest, FetchResponse
from app.rotator import ProxyRotator, Proxy

logger = logging.getLogger(__name__)

USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
]

RETAILER_HEADERS = {
    "ebay": {
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.9",
        "Accept-Encoding": "gzip, deflate, br",
        "Connection": "keep-alive",
        "Upgrade-Insecure-Requests": "1",
        "Referer": "https://www.google.com",
        "Sec-Fetch-Dest": "document",
        "Sec-Fetch-Mode": "navigate",
        "Sec-Fetch-Site": "cross-site",
    },
    "amazon": {
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.5",
        "Accept-Encoding": "gzip, deflate, br",
        "Connection": "keep-alive",
        "Upgrade-Insecure-Requests": "1",
    },
    "walmart": {
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.5",
        "Accept-Encoding": "gzip, deflate, br",
        "Connection": "keep-alive",
    },
    "default": {
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.5",
        "Accept-Encoding": "gzip, deflate, br",
        "Connection": "keep-alive",
    },
}

def build_headers(retailer: Optional[str]) -> dict:
    key = (retailer or "default").lower()
    base = RETAILER_HEADERS.get(key, RETAILER_HEADERS["default"]).copy()
    base["User-Agent"] = random.choice(USER_AGENTS)
    return base

def proxy_url(proxy: Optional[Proxy]) -> Optional[str]:
    if proxy is None:
        return None
    addr = proxy.address
    if addr.startswith("http"):
        return addr
    return f"http://{addr}"

class ProxyFetcher:
    def __init__(self, rotator: ProxyRotator):
        self.rotator = rotator

    async def fetch(self, request: FetchRequest) -> FetchResponse:
        attempts = 0
        last_error = None

        for attempt in range(request.max_retries):
            attempts += 1
            proxy = self.rotator.get_next()
            proxy_addr = proxy.address if proxy else "direct"
            logger.info(f"Attempt {attempts} for {request.url} via {proxy_addr}")

            try:
                result = await self._do_fetch(request, proxy)
                if result.success:
                    if proxy:
                        proxy.record_success()
                    result.attempts = attempts
                    return result

                last_error = result.error
                if proxy and result.status_code in (403, 429, 503):
                    proxy.record_failure()

            except Exception as e:
                last_error = str(e)
                if proxy:
                    proxy.record_failure()
                logger.warning(f"Attempt {attempts} failed: {e}")

            if attempt < request.max_retries - 1:
                await asyncio.sleep(1.5 * (attempt + 1))

        return FetchResponse(
            success=False,
            attempts=attempts,
            error=last_error or "All retries exhausted",
        )

    async def _do_fetch(self, request: FetchRequest, proxy: Optional[Proxy]) -> FetchResponse:
        headers = build_headers(request.retailer)
        proxies = {"http://": proxy_url(proxy), "https://": proxy_url(proxy)} if proxy else None

        async with httpx.AsyncClient(
            headers=headers,
            proxies=proxies,
            timeout=request.timeout,
            follow_redirects=True,
            verify=False,
        ) as client:
            response = await client.get(request.url)
            success = response.status_code == 200

            return FetchResponse(
                success=success,
                html=response.text if success else None,
                status_code=response.status_code,
                proxy_used=proxy.address if proxy else "direct",
                error=None if success else f"HTTP {response.status_code}",
            )