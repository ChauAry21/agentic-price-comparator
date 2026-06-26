import os
import random
import logging
from datetime import datetime, timezone
from dataclasses import dataclass, field
from typing import Optional
from app.models import ProxyStatus

logger = logging.getLogger(__name__)

MAX_FAILURES = 5

@dataclass
class Proxy:
    address: str
    failures: int = 0
    successes: int = 0
    last_used: Optional[datetime] = None
    healthy: bool = True

    def record_success(self):
        self.successes += 1
        self.last_used = datetime.now(timezone.utc)
        self.healthy = True

    def record_failure(self):
        self.failures += 1
        self.last_used = datetime.now(timezone.utc)
        if self.failures >= MAX_FAILURES:
            self.healthy = False
            logger.warning(f"Proxy {self.address} marked unhealthy after {self.failures} failures")

    @property
    def success_rate(self) -> float:
        total = self.successes + self.failures
        if total == 0:
            return 1.0
        return self.successes / total

class ProxyRotator:
    def __init__(self):
        self.proxies: list[Proxy] = []
        self.reload()

    def reload(self):
        raw = os.environ.get("PROXY_LIST", "")
        addresses = [p.strip() for p in raw.split(",") if p.strip()]
        self.proxies = [Proxy(address=addr) for addr in addresses]
        logger.info(f"Loaded {len(self.proxies)} proxies")

    def get_next(self) -> Optional[Proxy]:
        healthy = [p for p in self.proxies if p.healthy]
        if not healthy:
            return None
        return random.choice(healthy)

    def available_count(self) -> int:
        return len([p for p in self.proxies if p.healthy])

    def get_status(self) -> list[ProxyStatus]:
        return [
            ProxyStatus(
                address=p.address,
                healthy=p.healthy,
                failures=p.failures,
                last_used=p.last_used.isoformat() if p.last_used else None,
                success_rate=p.success_rate,
            )
            for p in self.proxies
        ]