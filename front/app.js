const API = "http://localhost:8080";
const CLIENT_ID = "uniconecta-web";
const REDIRECT_URI = "http://localhost:5500/callback.html";

const notice = document.querySelector("#notice");
const loginButton = document.querySelector("#login-button");
const registerForm = document.querySelector("#register-form");
const dashboard = document.querySelector("#dashboard");

function showNotice(message, error = false) {
  notice.textContent = message;
  notice.classList.toggle("error", error);
}

function base64Url(bytes) {
  return btoa(String.fromCharCode(...new Uint8Array(bytes)))
    .replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

async function login() {
  const verifier = base64Url(crypto.getRandomValues(new Uint8Array(48)));
  const challenge = base64Url(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier)));
  const state = base64Url(crypto.getRandomValues(new Uint8Array(24)));
  sessionStorage.setItem("pkce_verifier", verifier);
  sessionStorage.setItem("oauth_state", state);
  const query = new URLSearchParams({
    response_type: "code",
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope: "openid profile",
    state,
    code_challenge: challenge,
    code_challenge_method: "S256"
  });
  location.assign(`${API}/oauth2/authorize?${query}`);
}

async function request(path, options = {}) {
  const token = sessionStorage.getItem("access_token");
  const headers = new Headers(options.headers || {});
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const response = await fetch(`${API}${path}`, { ...options, headers });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.detail || body.message || `Falha na requisição (${response.status}).`);
  }
  if (response.status === 204) return null;
  return response.json();
}

registerForm.addEventListener("submit", async event => {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(registerForm));
  try {
    await request("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    registerForm.reset();
    showNotice("Conta criada. Use Entrar com OAuth2 para acessar.");
  } catch (error) {
    showNotice(error.message, true);
  }
});

document.querySelector("#matricula-form").addEventListener("submit", async event => {
  event.preventDefault();
  try {
    const formData = new FormData(event.currentTarget);
    await request("/api/v1/matriculas", { method: "POST", body: formData });
    event.currentTarget.reset();
    showNotice("Solicitação de matrícula enviada.");
    await loadDashboard();
  } catch (error) {
    showNotice(error.message, true);
  }
});

loginButton.addEventListener("click", login);
document.querySelector("#logout-button").addEventListener("click", () => {
  sessionStorage.clear();
  location.reload();
});

async function loadDashboard() {
  const profile = await request("/api/me");
  dashboard.hidden = false;
  document.querySelector("#register-section").hidden = true;
  document.querySelector("#welcome").textContent = `Bem-vindo(a), ${profile.email}`;
  if (profile.roles.includes("ALUNO")) {
    const enrollments = await request("/api/v1/matriculas");
    renderList("#matriculas-list", enrollments.map(item =>
      `Matrícula #${item.id} · Curso ${item.cursoId} · ${item.dataSolicitacao}`));
    const exams = await request("/api/v1/provas");
    renderExams(exams, true);
  } else {
    document.querySelector("#matricula-form").hidden = true;
    const [enrollments, exams] = await Promise.all([
      request("/api/v1/matriculas"),
      request("/api/v1/provas")
    ]);
    renderList("#matriculas-list", enrollments.map(item =>
      `${item.nomeCompleto} · Curso ${item.cursoId} · ${item.dataSolicitacao}`));
    renderExams(exams, false);
  }
}

function renderList(selector, items) {
  const list = document.querySelector(selector);
  list.replaceChildren();
  for (const text of items) {
    const row = document.createElement("li");
    row.textContent = text;
    list.append(row);
  }
  if (!items.length) {
    const row = document.createElement("li");
    row.textContent = "Nenhum registro encontrado.";
    list.append(row);
  }
}

function renderExams(exams, isStudent) {
  const list = document.querySelector("#provas-list");
  list.replaceChildren();
  for (const exam of exams) {
    const row = document.createElement("li");
    const text = document.createElement("span");
    text.textContent = `${exam.title} · ${exam.subject} · ${exam.status}`;
    row.append(text);
    if (isStudent && exam.status === "ABERTA") {
      const button = document.createElement("button");
      button.className = "button secondary";
      button.type = "button";
      button.textContent = "Responder";
      button.addEventListener("click", () => showExamForm(exam.id));
      row.append(" ", button);
    }
    list.append(row);
  }
  if (!exams.length) {
    const row = document.createElement("li");
    row.textContent = "Nenhuma prova disponível.";
    list.append(row);
  }
}

async function showExamForm(examId) {
  const container = document.querySelector("#prova-form-container");
  container.replaceChildren();
  try {
    const exam = await request(`/api/v1/provas/${examId}`);
    const form = document.createElement("form");
    form.className = "form-grid";
    const heading = document.createElement("h4");
    heading.textContent = `${exam.title} · ${exam.subject}`;
    form.append(heading);
    for (const question of exam.questions) {
      const fieldset = document.createElement("fieldset");
      const legend = document.createElement("legend");
      legend.textContent = question.prompt;
      fieldset.append(legend);
      if (question.type === "OBJETIVA") {
        question.alternatives.forEach((alternative, index) => {
          const label = document.createElement("label");
          const radio = document.createElement("input");
          radio.type = "radio";
          radio.name = `question-${question.id}`;
          radio.value = String(index);
          radio.required = true;
          label.append(radio, document.createTextNode(alternative));
          fieldset.append(label);
        });
      } else {
        const answer = document.createElement("textarea");
        answer.name = `question-${question.id}`;
        answer.required = true;
        fieldset.append(answer);
      }
      form.append(fieldset);
    }
    const submit = document.createElement("button");
    submit.className = "button";
    submit.type = "submit";
    submit.textContent = "Enviar respostas";
    form.append(submit);
    form.addEventListener("submit", async event => {
      event.preventDefault();
      const answers = exam.questions.map(question => {
        const value = new FormData(form).get(`question-${question.id}`);
        return question.type === "OBJETIVA"
          ? { questionId: question.id, selectedAlternative: Number(value) }
          : { questionId: question.id, text: value };
      });
      try {
        await request(`/api/v1/provas/${examId}/responder`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ answers })
        });
        showNotice("Respostas enviadas.");
        container.replaceChildren();
      } catch (error) {
        showNotice(error.message, true);
      }
    });
    container.append(form);
  } catch (error) {
    showNotice(error.message, true);
  }
}

if (sessionStorage.getItem("access_token")) {
  loadDashboard().catch(error => {
    sessionStorage.removeItem("access_token");
    showNotice(error.message, true);
  });
}
