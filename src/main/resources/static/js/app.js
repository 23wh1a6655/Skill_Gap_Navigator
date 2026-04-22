const { useEffect, useMemo, useState } = React;

const API = "";
const LEVELS = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

function getSession() {
  try {
    return JSON.parse(localStorage.getItem("skillGapNavigatorSession") || "null");
  } catch (error) {
    localStorage.removeItem("skillGapNavigatorSession");
    return null;
  }
}

function levelValue(level) {
  return LEVELS.indexOf(level) + 1;
}

function groupRoadmapItems(roadmap) {
  return roadmap.reduce((weeks, item) => {
    const key = item.weekLabel || "Next Focus";
    if (!weeks[key]) weeks[key] = [];
    weeks[key].push(item);
    return weeks;
  }, {});
}

function answeredQuizCount(questions, answers) {
  return questions.reduce((count, question) => {
    const value = answers[question.id];
    return value && String(value).trim() ? count + 1 : count;
  }, 0);
}

function quizTheme(skillName = "") {
  const seeds = [
    { tone: "theme-blue", symbol: "Q", label: "Knowledge Check" },
    { tone: "theme-teal", symbol: "A", label: "Applied Skills" },
    { tone: "theme-violet", symbol: "R", label: "Role Readiness" },
    { tone: "theme-orange", symbol: "S", label: "Skill Sprint" }
  ];
  const index = skillName.length % seeds.length;
  return seeds[index];
}

function featureMeta(title) {
  const registry = {
    "Role Discovery": { tone: "tone-role", symbol: "ROLE", eyebrow: "Choose direction" },
    "Skill Gap": { tone: "tone-gap", symbol: "GAP", eyebrow: "Measure fit" },
    "Weekly Plan": { tone: "tone-plan", symbol: "PLAN", eyebrow: "Follow path" },
    "Progress Proof": { tone: "tone-proof", symbol: "TRACK", eyebrow: "See growth" }
  };
  return registry[title] || { tone: "tone-role", symbol: "SKILL", eyebrow: "Navigator" };
}

async function api(url, options = {}) {
  const session = getSession();
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (session?.token) headers.Authorization = `Bearer ${session.token}`;
  let response;
  try {
    response = await fetch(`${API}${url}`, { ...options, headers });
  } catch (error) {
    throw new Error("Unable to reach the server. Please check that the app is running.");
  }
  const data = await response.json().catch(() => ({}));
  if (response.status === 401 || response.status === 403) {
    localStorage.removeItem("skillGapNavigatorSession");
    window.dispatchEvent(new CustomEvent("skill-gap-session-expired"));
    throw new Error("Your session expired. Please login again.");
  }
  if (!response.ok) throw new Error(data.message || "Request failed.");
  return data;
}

function App() {
  const [session, setSession] = useState(getSession());
  const [page, setPage] = useState(() => getSession() ? "dashboard" : "home");
  const [authMode, setAuthMode] = useState("login");
  const [authForm, setAuthForm] = useState({ username: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const [roles, setRoles] = useState([]);
  const [roleQuery, setRoleQuery] = useState("");
  const [roleAssessment, setRoleAssessment] = useState(null);
  const [roleAssessmentAnswers, setRoleAssessmentAnswers] = useState({});
  const [roleAssessmentResult, setRoleAssessmentResult] = useState(null);
  const [roleAssessmentIndex, setRoleAssessmentIndex] = useState(0);
  const [resumeText, setResumeText] = useState("");
  const [resumeFileName, setResumeFileName] = useState("");
  const [resumeAnalysis, setResumeAnalysis] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [dashboard, setDashboard] = useState(null);
  const [roadmap, setRoadmap] = useState([]);
  const [selectedRoadmapSkill, setSelectedRoadmapSkill] = useState("");
  const [quiz, setQuiz] = useState(null);
  const [quizAnswers, setQuizAnswers] = useState({});
  const [quizResult, setQuizResult] = useState(null);
  const [quizConfidence, setQuizConfidence] = useState(50);
  const [quizIndex, setQuizIndex] = useState(0);

  const filteredRoles = useMemo(() => {
    if (!roleQuery.trim()) return roles;
    return roles;
  }, [roles, roleQuery]);

  const groupedRoadmap = useMemo(() => groupRoadmapItems(roadmap), [roadmap]);
  const selectedRoadmapItem = useMemo(
    () => roadmap.find((item) => item.skillName === selectedRoadmapSkill) || roadmap[0] || null,
    [roadmap, selectedRoadmapSkill]
  );

  useEffect(() => {
    loadRoles();
  }, []);

  useEffect(() => {
    if (!session || page !== "role-selection") {
      return;
    }
    const handle = setTimeout(() => {
      if (roleQuery.trim().length < 2) {
        loadRoles();
        return;
      }
      searchRoles(roleQuery.trim());
    }, 300);
    return () => clearTimeout(handle);
  }, [roleQuery, page, session?.user?.id]);

  useEffect(() => {
    function handleExpiredSession() {
      setSession(null);
      setDashboard(null);
      setRoadmap([]);
      setAnalysis(null);
      setResumeAnalysis(null);
      setResumeText("");
      setResumeFileName("");
      setRoleAssessment(null);
      setRoleAssessmentAnswers({});
      setRoleAssessmentResult(null);
      setRoleAssessmentIndex(0);
      setQuiz(null);
      setQuizResult(null);
      setQuizIndex(0);
      setSelectedRoadmapSkill("");
      setPage("login");
      setAuthMode("login");
    }

    window.addEventListener("skill-gap-session-expired", handleExpiredSession);
    return () => window.removeEventListener("skill-gap-session-expired", handleExpiredSession);
  }, []);

  useEffect(() => {
    if (session?.user?.id) {
      hydrateWorkspace(session.user.id);
      if (session.user.targetRole) {
        setRoleQuery(session.user.targetRole);
      }
    }
  }, [session?.user?.id]);

  async function loadRoles() {
    try {
      setRoles(await api("/api/catalog/roles"));
    } catch (err) {
      setError(err.message);
    }
  }

  async function searchRoles(query) {
    try {
      setRoles(await api(`/api/catalog/roles/search?query=${encodeURIComponent(query)}`));
    } catch (err) {
      setError(err.message);
    }
  }

  async function hydrateWorkspace(userId) {
    try {
      const [dashboardData, roadmapData] = await Promise.all([
        api(`/api/dashboard/${userId}`),
        api(`/api/roadmap/${userId}`)
      ]);
      setDashboard(dashboardData);
      setRoadmap(roadmapData);
      setSelectedRoadmapSkill((current) => {
        if (roadmapData.some((item) => item.skillName === current)) return current;
        return roadmapData[0]?.skillName || "";
      });
    } catch (err) {
      setError(err.message);
    }
  }

  function persistSession(data) {
    const nextSession = { token: data.token, user: data.user };
    setSession(nextSession);
    localStorage.setItem("skillGapNavigatorSession", JSON.stringify(nextSession));
  }

  async function submitAuth(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      const endpoint = authMode === "register" ? "/api/auth/register" : "/api/auth/login";
      const payload = authMode === "register"
        ? authForm
        : { email: authForm.email, password: authForm.password };
      const data = await api(endpoint, { method: "POST", body: JSON.stringify(payload) });
      persistSession(data);
      setAuthForm({ username: "", email: "", password: "" });
      setPage(data.user.targetRole ? "dashboard" : "role-selection");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    localStorage.removeItem("skillGapNavigatorSession");
    setSession(null);
    setRoleAssessment(null);
    setRoleAssessmentAnswers({});
    setRoleAssessmentResult(null);
    setRoleAssessmentIndex(0);
    setResumeAnalysis(null);
    setResumeText("");
    setResumeFileName("");
    setAnalysis(null);
    setDashboard(null);
    setRoadmap([]);
    setSelectedRoadmapSkill("");
    setQuiz(null);
    setQuizAnswers({});
    setQuizResult(null);
    setQuizIndex(0);
    setPage("home");
  }

  async function continueRoleSelection(roleName) {
    try {
      const payload = await api(`/api/quiz/role-assessment?userId=${session.user.id}&role=${encodeURIComponent(roleName)}`);
      setRoleAssessment(payload);
      setRoleAssessmentAnswers({});
      setRoleAssessmentResult(null);
      setRoleAssessmentIndex(0);
      setPage("role-assessment");
    } catch (err) {
      setError(err.message);
    }
  }

  async function submitRoleAssessment() {
    if (!roleAssessment) return;
    setLoading(true);
    setError("");
    try {
      const data = await api("/api/quiz/role-assessment/submit", {
        method: "POST",
        body: JSON.stringify({
          userId: session.user.id,
          role: roleAssessment.roleName,
          answers: Object.entries(roleAssessmentAnswers).map(([questionId, value]) => ({
            questionId,
            selectedOption: value,
            responseText: value
          }))
        })
      });
      setRoleAssessmentResult(data);
      setAnalysis(data.analysis);
      const nextSession = { ...session, user: { ...session.user, targetRole: data.analysis.role, onboardingComplete: true } };
      setSession(nextSession);
      localStorage.setItem("skillGapNavigatorSession", JSON.stringify(nextSession));
      await hydrateWorkspace(session.user.id);
      setPage("assessment-result");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function analyzeResume() {
    if (!resumeText.trim() || !roleQuery.trim()) {
      setError("Paste resume text and choose a role first.");
      return;
    }
    try {
      const data = await api("/api/dashboard/resume-analysis", {
        method: "POST",
        body: JSON.stringify({
          userId: session.user.id,
          role: roleQuery.trim(),
          resumeText
        })
      });
      setResumeAnalysis(data);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleResumeUpload(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    setResumeFileName(file.name);
    try {
      const text = await file.text();
      setResumeText(text);
    } catch (err) {
      setError("Unable to read this file. Upload a text-based resume or paste the content manually.");
    }
  }

  async function openQuiz(skillName) {
    try {
      const data = await api(`/api/quiz?userId=${session.user.id}&skill=${encodeURIComponent(skillName)}`);
      setQuiz(data);
      setQuizAnswers({});
      setQuizResult(null);
      setQuizConfidence(data.confidenceScore ?? 50);
      setQuizIndex(0);
      setPage("quiz");
    } catch (err) {
      setError(err.message);
    }
  }

  async function submitQuiz() {
    try {
      const result = await api("/api/quiz/submit", {
        method: "POST",
        body: JSON.stringify({
          userId: session.user.id,
          skillName: quiz.skillName,
          confidenceRating: quizConfidence,
          answers: Object.entries(quizAnswers).map(([questionId, value]) => ({ questionId, selectedOption: value, responseText: value }))
        })
      });
      setQuizResult(result);
      await hydrateWorkspace(session.user.id);
    } catch (err) {
      setError(err.message);
    }
  }

  async function saveRoadmapPreferences(skillName, patch) {
    try {
      await api("/api/roadmap/preferences", {
        method: "PUT",
        body: JSON.stringify({ userId: session.user.id, skillName, ...patch })
      });
      await hydrateWorkspace(session.user.id);
    } catch (err) {
      setError(err.message);
    }
  }

  function openRoadmapDetail(skillName) {
    setSelectedRoadmapSkill(skillName);
    setPage("roadmap-detail");
  }

  const currentRoleAssessmentQuestion = roleAssessment?.questions?.[roleAssessmentIndex] || null;
  const currentQuizQuestion = quiz?.questions?.[quizIndex] || null;

  function moveRoleAssessment(direction) {
    setRoleAssessmentIndex((current) => {
      const total = roleAssessment?.questions?.length || 0;
      if (!total) return current;
      return Math.max(0, Math.min(current + direction, total - 1));
    });
  }

  function moveQuiz(direction) {
    setQuizIndex((current) => {
      const total = quiz?.questions?.length || 0;
      if (!total) return current;
      return Math.max(0, Math.min(current + direction, total - 1));
    });
  }

  function jumpToRoleAssessment(index) {
    setRoleAssessmentIndex(index);
  }

  function jumpToQuiz(index) {
    setQuizIndex(index);
  }

  return (
    <div className="app-root">
      <TopNav
        session={session}
        page={page}
        setPage={setPage}
        setAuthMode={setAuthMode}
        logout={logout}
      />

      {error && <div className="page-shell"><div className="alert-banner">{error}</div></div>}

      {!session && page === "home" && (
        <main className="page-shell">
          <section className="hero-block simple-home-hero">
            <div className="hero-copy home-name-only">
              <img className="home-logo" src="/images/skill-gap-logo.svg" alt="Skill Gap Navigator logo" />
              <h1>Skill Gap Navigator</h1>
              <p className="home-tagline">Assessment-based skill gap analysis with a personalized roadmap to help users become role-ready.</p>
              <div className="hero-actions">
                <button className="primary-btn" onClick={() => { setAuthMode("register"); setPage("register"); }}>Get Started</button>
                <button className="secondary-btn" onClick={() => { setAuthMode("login"); setPage("login"); }}>Login</button>
              </div>
            </div>
          </section>
        </main>
      )}

      {!session && (page === "login" || page === "register") && (
        <main className="auth-page">
          <section className="auth-card large-auth-card">
            <div className="brand-mark">
              <div className="brand-icon">
                <img className="auth-logo" src="/images/skill-gap-logo.svg" alt="Skill Gap Navigator logo" />
              </div>
              <div>
                <h2>Skill Gap Navigator</h2>
              </div>
            </div>

            <form onSubmit={submitAuth} className="auth-form">
              {page === "register" && (
                <label className="form-group">
                  <span>Name</span>
                  <input value={authForm.username} onChange={(e) => setAuthForm({ ...authForm, username: e.target.value })} />
                </label>
              )}
              <label className="form-group">
                <span>Email</span>
                <input type="email" value={authForm.email} onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })} />
              </label>
              <label className="form-group">
                <span>Password</span>
                <input type="password" value={authForm.password} onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })} />
              </label>
              <button className="primary-btn wide" disabled={loading}>{loading ? "Please wait..." : page === "login" ? "Sign In" : "Create Account"}</button>
            </form>

            <div className="auth-switch">
              {page === "login" ? (
                <button onClick={() => { setAuthMode("register"); setPage("register"); }}>Create account</button>
              ) : (
                <button onClick={() => { setAuthMode("login"); setPage("login"); }}>Back to login</button>
              )}
            </div>
          </section>
        </main>
      )}

      {session && page === "role-selection" && (
        <main className="page-shell">
          <section className="panel-card setup-hero">
            <div className="content-header">
                <div className="eyebrow">Role Discovery</div>
                <h1>Choose your target role</h1>
            </div>
            <div className="setup-hero-actions">
              <div className="setup-search">
                <input
                  value={roleQuery}
                  placeholder="Type any job role"
                  onChange={(e) => setRoleQuery(e.target.value)}
                />
                <span>{filteredRoles.length} roles</span>
              </div>
              <button className="secondary-btn" onClick={() => setPage("resume-analyzer")}>Resume Analyzer</button>
            </div>
          </section>

          <section className="role-grid">
            {filteredRoles.map((role) => (
              <article key={role.name} className={`role-card ${roleQuery === role.name ? "selected" : ""}`} onClick={() => { setRoleQuery(role.name); continueRoleSelection(role.name); }}>
                <div className="role-icon" aria-hidden="true">{roleSymbol(role.name)}</div>
                <h3>{role.name}</h3>
                <p>{role.description}</p>
                <div className="role-meta">
                  <span>{role.salarySignal}</span>
                  <span>{role.hiringSignal}</span>
                </div>
                <div className="pill-row">
                  {role.skills.slice(0, 4).map((skill) => <span key={skill.skillName} className="pill">{skill.skillName}</span>)}
                </div>
              </article>
            ))}
          </section>

          {roleQuery.trim().length >= 2 && !filteredRoles.length && (
            <section className="panel-card center-card">
              <h3>Use this custom role</h3>
              <div className="footer-actions">
                <button className="primary-btn" onClick={() => continueRoleSelection(roleQuery.trim())}>Continue With This Role</button>
              </div>
            </section>
          )}
        </main>
      )}

      {session && page === "role-assessment" && roleAssessment && (
        <main className="page-shell narrow">
          <section className="panel-card quiz-shell theme-violet">
            <div className="quiz-hero">
              <div className="quiz-hero-copy">
                <div className="eyebrow">Role Assessment</div>
                <h3>{roleAssessment.roleName} Screening Test</h3>
                <p>{roleAssessment.totalQuestions} questions • {roleAssessment.totalSkills} skills</p>
              </div>
              <div className="quiz-hero-mark">
                <span>Measured Analysis</span>
                <strong>RA</strong>
              </div>
            </div>

            <div className="quiz-insight-grid">
              <div className="quiz-insight-card">
                <span>Answered</span>
                <strong>{answeredQuizCount(roleAssessment.questions, roleAssessmentAnswers)}/{roleAssessment.totalQuestions}</strong>
                <small>questions completed</small>
              </div>
              <div className="quiz-insight-card">
                <span>Coverage</span>
                <strong>{roleAssessment.totalSkills}</strong>
                <small>required role skills</small>
              </div>
            </div>

            <div className="quiz-jump-row">
              {roleAssessment.questions.map((question, index) => {
                const answered = !!roleAssessmentAnswers[question.id]?.toString().trim();
                const active = index === roleAssessmentIndex;
                return (
                  <button
                    key={question.id}
                    type="button"
                    className={`quiz-jump-pill ${answered ? "answered" : "unanswered"} ${active ? "active" : ""}`}
                    onClick={() => jumpToRoleAssessment(index)}
                  >
                    {String(index + 1).padStart(2, "0")}
                  </button>
                );
              })}
            </div>

            {currentRoleAssessmentQuestion && (
              <article className="question-block quiz-question-card single-question-card" key={currentRoleAssessmentQuestion.id}>
                <div className="quiz-question-head">
                  <div className="quiz-question-number">{String(roleAssessmentIndex + 1).padStart(2, "0")}</div>
                  <div>
                    <h4>{currentRoleAssessmentQuestion.skillName}</h4>
                    <small className="question-progress-label">
                      Question {roleAssessmentIndex + 1} of {roleAssessment.questions.length}
                    </small>
                    <div className="question-meta">
                      <span className="pill">{currentRoleAssessmentQuestion.questionType}</span>
                      <span className="pill">{currentRoleAssessmentQuestion.difficulty}</span>
                      <span className="pill">{currentRoleAssessmentQuestion.concept}</span>
                    </div>
                  </div>
                </div>
                <p>{currentRoleAssessmentQuestion.prompt}</p>
                {currentRoleAssessmentQuestion.options?.length ? (
                  <div className="option-stack">
                    {currentRoleAssessmentQuestion.options.map((option, optionIndex) => (
                      <label className={`option-card quiz-option-card ${roleAssessmentAnswers[currentRoleAssessmentQuestion.id] === option ? "selected" : ""}`} key={option}>
                        <input
                          type="radio"
                          name={currentRoleAssessmentQuestion.id}
                          checked={roleAssessmentAnswers[currentRoleAssessmentQuestion.id] === option}
                          onChange={() => setRoleAssessmentAnswers({ ...roleAssessmentAnswers, [currentRoleAssessmentQuestion.id]: option })}
                        />
                        <span className="option-token">{String.fromCharCode(65 + optionIndex)}</span>
                        <span>{option}</span>
                      </label>
                    ))}
                  </div>
                ) : (
                  <textarea
                    className="assessment-textarea quiz-textarea"
                    placeholder="Write your answer"
                    value={roleAssessmentAnswers[currentRoleAssessmentQuestion.id] || ""}
                    onChange={(e) => setRoleAssessmentAnswers({ ...roleAssessmentAnswers, [currentRoleAssessmentQuestion.id]: e.target.value })}
                  />
                )}
                <div className="question-flow-actions">
                  <button className="secondary-btn" disabled={roleAssessmentIndex === 0} onClick={() => moveRoleAssessment(-1)}>Back</button>
                  {roleAssessmentIndex < roleAssessment.questions.length - 1 && (
                    <button className="primary-btn" onClick={() => moveRoleAssessment(1)}>Next</button>
                  )}
                </div>
              </article>
            )}

            <div className="footer-actions">
              <button className="secondary-btn" onClick={() => setPage("role-selection")}>Back</button>
              <button className="primary-btn" disabled={loading} onClick={submitRoleAssessment}>
                {loading ? "Analysing..." : "Analyse Skills From Test"}
              </button>
            </div>

            {roleAssessmentResult && (
              <div className="quiz-result-card vivid-quiz-result">
                <div className="quiz-result-hero">
                  <div className="quiz-score-orb">
                    <span>Score</span>
                    <strong>{roleAssessmentResult.percentage}%</strong>
                  </div>
                  <div className="quiz-result-copy">
                    <h3>{roleAssessmentResult.roleName} Assessment</h3>
                    <p>Your roadmap is now generated from assessed performance instead of only self-reported levels.</p>
                  </div>
                </div>
              </div>
            )}
          </section>
        </main>
      )}

      {session && page === "assessment-result" && roleAssessmentResult && (
        <main className="page-shell narrow">
          <section className="panel-card quiz-shell theme-teal">
            <div className="quiz-hero">
              <div className="quiz-hero-copy">
                <div className="eyebrow">Assessment Result</div>
                <h3>{roleAssessmentResult.roleName}</h3>
                <p>
                  {roleAssessmentResult.percentage === 100 || !roleAssessmentResult.analysis.missingSkills.length
                    ? "You are directly eligible for this role based on the assessment."
                    : "Roadmap generated from your weaker areas."}
                </p>
              </div>
              <div className="quiz-score-orb">
                <span>Score</span>
                <strong>{roleAssessmentResult.percentage}%</strong>
              </div>
            </div>

            <div className="quiz-insight-grid">
              <div className="quiz-insight-card">
                <span>Status</span>
                <strong>{roleAssessmentResult.percentage === 100 || !roleAssessmentResult.analysis.missingSkills.length ? "Eligible" : "Needs Focus"}</strong>
                <small>based on role assessment</small>
              </div>
              <div className="quiz-insight-card">
                <span>Ready Skills</span>
                <strong>{roleAssessmentResult.analysis.strengths.length}</strong>
                <small>skills already matching role expectation</small>
              </div>
              <div className="quiz-insight-card">
                <span>Missing Skills</span>
                <strong>{roleAssessmentResult.analysis.missingSkills.length}</strong>
                <small>skills to improve for this role</small>
              </div>
            </div>

            {!!roleAssessmentResult.analysis.missingSkills.length && (
              <div className="panel-card">
                <div className="panel-head">
                  <h3>Focus Areas</h3>
                </div>
                <div className="focus-stack">
                  {roleAssessmentResult.analysis.missingSkills.map((skill) => (
                    <div className="focus-card" key={skill.skillName}>
                      <div>
                        <strong>{skill.skillName}</strong>
                      </div>
                      <span className="pill warning">Target {skill.targetLevel}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="quiz-result-grid">
              {roleAssessmentResult.feedback.map((item) => <div key={item} className="result-row">{item}</div>)}
              <div className="result-row">{roleAssessmentResult.analysis.recommendation}</div>
            </div>

            <div className="footer-actions">
              <button className="secondary-btn" onClick={() => setPage("role-selection")}>Retake For Another Role</button>
              <button className="primary-btn" onClick={() => setPage("dashboard")}>
                {roleAssessmentResult.percentage === 100 || !roleAssessmentResult.analysis.missingSkills.length ? "Open Dashboard" : "View Your Roadmap"}
              </button>
            </div>
          </section>
        </main>
      )}

      {session && page === "resume-analyzer" && (
        <main className="page-shell narrow">
          <section className="panel-card">
            <div className="panel-head row-between">
              <div>
                <div className="eyebrow">Resume Analyzer</div>
                <h2>Upload Resume And Detect Skill Gaps</h2>
                <p>Choose a target role, upload a text-based resume, and analyze which role skills are missing.</p>
              </div>
              <button className="secondary-btn" onClick={() => setPage("role-selection")}>Back To Roles</button>
            </div>

            <div className="auth-form">
              <label className="form-group">
                <span>Target Role</span>
                <input value={roleQuery} onChange={(e) => setRoleQuery(e.target.value)} placeholder="Enter target role" />
              </label>

              <label className="form-group">
                <span>Upload Resume File</span>
                <input type="file" accept=".txt,.md,.csv,.json" onChange={handleResumeUpload} />
              </label>

              {!!resumeFileName && (
                <div className="alert-banner">Loaded file: {resumeFileName}</div>
              )}

              <label className="form-group">
                <span>Resume Text</span>
                <textarea
                  className="assessment-textarea"
                  placeholder="Upload a text-based resume file or paste resume content here"
                  value={resumeText}
                  onChange={(e) => setResumeText(e.target.value)}
                />
              </label>

              <div className="footer-actions">
                <button className="primary-btn" onClick={analyzeResume}>Analyze Resume</button>
              </div>
            </div>

            {resumeAnalysis && (
              <div className="achievement-stack">
                <div className="achievement-card">
                  <strong>Detected Skills</strong>
                  <p>{resumeAnalysis.detectedSkills.join(", ") || "No role skills detected in resume."}</p>
                </div>
                <div className="achievement-card">
                  <strong>Missing Skills</strong>
                  <p>{resumeAnalysis.missingSkills.join(", ") || "No missing skills detected."}</p>
                </div>
                <div className="achievement-card">
                  <strong>Recommendation</strong>
                  <p>{resumeAnalysis.recommendation}</p>
                </div>
              </div>
            )}
          </section>
        </main>
      )}

      {session && page === "dashboard" && (
        <main className="page-shell">
          <section className="dashboard-hero clean-dashboard-hero">
            <div className="dashboard-copy">
              <div className="eyebrow">Dashboard</div>
              <h1>{session.user.targetRole || "Career Dashboard"}</h1>
              <p>Track your readiness, review recent results, and continue the roadmap toward your target role.</p>
            </div>
            <div className="dashboard-hero-actions">
              <button className="primary-btn" onClick={() => setPage("quiz")}>Take Quiz</button>
              <button className="secondary-btn" onClick={() => setPage("roadmap")}>Open Roadmap</button>
            </div>
          </section>

          <section className="stats-grid">
            <MetricCard title="Readiness" value={`${dashboard?.readinessScore ?? 0}%`} />
            <MetricCard title="Completion" value={`${dashboard?.completionRate ?? 0}%`} />
            <MetricCard title="Completed Skills" value={dashboard?.completedSkills ?? 0} />
            <MetricCard title="Pending Skills" value={dashboard?.pendingSkills ?? 0} />
          </section>

          <section className="dashboard-main-grid">
            <article className="panel-card chart-card primary-panel">
              <div className="panel-head">
                <h3>Readiness Overview</h3>
                <p>Current learning distribution for your target role</p>
              </div>
              <div className="overview-grid">
                <ProgressChart dashboard={dashboard} />
                <div className="overview-list">
                  <div className="overview-item">
                    <span>Target Role</span>
                    <strong>{dashboard?.targetRole || "-"}</strong>
                  </div>
                  <div className="overview-item">
                    <span>Hours Left</span>
                    <strong>{dashboard?.totalEstimatedHours ?? 0}</strong>
                  </div>
                  <div className="overview-item">
                    <span>Learning Streak</span>
                    <strong>{dashboard?.streakDays ?? 0} day</strong>
                  </div>
                </div>
              </div>
            </article>

            <article className="panel-card secondary-panel">
              <div className="panel-head">
                <h3>Current Focus</h3>
                <p>What needs attention right now</p>
              </div>
              <div className="focus-stack">
                {(analysis?.missingSkills?.length ? analysis.missingSkills : roadmap).slice(0, 5).map((skill) => (
                  <div className="focus-card" key={skill.skillName}>
                    <div>
                      <strong>{skill.skillName}</strong>
                      <p>{skill.description || "Important for your roadmap."}</p>
                    </div>
                    <span className="pill warning">{skill.targetLevel || "Focus"}</span>
                  </div>
                ))}
              </div>
            </article>
          </section>

          <section className="dashboard-main-grid">
            <article className="panel-card">
              <div className="panel-head">
                <h3>Progress Analytics</h3>
              </div>
              <div className="stats-grid">
                <MetricCard title="Average Quiz Score" value={`${dashboard?.analytics?.averageQuizScore ?? 0}%`} />
                <MetricCard title="Latest Score" value={`${dashboard?.analytics?.latestQuizScore ?? 0}%`} />
                <MetricCard title="Improvement" value={`${dashboard?.analytics?.improvementDelta ?? 0}%`} />
                <MetricCard title="Reassessments" value={dashboard?.analytics?.reassessmentCount ?? 0} />
              </div>
              <div className="focus-stack">
                {(dashboard?.analytics?.weakestSkills || []).map((skill) => (
                  <div className="focus-card" key={skill}>
                    <div>
                      <strong>{skill}</strong>
                    </div>
                  </div>
                ))}
              </div>
            </article>

            <article className="panel-card">
              <div className="panel-head">
                <h3>Recent Results</h3>
              </div>
              <div className="mini-results">
                {dashboard?.recentResults?.length ? dashboard.recentResults.slice(0, 4).map((item) => (
                  <div className="mini-result-card" key={`${item.skillName}-${item.completedAt}`}>
                    <strong>{item.skillName}</strong>
                    <span>{item.percentage}%</span>
                  </div>
                )) : <div className="empty-inline">No quiz results yet.</div>}
              </div>
              {!!dashboard?.achievements?.length && (
                <div className="achievement-stack">
                  {dashboard.achievements.slice(0, 2).map((item) => (
                    <div className="achievement-card" key={`${item.title}-${item.issuedAt}`}>
                      <strong>{item.title}</strong>
                      <p>{item.description}</p>
                    </div>
                  ))}
                </div>
              )}
            </article>
          </section>

          <section className="panel-card hidden-panel">
            <div className="panel-head">
              <h3>Job Description Matching</h3>
              <p>See how your current readiness compares with live job opportunities.</p>
            </div>
            <div className="achievement-stack">
              {(dashboard?.jobMatches || []).length ? dashboard.jobMatches.map((job) => (
                <a className="achievement-card" key={`${job.title}-${job.company}`} href={job.url} target="_blank" rel="noreferrer">
                  <strong>{job.title}</strong>
                  <p>{job.company} • {job.location}</p>
                  <p>Match Score: {job.matchScore}%</p>
                  <p>Matched: {job.matchedSkills.join(", ") || "None"}</p>
                  <p>Missing: {job.missingSkills.join(", ") || "None"}</p>
                </a>
              )) : <div className="empty-inline">No live job matches available right now.</div>}
            </div>
          </section>

          <section className="panel-card roadmap-panel">
            <div className="panel-head row-between">
              <div>
                <h3>Weekly Learning Roadmap</h3>
                <p>Follow your path week by week and open each stop to see the full course details.</p>
              </div>
              <button className="primary-btn small" onClick={() => setPage("roadmap")}>Open Roadmap</button>
            </div>

            {roadmap.length ? (
              <RoadmapPathView groupedRoadmap={groupedRoadmap} selectedSkill={selectedRoadmapItem?.skillName} onSelect={openRoadmapDetail} />
            ) : <div className="empty-card">No roadmap yet.</div>}
          </section>
        </main>
      )}

      {session && page === "roadmap" && (
        <main className="page-shell">
          <section className="panel-card roadmap-page-card">
            <div className="panel-head row-between">
              <div>
                <div className="eyebrow">Roadmap</div>
                <h2>Weekly Learning Path</h2>
                <p>Move through the path in order and open any course stop to see the full details.</p>
              </div>
              <button className="secondary-btn" onClick={() => setPage("dashboard")}>Back To Dashboard</button>
            </div>
            {roadmap.length ? (
              <RoadmapPathView groupedRoadmap={groupedRoadmap} selectedSkill={selectedRoadmapItem?.skillName} onSelect={openRoadmapDetail} />
            ) : (
              <div className="empty-card">No roadmap yet.</div>
            )}
          </section>
        </main>
      )}

      {session && page === "roadmap-detail" && selectedRoadmapItem && (
        <main className="page-shell narrow">
          <section className="panel-card roadmap-page-card">
            <div className="panel-head row-between">
              <button className="secondary-btn" onClick={() => setPage("roadmap")}>Back To Roadmap</button>
              <button className="primary-btn small" onClick={() => openQuiz(selectedRoadmapItem.skillName)}>Take Quiz</button>
            </div>
            <RoadmapDetailView item={selectedRoadmapItem} />
          </section>
        </main>
      )}

      {session && page === "quiz" && (
        <main className="page-shell narrow">
          {!quiz ? (
            <section className="panel-card quiz-launch-card">
              <div className="quiz-launch-hero">
                <div>
                  <div className="eyebrow">Assessment Studio</div>
                  <h2>Skill Quiz Center</h2>
                </div>
                <div className="quiz-launch-badge">
                  <span>TRACK</span>
                  <strong>{roadmap.length}</strong>
                  <small>skills ready for quiz</small>
                </div>
              </div>
              <div className="roadmap-list quiz-skill-grid">
                {roadmap.map((item) => (
                  <button key={item.skillName} className="quiz-skill-btn" onClick={() => openQuiz(item.skillName)}>
                    <span className="quiz-skill-symbol" aria-hidden="true">{skillSymbol(item.skillName)}</span>
                    <span className="quiz-skill-copy">
                      <strong>{item.skillName}</strong>
                      <small>{item.weekLabel} • {item.targetLevel}</small>
                    </span>
                    <span className={`status-pill ${item.status.toLowerCase()}`}>{item.status.replaceAll("_", " ")}</span>
                  </button>
                ))}
              </div>
            </section>
          ) : (
            <section className={`panel-card quiz-shell ${quizTheme(quiz.skillName).tone}`}>
              <div className="quiz-hero">
                <div className="quiz-hero-copy">
                  <div className="eyebrow">Assessment</div>
                  <h3>{quiz.skillName} Quiz</h3>
                  <p>{quiz.questions.length} questions • {quiz.recommendedDifficulty}</p>
                </div>
                <div className="quiz-hero-mark">
                  <span>{quizTheme(quiz.skillName).label}</span>
                  <strong>{quizTheme(quiz.skillName).symbol}</strong>
                </div>
              </div>

              <div className="quiz-insight-grid">
                <div className="quiz-insight-card">
                  <span>Progress</span>
                  <strong>{answeredQuizCount(quiz.questions, quizAnswers)}/{quiz.questions.length}</strong>
                  <small>questions answered</small>
                </div>
                <div className="quiz-insight-card">
                  <span>Difficulty</span>
                  <strong>{quiz.recommendedDifficulty}</strong>
                  <small>recommended level</small>
                </div>
                <div className="quiz-insight-card confidence-card">
                  <div className="confidence-row">
                    <span>Confidence Before Quiz</span>
                    <strong>{quizConfidence}%</strong>
                  </div>
                  <input type="range" min="0" max="100" value={quizConfidence} onChange={(e) => setQuizConfidence(Number(e.target.value))} />
                  <small>Move the slider based on how ready you feel right now.</small>
                </div>
              </div>

              <div className="quiz-jump-row">
                {quiz.questions.map((question, index) => {
                  const answered = !!quizAnswers[question.id]?.toString().trim();
                  const active = index === quizIndex;
                  return (
                    <button
                      key={question.id}
                      type="button"
                      className={`quiz-jump-pill ${answered ? "answered" : "unanswered"} ${active ? "active" : ""}`}
                      onClick={() => jumpToQuiz(index)}
                    >
                      {String(index + 1).padStart(2, "0")}
                    </button>
                  );
                })}
              </div>

              {currentQuizQuestion && (
                <article className="question-block quiz-question-card single-question-card" key={currentQuizQuestion.id}>
                  <div className="quiz-question-head">
                    <div className="quiz-question-number">{String(quizIndex + 1).padStart(2, "0")}</div>
                    <div>
                      <h4>Question {quizIndex + 1}</h4>
                      <small className="question-progress-label">
                        Question {quizIndex + 1} of {quiz.questions.length}
                      </small>
                      <div className="question-meta">
                        <span className="pill">{currentQuizQuestion.questionType}</span>
                        <span className="pill">{currentQuizQuestion.difficulty}</span>
                        <span className="pill">{currentQuizQuestion.concept}</span>
                      </div>
                    </div>
                  </div>
                  <p>{currentQuizQuestion.prompt}</p>
                  {currentQuizQuestion.options?.length ? (
                    <div className="option-stack">
                      {currentQuizQuestion.options.map((option, optionIndex) => (
                        <label className={`option-card quiz-option-card ${quizAnswers[currentQuizQuestion.id] === option ? "selected" : ""}`} key={option}>
                          <input
                            type="radio"
                            name={currentQuizQuestion.id}
                            checked={quizAnswers[currentQuizQuestion.id] === option}
                            onChange={() => setQuizAnswers({ ...quizAnswers, [currentQuizQuestion.id]: option })}
                          />
                          <span className="option-token">{String.fromCharCode(65 + optionIndex)}</span>
                          <span>{option}</span>
                        </label>
                      ))}
                    </div>
                  ) : (
                    <textarea
                      className="assessment-textarea quiz-textarea"
                      placeholder="Write your answer"
                      value={quizAnswers[currentQuizQuestion.id] || ""}
                      onChange={(e) => setQuizAnswers({ ...quizAnswers, [currentQuizQuestion.id]: e.target.value })}
                    />
                  )}
                  <div className="question-flow-actions">
                    <button className="secondary-btn" disabled={quizIndex === 0} onClick={() => moveQuiz(-1)}>Back</button>
                    {quizIndex < quiz.questions.length - 1 && (
                      <button className="primary-btn" onClick={() => moveQuiz(1)}>Next</button>
                    )}
                  </div>
                </article>
              )}
              <div className="footer-actions">
                <button className="secondary-btn" onClick={() => setPage("dashboard")}>Back</button>
                <button className="primary-btn" onClick={submitQuiz}>Submit</button>
              </div>
              {quizResult && (
                <div className="quiz-result-card vivid-quiz-result">
                  <div className="quiz-result-hero">
                    <div className="quiz-score-orb">
                      <span>Score</span>
                      <strong>{quizResult.percentage}%</strong>
                    </div>
                    <div className="quiz-result-copy">
                      <h3>{quiz.skillName} Assessment Result</h3>
                      <p>Confidence submitted {quizResult.submittedConfidence}% • measured {quizResult.measuredConfidence}%</p>
                    </div>
                  </div>
                  <div className="quiz-result-grid">
                    {quizResult.feedback.map((item) => <div key={item} className="result-row">{item}</div>)}
                    {!!quizResult.weakAreas?.length && <div className="result-row">Weak areas: {quizResult.weakAreas.join(", ")}</div>}
                    {!!quizResult.nextActions?.length && <div className="result-row">Next actions: {quizResult.nextActions.join(" | ")}</div>}
                    <div className="result-row">Attempt: {quizResult.attemptNumber} | Improvement: {quizResult.improvementDelta}%</div>
                    {!!quizResult.weakAreaInsights?.length && quizResult.weakAreaInsights.map((item) => (
                      <div className="result-row" key={`${item.skillName}-${item.firstStep}`}>
                        <strong>{item.skillName}:</strong> {item.explanation} Start with {item.firstStep}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </section>
          )}
        </main>
      )}

    </div>
  );
}

function TopNav({ session, page, setPage, setAuthMode, logout }) {
  return (
    <header className="top-nav">
      <div className="nav-shell">
        <button className="brand-link" aria-label="Skill Gap Navigator" onClick={() => setPage(session ? "dashboard" : "home")}>
          <span className="brand-box">
            <img className="brand-logo" src="/images/skill-gap-logo.svg" alt="" />
          </span>
          {!(page === "home" && !session) && <span className="brand-title">Skill Gap Navigator</span>}
        </button>

        <nav className="nav-links">
          {session ? (
            <>
              <button className={page === "dashboard" ? "nav-active" : ""} onClick={() => setPage("dashboard")}>Dashboard</button>
              <button className={page === "role-selection" ? "nav-active" : ""} onClick={() => setPage("role-selection")}>Roles</button>
              <button className={page === "resume-analyzer" ? "nav-active" : ""} onClick={() => setPage("resume-analyzer")}>Resume</button>
              <button className={page === "quiz" ? "nav-active" : ""} onClick={() => setPage("quiz")}>Quiz</button>
              <button className="outline-btn" onClick={logout}>Logout</button>
            </>
          ) : (
            <>
              <button onClick={() => setPage("home")}>Home</button>
              <button onClick={() => { setAuthMode("login"); setPage("login"); }}>Log in</button>
              <button className="primary-btn small" onClick={() => { setAuthMode("register"); setPage("register"); }}>Sign up</button>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}

function FeatureCard({ title, text }) {
  const meta = featureMeta(title);
  return (
    <article className={`feature-card ${meta.tone}`}>
      <div className="feature-card-top">
        <div className="feature-icon">
          <span>{meta.symbol}</span>
        </div>
        <div className="feature-mini-badge">{meta.eyebrow}</div>
      </div>
      <h3>{title}</h3>
      <p>{text}</p>
      <div className="feature-path-line"></div>
    </article>
  );
}

function MetricCard({ title, value }) {
  return (
    <article className="metric-card">
      <span>{title}</span>
      <strong>{value}</strong>
    </article>
  );
}

function ProgressChart({ dashboard }) {
  const completed = dashboard?.completedSkills ?? 0;
  const inProgress = dashboard?.inProgressSkills ?? 0;
  const pending = dashboard?.pendingSkills ?? 0;
  const total = Math.max(completed + inProgress + pending, 1);
  return (
    <div className="chart-wrap">
      <div
        className="chart-ring"
        style={{
          background: `conic-gradient(#2f6fe4 0 ${(completed / total) * 360}deg, #22b6cf ${(completed / total) * 360}deg ${((completed + inProgress) / total) * 360}deg, #d8e7f6 ${((completed + inProgress) / total) * 360}deg 360deg)`
        }}
      >
        <div className="chart-center">{dashboard?.readinessScore ?? 0}%</div>
      </div>
    </div>
  );
}

function RoadmapPathView({ groupedRoadmap, selectedSkill, onSelect }) {
  const palette = ["blue", "green", "orange", "teal", "yellow", "coral", "indigo", "olive", "amber"];
  const steps = Object.entries(groupedRoadmap).flatMap(([weekLabel, items]) =>
    items.map((item, index) => ({ ...item, weekLabel, localIndex: index + 1 }))
  );

  return (
    <div className="roadmap-path-layout creative-roadmap">
      <div className="roadmap-flow-grid">
        {steps.map((item, index) => {
          const tone = palette[index % palette.length];
          const isActive = selectedSkill === item.skillName;
          const layout = index % 6 < 3 ? "top" : "bottom";
          const align = index % 3;
          return (
            <button
              key={`${item.weekLabel}-${item.skillName}`}
              className={`flow-step-card ${tone} ${layout} ${isActive ? "active" : ""} col-${align + 1}`}
              onClick={() => onSelect(item.skillName)}
            >
              <span className="flow-step-number">{String(index + 1).padStart(2, "0")}</span>
              <span className="flow-step-week">{item.weekLabel}</span>
              <div className="flow-step-node">{item.skillName.charAt(0)}</div>
              <div className="flow-step-panel">
                <div className="flow-step-panel-glow"></div>
                <strong>{item.skillName}</strong>
                <p>{item.description}</p>
                <small>{item.estimatedHours} hours | Target {item.targetLevel}</small>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function RoadmapDetailView({ item }) {
  const [activeSection, setActiveSection] = useState("overview");

  return (
    <div className="roadmap-detail-panel standalone-detail">
      <div className="course-hero-card">
        <div className="course-hero-copy">
          <div className="roadmap-meta">
            <span className="week-badge">{item.weekLabel}</span>
            <span className="skill-chip">Target {item.targetLevel}</span>
            <span className={`status-pill ${item.status.toLowerCase()}`}>{item.status.replaceAll("_", " ")}</span>
          </div>
          <h4>{item.skillName}</h4>
          <p>{item.description}</p>
          <p className="weekly-goal">{item.weeklyGoal}</p>
        </div>
        <div className="course-orbit">
          <div className="course-orbit-core" aria-hidden="true">{skillSymbol(item.skillName)}</div>
          <span className="orbit-chip orbit-chip-a">PATH</span>
          <span className="orbit-chip orbit-chip-b">{item.estimatedHours}H</span>
          <span className="orbit-chip orbit-chip-c">{item.proficiencyScore}%</span>
        </div>
      </div>

      <div className="course-visual-strip">
        <div className="visual-tile visual-tile-primary">
          <span>Target</span>
          <strong>{item.targetLevel}</strong>
          <small>{item.estimatedHours} hour plan</small>
        </div>
        <div className="visual-tile visual-tile-secondary">
          <span>Focus</span>
          <strong>{item.category}</strong>
          <small>{item.confidenceScore ?? 0}% confidence</small>
        </div>
        <div className="visual-tile visual-tile-accent">
          <span>Milestone</span>
          <strong>{item.weekLabel}</strong>
          <small>{item.proficiencyScore}% proficiency</small>
        </div>
      </div>

      <div className="detail-tab-bar">
        {[
          ["overview", "Overview"],
          ["journey", "Journey"],
          ["resources", "Resources"]
        ].map(([key, label]) => (
          <button
            key={key}
            className={`detail-tab ${activeSection === key ? "active" : ""}`}
            onClick={() => setActiveSection(key)}
          >
            {label}
          </button>
        ))}
      </div>

      {activeSection === "overview" && (
        <div className="detail-section-grid">
          <div className="course-signal-grid compact-signal-grid">
            <div className="course-signal-card">
              <span className="signal-mark">01</span>
              <strong>Prerequisites</strong>
              <p>{item.prerequisites?.join(", ") || "None"}</p>
            </div>
            <div className="course-signal-card">
              <span className="signal-mark">02</span>
              <strong>Confidence</strong>
              <p>{item.confidenceScore ?? 0}% ready</p>
            </div>
            <div className="course-signal-card">
              <span className="signal-mark">03</span>
              <strong>Reminder</strong>
              <p>{item.reminderDate ? `${item.reminderDate} - ${item.reminderMessage}` : "No reminder set yet."}</p>
            </div>
          </div>

          <div className="project-grid creative-project-grid">
            <div className="project-card feature-project-card">
              <span className="detail-label">Mini Project</span>
              <strong>{item.miniProject?.title}</strong>
              <p>{item.miniProject?.objective}</p>
              <div className="project-symbol-row">
                <span className="project-symbol">BUILD</span>
                <span className="project-symbol">PRACTICE</span>
              </div>
            </div>
            <div className="project-card feature-project-card">
              <span className="detail-label">Portfolio Project</span>
              <strong>{item.portfolioProject?.title}</strong>
              <p>{item.portfolioProject?.objective}</p>
              <div className="project-symbol-row">
                <span className="project-symbol">SHOWCASE</span>
                <span className="project-symbol">OUTCOME</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeSection === "journey" && (
        <div className="guided-path-card interactive-path-card">
          <div className="resource-top">
            <strong>Learning Journey</strong>
            <span>{item.proficiencyScore}% proficiency</span>
          </div>
          <div className="journey-path-line"></div>
          <div className="guided-step-list guided-step-path">
            {item.guidedPath?.map((step) => (
              <div key={`${item.skillName}-${step.day}`} className="guided-step journey-step">
                <div className="journey-step-dot">{step.day}</div>
                <div className="journey-step-copy">
                  <strong>Day {step.day}</strong>
                  <span>{step.title}</span>
                  <small>{step.goal}</small>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeSection === "resources" && (
        <div className="resource-list interactive-resource-list compact-resource-grid">
          {item.resources?.map((resource) => (
            <a
              key={`${item.skillName}-${resource.title}`}
              className={`resource-card vivid-resource-card ${resourceTone(resource)}`}
              href={resource.url}
              target="_blank"
              rel="noreferrer"
            >
              <div className="resource-symbol">{renderResourceLogo(resource)}</div>
              <div className="resource-top">
                <strong>{resource.title}</strong>
                <span>{resource.type}</span>
              </div>
              <div className="resource-chip-row">
                <span className="resource-chip">{resource.platform}</span>
                <span className="resource-chip">{resource.duration}</span>
                <span className="resource-chip">{resource.level}</span>
              </div>
              <p>{resource.language} | {resource.budget}</p>
              <p>Rating {resource.rating} | Practical {resource.practicalScore}/100</p>
              <small>{resource.description}</small>
              <div className="resource-open-row">
                <span>Open Resource</span>
                <span>></span>
              </div>
            </a>
          ))}
        </div>
      )}
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<App />);

function resourceTone(resource) {
  const type = (resource.type || "").toLowerCase();
  if (type.includes("video")) return "resource-blue";
  if (type.includes("documentation")) return "resource-violet";
  if (type.includes("practice")) return "resource-teal";
  if (type.includes("project")) return "resource-orange";
  return "resource-sky";
}

function resourceSymbol(resource) {
  const type = `${resource.type || ""} ${resource.platform || ""} ${resource.title || ""}`.toLowerCase();
  if (type.includes("youtube") || type.includes("video")) return "▶";
  if (type.includes("google") || type.includes("documentation") || type.includes("docs")) return "📘";
  if (type.includes("practice") || type.includes("lab") || type.includes("exercise")) return "🧪";
  if (type.includes("project")) return "🧩";
  if (type.includes("quiz")) return "❓";
  if (type.includes("coursera")) return "🎓";
  if (type.includes("udemy")) return "🟣";
  if (type.includes("github")) return "🐙";
  return "📚";
}

function resourceLogoUrl(resource) {
  const source = `${resource.platform || ""} ${resource.title || ""} ${resource.url || ""}`.toLowerCase();
  if (source.includes("youtube")) return "https://cdn.simpleicons.org/youtube/ffffff";
  if (source.includes("coursera")) return "https://cdn.simpleicons.org/coursera/ffffff";
  if (source.includes("udemy")) return "https://cdn.simpleicons.org/udemy/ffffff";
  if (source.includes("edx")) return "https://cdn.simpleicons.org/edx/ffffff";
  if (source.includes("freecodecamp")) return "https://cdn.simpleicons.org/freecodecamp/ffffff";
  if (source.includes("khan academy")) return "https://cdn.simpleicons.org/khanacademy/ffffff";
  if (source.includes("github")) return "https://cdn.simpleicons.org/github/ffffff";
  if (source.includes("google")) return "https://cdn.simpleicons.org/google/ffffff";
  if (source.includes("mdn")) return "https://cdn.simpleicons.org/mdnwebdocs/ffffff";
  if (source.includes("microsoft")) return "https://cdn.simpleicons.org/microsoft/ffffff";
  if (source.includes("aws") || source.includes("amazon web services")) return "https://cdn.simpleicons.org/amazonwebservices/ffffff";
  if (source.includes("oracle")) return "https://cdn.simpleicons.org/oracle/ffffff";
  return "";
}

function renderResourceLogo(resource) {
  const logoUrl = resourceLogoUrl(resource);
  if (logoUrl) {
    const name = resource.platform || resource.title || "Resource";
    return <img className="resource-logo" src={logoUrl} alt={`${name} logo`} />;
  }

  const fallback = `${resource.type || ""} ${resource.platform || ""}`.toLowerCase();
  if (fallback.includes("video") || fallback.includes("youtube")) return <span className="resource-fallback">PLY</span>;
  if (fallback.includes("documentation") || fallback.includes("docs")) return <span className="resource-fallback">DOC</span>;
  if (fallback.includes("practice") || fallback.includes("lab")) return <span className="resource-fallback">LAB</span>;
  if (fallback.includes("project")) return <span className="resource-fallback">PRJ</span>;
  return <span className="resource-fallback">RES</span>;
}

function roleSymbol(roleName = "") {
  const role = roleName.toLowerCase();
  if (role.includes("full stack")) return "🌐";
  if (role.includes("frontend") || role.includes("ui") || role.includes("react")) return "⚛";
  if (role.includes("backend") || role.includes("java")) return "🛠";
  if (role.includes("data")) return "📊";
  if (role.includes("python")) return "🐍";
  if (role.includes("mobile") || role.includes("android") || role.includes("ios")) return "📱";
  if (role.includes("cloud") || role.includes("devops")) return "☁";
  if (role.includes("security")) return "🔒";
  if (role.includes("design")) return "🎨";
  return "🚀";
}

function skillSymbol(skillName = "") {
  const skill = skillName.toLowerCase();
  if (skill.includes("python")) return "🐍";
  if (skill.includes("java")) return "☕";
  if (skill.includes("react")) return "⚛";
  if (skill.includes("javascript")) return "🟨";
  if (skill.includes("typescript")) return "🔷";
  if (skill.includes("sql")) return "🗄";
  if (skill.includes("excel")) return "📗";
  if (skill.includes("statistics")) return "📈";
  if (skill.includes("communication")) return "💬";
  if (skill.includes("visualization")) return "📊";
  if (skill.includes("spring")) return "🌱";
  if (skill.includes("html")) return "🌍";
  if (skill.includes("css")) return "🎨";
  if (skill.includes("git")) return "🔀";
  if (skill.includes("cloud")) return "☁";
  return "📘";
}
