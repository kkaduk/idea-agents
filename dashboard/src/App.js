import React, { useEffect, useState, useRef } from "react";

const AGENTS = [
  { id: "human-agent", display: "Human Agent", color: "#ffc800" },
  { id: "idea-creator-agent", display: "Creator Agent", color: "#73f7dd" },
  { id: "idea-critic-agent", display: "Critic Agent", color: "#f35b53" },
  { id: "idea-finalizer-agent", display: "Finalizer Agent", color: "#d473f7" },
  { id: "risk-estimator-agent", display: "Risk Estimator", color: "#6fa8dc" },
];

const LOG_POLL_INTERVAL = 3000; // ms

function App() {
  const [idea, setIdea] = useState("");
  const [orchestrateStatus, setOrchestrateStatus] = useState("");
  const [logs, setLogs] = useState({});
  const [loadingLogs, setLoadingLogs] = useState(false);
  const logRefs = useRef({});

  useEffect(() => {
    fetchAllLogs();
    const interval = setInterval(fetchAllLogs, LOG_POLL_INTERVAL);
    return () => clearInterval(interval);
    // eslint-disable-next-line
  }, []);

  function fetchAllLogs() {
    setLoadingLogs(true);
    Promise.all(
      AGENTS.map((agent) =>
        fetch(`/api/logs/${agent.id}?lines=100`).then((res) => res.text())
      )
    ).then((results) => {
      const next = {};
      AGENTS.forEach((agent, idx) => {
        next[agent.id] = results[idx];
      });
      setLogs(next);
      setLoadingLogs(false);
      // Scroll to bottom of each log (after DOM update)
      setTimeout(() => {
        AGENTS.forEach((agent) => {
          if (logRefs.current[agent.id]) {
            logRefs.current[agent.id].scrollTop = logRefs.current[agent.id].scrollHeight;
          }
        });
      }, 100);
    });
  }

  function handleOrchestrate(e) {
    e.preventDefault();
    setOrchestrateStatus("Starting...");
    fetch("/api/product-ideas/orchestrate", {
      method: "POST",
      headers: { "Content-Type": "text/plain" },
      body: idea.trim(),
    })
      .then((res) => res.text())
      .then((msg) => {
        setOrchestrateStatus(`Response: ${msg}`);
        fetchAllLogs();
      })
      .catch((err) => setOrchestrateStatus("Error: " + err.message));
  }

  return (
    <div className="dashboard-root">
      <h1>Idea Agents Diagnostic Dashboard</h1>
      <section className="idea-input-section">
        <form onSubmit={handleOrchestrate}>
          <input
            type="text"
            placeholder="Describe your idea..."
            value={idea}
            onChange={(e) => setIdea(e.target.value)}
            className="idea-input"
            required
          />
          <button type="submit" className="orchestrate-btn">
            Orchestrate Product Development
          </button>
        </form>
        <div className="orchestrate-status">{orchestrateStatus}</div>
      </section>
      <h2>Agent Logs</h2>
      <div className="logs-wrapper">
        {AGENTS.map((agent) => (
          <div className="log-panel" key={agent.id}>
            <div
              className="log-header"
              style={{ background: agent.color, color: "#222", fontWeight: 700 }}
            >
              {agent.display}
            </div>
            <pre
              className="log-output"
              ref={(el) => (logRefs.current[agent.id] = el)}
              style={{
                background: "#14172b",
                color: "#eaeaea",
                borderLeft: `5px solid ${agent.color}`,
              }}
            >
              {logs[agent.id] || (loadingLogs ? "Loading..." : "No log.")}
            </pre>
          </div>
        ))}
      </div>
    </div>
  );
}

export default App;
