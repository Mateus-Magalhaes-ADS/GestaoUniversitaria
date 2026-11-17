const API = `${location.protocol}//${location.hostname}:8080`;
const CLIENT_ID = "uniconecta-web";
const REDIRECT_URI = new URL("callback.html", location.href).href;

const notice = document.querySelector("#notice");
const loginButton = document.querySelector("#login-button");
const registerForm = document.querySelector("#register-form");
const registerProfile = document.querySelector("#register-profile");
const inviteCodeField = document.querySelector("#invite-code-field");
const inviteCodeInput = document.querySelector("#register-invite-code");
const enrollmentLink = document.querySelector("#enrollment-link");
const dashboard = document.querySelector("#dashboard");
const dashboardNavigation = document.querySelector("#dashboard-navigation");
const studentNavigation = document.querySelector("#student-navigation");
const staffNavigation = document.querySelector("#staff-navigation");
const staffDashboardLink = document.querySelector("#staff-dashboard-link");
const matriculaForm = document.querySelector("#matricula-form");
const matriculasSection = document.querySelector("#matriculas-section");
const provasSection = document.querySelector("#provas-section");
const studentSummary = document.querySelector("#student-summary");
const registerPassword = document.querySelector("#register-password");
const passwordConfirmation = document.querySelector("#register-password-confirmation");
const maximumFileSize = 10 * 1024 * 1024;
const enrollmentIntentKey = "open_enrollment_after_login";

function showNotice(message, error = false) {
  notice.textContent = message;
  notice.classList.toggle("error", error);
}

function base64Url(bytes) {
  return btoa(String.fromCharCode(...new Uint8Array(bytes)))
    .replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

async function login() {
  if (!window.isSecureContext || !window.crypto.subtle) {
    throw new Error("O login seguro exige HTTPS. Acesse por uma conexão HTTPS ou use localhost.");
  }
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

function validatePasswordConfirmation() {
  passwordConfirmation.setCustomValidity(
    passwordConfirmation.value && passwordConfirmation.value !== registerPassword.value
      ? "As senhas não coincidem."
      : ""
  );
}

function updateRegistrationRoleFields() {
  const isStudent = registerProfile.value === "ALUNO";
  inviteCodeField.hidden = isStudent;
  inviteCodeInput.disabled = isStudent;
  inviteCodeInput.required = !isStudent;
  if (isStudent) inviteCodeInput.value = "";
  enrollmentLink.textContent = isStudent
    ? "Entre e envie os documentos da matrícula"
    : "Entre para acessar seu painel";
}

registerProfile.addEventListener("change", updateRegistrationRoleFields);
updateRegistrationRoleFields();
registerPassword.addEventListener("input", validatePasswordConfirmation);
passwordConfirmation.addEventListener("input", validatePasswordConfirmation);

function validateEnrollmentFiles() {
  let valid = true;
  for (const input of matriculaForm.querySelectorAll('input[type="file"]')) {
    const file = input.files[0];
    const tooLarge = file && file.size > maximumFileSize;
    input.setCustomValidity(tooLarge ? "Cada arquivo deve ter no máximo 10 MB." : "");
    valid = valid && !tooLarge;
  }
  return valid;
}

matriculaForm.querySelectorAll('input[type="file"]').forEach(input => {
  input.addEventListener("change", validateEnrollmentFiles);
});

async function request(path, options = {}) {
  const token = sessionStorage.getItem("access_token");
  const headers = new Headers(options.headers || {});
  if (token) headers.set("Authorization", `Bearer ${token}`);
  let response;
  try {
    response = await fetch(`${API}${path}`, { ...options, headers });
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error(`Não foi possível conectar à API em ${API}. Verifique se o backend está iniciado e se ${location.origin} está permitido no CORS.`);
    }
    throw error;
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    const conflictMessages = {
      "/api/auth/register": "Já existe uma conta com este e-mail. Use Entrar com OAuth2 para acessar.",
      "/api/v1/matriculas": "Já existe uma solicitação de matrícula para este CPF."
    };
    const fallbackMessage = response.status === 409
      ? conflictMessages[path]
      : null;
    throw new Error(body.detail || body.message || fallbackMessage || `Falha na requisição (${response.status}).`);
  }
  if (response.status === 204) return null;
  return response.json();
}

registerForm.addEventListener("submit", async event => {
  event.preventDefault();
  const { passwordConfirmation: confirmation, ...data } = Object.fromEntries(new FormData(registerForm));
  if (data.password !== confirmation) {
    showNotice("As senhas não coincidem.", true);
    return;
  }
  const submitButton = document.querySelector("#register-button");
  submitButton.disabled = true;
  try {
    await request("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    registerForm.reset();
    updateRegistrationRoleFields();
    const profileName = {
      ALUNO: "aluno",
      PROFESSOR: "professor",
      COORDENADOR: "coordenador",
      ATENDENTE: "administrativo"
    }[data.profile];
    showNotice(`Conta de ${profileName} criada. Entre para acessar seu painel.`);
  } catch (error) {
    showNotice(error.message, true);
  } finally {
    submitButton.disabled = false;
  }
});

matriculaForm.addEventListener("submit", async event => {
  event.preventDefault();
  if (!validateEnrollmentFiles()) {
    matriculaForm.reportValidity();
    return;
  }
  const submitButton = document.querySelector("#matricula-submit");
  submitButton.disabled = true;
  try {
    const formData = new FormData(event.currentTarget);
    await request("/api/v1/matriculas", { method: "POST", body: formData });
    event.currentTarget.reset();
    showNotice("Documentos enviados. Sua solicitação de matrícula está aguardando validação.");
    await loadDashboard();
  } catch (error) {
    showNotice(error.message, true);
  } finally {
    submitButton.disabled = false;
  }
});

loginButton.addEventListener("click", () => {
  login().catch(error => showNotice(error.message, true));
});
enrollmentLink.addEventListener("click", event => {
  event.preventDefault();
  if (registerProfile.value === "ALUNO") {
    sessionStorage.setItem(enrollmentIntentKey, "true");
  } else {
    sessionStorage.removeItem(enrollmentIntentKey);
  }
  login().catch(error => {
    sessionStorage.removeItem(enrollmentIntentKey);
    showNotice(error.message, true);
  });
});
document.querySelector("#logout-button").addEventListener("click", () => {
  sessionStorage.clear();
  location.reload();
});

async function loadDashboard() {
  const profile = await request("/api/me");
  const roles = profile.roles;
  const isStudent = roles.includes("ALUNO");
  const canViewEnrollments = roles.some(role =>
    ["ALUNO", "ATENDENTE", "COORDENADOR"].includes(role));
  const canViewExams = roles.some(role =>
    ["ALUNO", "PROFESSOR", "COORDENADOR"].includes(role));
  const dashboardTitles = {
    ALUNO: "Painel do aluno",
    PROFESSOR: "Painel do professor",
    COORDENADOR: "Painel do coordenador",
    ATENDENTE: "Painel administrativo"
  };
  const staffDashboards = {
    PROFESSOR: { href: "/professor/dashboard", text: "Abrir painel do professor" },
    COORDENADOR: { href: "/coordenador/dashboard", text: "Abrir painel do coordenador" },
    ATENDENTE: { href: "/atendente/dashboard", text: "Abrir painel administrativo" }
  };
  const staffDashboard = roles.map(role => staffDashboards[role]).find(Boolean);
  dashboard.hidden = false;
  document.querySelector("#register-section").hidden = true;
  loginButton.hidden = true;
  document.querySelector("#welcome").textContent = roles
    .map(role => dashboardTitles[role])
    .find(Boolean) || "Painel";
  document.querySelector("#dashboard-email").textContent = profile.email;
  dashboardNavigation.hidden = !isStudent && !staffDashboard;
  studentNavigation.hidden = !isStudent;
  staffNavigation.hidden = !staffDashboard;
  staffDashboardLink.hidden = !staffDashboard;
  if (staffDashboard) {
    staffDashboardLink.href = `${API}${staffDashboard.href}`;
    staffDashboardLink.textContent = staffDashboard.text;
  }
  studentSummary.hidden = !isStudent;
  matriculasSection.hidden = !canViewEnrollments;
  provasSection.hidden = !canViewExams;
  matriculaForm.hidden = !isStudent;

  if (canViewEnrollments) {
    const enrollments = await request("/api/v1/matriculas");
    if (isStudent) {
      document.querySelector("#matricula-summary").textContent =
        `${enrollments.length} ${enrollments.length === 1 ? "solicitação" : "solicitações"}`;
    }
    renderList("#matriculas-list", enrollments.map(item => isStudent
      ? `Matrícula #${item.id} · Curso ${item.cursoId} · ${item.dataSolicitacao}`
      : `${item.nomeCompleto} · Curso ${item.cursoId} · ${item.dataSolicitacao}`));
  }

  if (canViewExams) {
    const exams = await request("/api/v1/provas");
    if (isStudent) {
      const openExams = exams.filter(exam => exam.status === "ABERTA").length;
      document.querySelector("#exam-summary").textContent =
        `${openExams} ${openExams === 1 ? "prova aberta" : "provas abertas"}`;
    }
    renderExams(exams, isStudent);
  }

  if (isStudent && sessionStorage.getItem(enrollmentIntentKey) === "true") {
    sessionStorage.removeItem(enrollmentIntentKey);
    matriculaForm.scrollIntoView({ behavior: "smooth", block: "start" });
    matriculaForm.querySelector("input").focus({ preventScroll: true });
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
    dashboard.hidden = true;
    document.querySelector("#register-section").hidden = false;
    loginButton.hidden = false;
    showNotice(error.message, true);
  });
}
