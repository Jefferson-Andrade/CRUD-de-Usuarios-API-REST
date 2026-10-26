"""Read-only HTTPS checks. Set DEMO_URL and DEMO_READER_USERNAME/PASSWORD."""
import base64
import json
import os
import urllib.error
import urllib.parse
import urllib.request


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise RuntimeError("Redirecionamento recusado; use a URL HTTPS final")


def validate_url(url):
    parts = urllib.parse.urlsplit(url)
    if (parts.scheme != "https" or not parts.hostname or parts.username is not None
            or parts.password is not None or parts.path not in ("", "/")
            or parts.query or parts.fragment):
        raise ValueError("DEMO_URL deve ser a origem HTTPS final, sem credenciais ou caminho")
    return url.rstrip("/")


def verify(base, username, password):
    base = validate_url(base)
    if not username or not password:
        raise ValueError("Configure DEMO_READER_USERNAME e DEMO_READER_PASSWORD")
    auth = "Basic " + base64.b64encode((username + ":" + password).encode()).decode()
    client = urllib.request.build_opener(NoRedirect())

    def request(path, authenticated=False, as_json=True):
        headers = {"Authorization": auth} if authenticated else {}
        req = urllib.request.Request(base + path, headers=headers)
        try:
            with client.open(req, timeout=30) as response:
                body = response.read()
                return response.status, json.loads(body) if as_json and body else None
        except urllib.error.HTTPError as error:
            if 300 <= error.code < 400:
                raise RuntimeError("Redirecionamento recusado") from None
            error.close()
            return error.code, None

    def check(condition, message):
        if not condition:
            raise RuntimeError(message)

    status, health = request("/actuator/health")
    check(status == 200 and health == {"status": "UP"}, "Health anonimo deve ser UP sem detalhes")
    status, docs = request("/v3/api-docs")
    check(status == 200 and "openapi" in docs, "OpenAPI deve estar acessivel")
    check(request("/swagger-ui/index.html", as_json=False)[0] == 200, "Swagger deve abrir")
    check(request("/users")[0] == 401, "Consulta anonima deve retornar 401")
    check(request("/users", authenticated=True)[0] == 200, "READER deve consultar usuarios")
    check(request("/actuator/health", authenticated=True) == (200, {"status": "UP"}),
          "READER nao deve receber detalhes de saude")
    for path in ("/actuator/health/db", "/actuator/env"):
        check(request(path, authenticated=True)[0] == 403, "Endpoint operacional deve ser restrito")
    print("HTTPS, Swagger, health e consultas READER verificados. Nenhum dado foi alterado.")


if __name__ == "__main__":
    try:
        verify(os.environ["DEMO_URL"], os.environ["DEMO_READER_USERNAME"],
               os.environ["DEMO_READER_PASSWORD"])
    except Exception:
        # Never print request objects, credentials or credential-bearing proxy URLs.
        raise SystemExit("Verificacao falhou. Confira URL final, disponibilidade, permissoes e variaveis.")
