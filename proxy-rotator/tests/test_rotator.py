import pytest
from app.rotator import ProxyRotator, Proxy, MAX_FAILURES

def test_no_proxies_returns_none(monkeypatch):
    monkeypatch.setenv("PROXY_LIST", "")
    r = ProxyRotator()
    assert r.get_next() is None
    assert r.available_count() == 0

def test_loads_proxies_from_env(monkeypatch):
    monkeypatch.setenv("PROXY_LIST", "proxy1:8080,proxy2:8080")
    r = ProxyRotator()
    assert r.available_count() == 2

def test_unhealthy_proxy_excluded(monkeypatch):
    monkeypatch.setenv("PROXY_LIST", "bad:8080,good:8080")
    r = ProxyRotator()
    r.proxies[0].healthy = False
    for _ in range(20):
        p = r.get_next()
        assert p is not None
        assert p.address == "good:8080"

def test_proxy_marked_unhealthy_after_max_failures():
    p = Proxy(address="test:8080")
    for _ in range(MAX_FAILURES):
        p.record_failure()
    assert not p.healthy

def test_success_rate_calculation():
    p = Proxy(address="test:8080")
    p.record_success()
    p.record_success()
    p.record_failure()
    assert abs(p.success_rate - 2/3) < 0.01

def test_success_resets_healthy():
    p = Proxy(address="test:8080")
    for _ in range(MAX_FAILURES):
        p.record_failure()
    assert not p.healthy
    p.record_success()
    assert p.healthy