from unittest.mock import AsyncMock, MagicMock, patch
from app.fetcher import ProxyFetcher, build_headers
from app.models import FetchRequest
from app.rotator import ProxyRotator, Proxy
import pytest

def make_rotator_with_proxy():
    r = ProxyRotator.__new__(ProxyRotator)
    r.proxies = [Proxy(address="testproxy:8080")]
    return r

def test_build_headers_ebay():
    h = build_headers("ebay")
    assert "Referer" in h
    assert "User-Agent" in h

def test_build_headers_default():
    h = build_headers(None)
    assert "User-Agent" in h

@pytest.mark.asyncio
async def test_fetch_success():
    rotator = make_rotator_with_proxy()
    fetcher = ProxyFetcher(rotator)
    request = FetchRequest(url="https://www.ebay.com/sch/i.html?_nkw=iphone", retailer="ebay", max_retries=1)

    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.text = "<html>result</html>"

    with patch("httpx.AsyncClient") as mock_client_cls:
        mock_client = AsyncMock()
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=False)
        mock_client.get = AsyncMock(return_value=mock_response)
        mock_client_cls.return_value = mock_client

        result = await fetcher.fetch(request)

    assert result.success
    assert result.html == "<html>result</html>"
    assert result.attempts == 1

@pytest.mark.asyncio
async def test_fetch_retries_on_403():
    rotator = make_rotator_with_proxy()
    fetcher = ProxyFetcher(rotator)
    request = FetchRequest(url="https://www.ebay.com/sch/i.html?_nkw=iphone", retailer="ebay", max_retries=2)

    mock_403 = MagicMock()
    mock_403.status_code = 403
    mock_403.text = ""

    with patch("httpx.AsyncClient") as mock_client_cls:
        mock_client = AsyncMock()
        mock_client.__aenter__ = AsyncMock(return_value=mock_client)
        mock_client.__aexit__ = AsyncMock(return_value=False)
        mock_client.get = AsyncMock(return_value=mock_403)
        mock_client_cls.return_value = mock_client

        with patch("asyncio.sleep", new_callable=AsyncMock):
            result = await fetcher.fetch(request)

    assert not result.success
    assert result.attempts == 2