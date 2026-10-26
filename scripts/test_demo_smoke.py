"""Offline tests for the read-only hosted smoke check."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch
import urllib.error
import urllib.request

spec = importlib.util.spec_from_file_location("demo_smoke", Path(__file__).with_name("demo-smoke.py"))
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)


class DemoSmokeTest(unittest.TestCase):
    def test_only_accepts_final_https_origin(self):
        for url in ("http://example.com", "https://user:pass@example.com",
                    "https://example.com/swagger-ui", "https://example.com?secret=x",
                    "https://example.com#fragment", "https://"):
            with self.subTest(url=url), self.assertRaises(ValueError):
                smoke.validate_url(url)
        self.assertEqual("https://example.com", smoke.validate_url("https://example.com/"))

    def test_refuses_redirect_even_to_another_https_origin(self):
        with self.assertRaises(RuntimeError):
            smoke.NoRedirect().redirect_request(None, None, 302, "", {}, "https://other.example.com")

    def test_refuses_missing_credentials_before_network(self):
        with patch.object(smoke.urllib.request, "build_opener") as opener:
            with self.assertRaises(ValueError):
                smoke.verify("https://example.com", "", "unused")
            opener.assert_not_called()

    def test_verifies_expected_responses_with_get_requests_only(self):
        calls = []
        class Response:
            status = 200
            def __init__(self, body):
                self.body = body
            def __enter__(self):
                return self
            def __exit__(self, *args):
                return False
            def read(self):
                return self.body

        class Client:
            def open(self, req, timeout):
                calls.append(req)
                path = req.full_url.split("example.com", 1)[1]
                authenticated = req.has_header("Authorization")
                if path in ("/actuator/health/db", "/actuator/env"):
                    raise urllib.error.HTTPError(req.full_url, 403, "", {}, None)
                if path == "/users" and not authenticated:
                    raise urllib.error.HTTPError(req.full_url, 401, "", {}, None)
                if path == "/actuator/health":
                    return Response(b'{"status":"UP"}')
                if path == "/v3/api-docs":
                    return Response(b'{"openapi":"3.1.0"}')
                return Response(b"{}")

        with patch.object(smoke.urllib.request, "build_opener", return_value=Client()):
            smoke.verify("https://example.com", "reader", "test-only-password")
        self.assertEqual(8, len(calls))
        self.assertTrue(all(req.get_method() == "GET" and req.data is None for req in calls))
        self.assertFalse(calls[0].has_header("Authorization"))
        self.assertTrue(calls[4].has_header("Authorization"))


if __name__ == "__main__":
    unittest.main()
