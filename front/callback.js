const API = "http://localhost:8080";
const message = document.querySelector("#callback-message");

async function completeLogin() {
  const params = new URLSearchParams(location.search);
  const expectedState = sessionStorage.getItem("oauth_state");
  const verifier = sessionStorage.getItem("pkce_verifier");
  if (params.get("error")) throw new Error(params.get("error_description") || params.get("error"));
  if (!params.get("code") || !expectedState || params.get("state") !== expectedState || !verifier) {
    throw new Error("Não foi possível validar a resposta de autenticação.");
  }

  const body = new URLSearchParams({
    grant_type: "authorization_code",
    client_id: "uniconecta-web",
    code: params.get("code"),
    redirect_uri: "http://localhost:5500/callback.html",
    code_verifier: verifier
  });
  const response = await fetch(`${API}/oauth2/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body
  });
  if (!response.ok) throw new Error("Não foi possível obter o token de acesso.");
  const token = await response.json();
  sessionStorage.setItem("access_token", token.access_token);
  sessionStorage.removeItem("oauth_state");
  sessionStorage.removeItem("pkce_verifier");
  location.replace("index.html");
}

completeLogin().catch(error => {
  message.textContent = error.message;
});
