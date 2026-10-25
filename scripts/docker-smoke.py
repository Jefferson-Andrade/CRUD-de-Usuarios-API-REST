"""CI only: checks the disposable Compose stack started by the workflow."""
import base64
import http.cookiejar
import json
import os
import subprocess
import time
import urllib.error
import urllib.request

base = "http://127.0.0.1:" + os.environ.get("API_PORT", "8080")
credentials = (os.environ["APP_ADMIN_USERNAME"] + ":" + os.environ["APP_ADMIN_PASSWORD"]).encode()
authorization = "Basic " + base64.b64encode(credentials).decode()
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


def request(path, method="GET", body=None, authenticated=True, csrf=None):
    headers = {}
    if authenticated:
        headers["Authorization"] = authorization
    if csrf:
        headers[csrf["headerName"]] = csrf["token"]
    data = None if body is None else json.dumps(body).encode()
    if data is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(base + path, data=data, headers=headers, method=method)
    try:
        with client.open(req, timeout=5) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        return error.code, None


def ready():
    for _ in range(90):
        try:
            status, _ = request("/v3/api-docs", authenticated=False)
            if status == 200:
                return
        except (OSError, TimeoutError):
            pass
        time.sleep(2)
    raise AssertionError("API nao ficou disponivel em 180 segundos")


ready()
assert request("/users", authenticated=False)[0] == 401
assert request("/users")[0] == 200
status, csrf = request("/csrf")
assert status == 200
status, user = request("/users", "POST", {"name": "Demo Docker", "email": "docker@example.com"}, csrf=csrf)
assert status == 201
user_id = user["id"]
# Basic authentication may rotate the CSRF cookie; refresh before each write.
status, csrf = request("/csrf")
assert status == 200
status, updated = request("/users/" + str(user_id), "PUT",
                          {"name": "Demo Atualizada", "email": "docker@example.com"}, csrf=csrf)
assert status == 200, f"PUT retornou HTTP {status}"
assert updated["name"] == "Demo Atualizada"

# Explicit stop/start avoids accepting the old API process during a restart.
subprocess.run(["docker", "compose", "stop", "api", "db"], check=True)
subprocess.run(["docker", "compose", "up", "-d", "db", "api"], check=True)
ready()
status, persisted = request("/users/" + str(user_id))
assert status == 200 and persisted["name"] == "Demo Atualizada"
assert persisted["email"] == "docker@example.com"

migration_count = subprocess.check_output(
    ["docker", "compose", "exec", "-T", "db", "psql", "-U", os.environ.get("POSTGRES_USER", "postgres"),
     "-d", os.environ.get("POSTGRES_DB", "crud"), "-tAc",
     "SELECT count(*) FROM flyway_schema_history WHERE success AND version IN ('1','2','3','4')"],
    text=True).strip()
assert migration_count == "4"
status, csrf = request("/csrf")
assert status == 200
assert request("/users/" + str(user_id), "DELETE", csrf=csrf)[0] == 204
assert request("/users/" + str(user_id))[0] == 404
print("Docker smoke OK: Basic, CSRF, CRUD, V1-V4 e persistencia apos reiniciar.")
