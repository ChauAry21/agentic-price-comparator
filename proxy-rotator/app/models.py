from pydantic import BaseModel
from typing import Optional

class FetchRequest(BaseModel):
    url: str
    retailer: Optional[str] = None
    timeout: int = 15
    max_retries: int = 3

class FetchResponse(BaseModel):
    success: bool
    html: Optional[str] = None
    status_code: Optional[int] = None
    proxy_used: Optional[str] = None
    attempts: int = 0
    error: Optional[str] = None

class ProxyStatus(BaseModel):
    address: str
    healthy: bool
    failures: int
    last_used: Optional[str] = None
    success_rate: float