"use client";

import { useEffect, useState } from "react";
import { ArrowLeft, CheckCircle2, Github, RefreshCw } from "lucide-react";
import Link from "next/link";
import "./integrations.css";

type Workspace = { id: string; name: string };
type SyncResult = { jobId: string; repositories: number; pullRequests: number; status: string };
type Provider = { id: string; name: string; authorizationPath: string };
type ContextSyncResult = { provider: string; importedMemories: number; syncedAt: string; status: string; error?: string };

export default function IntegrationsPage() {
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [selected, setSelected] = useState("");
  const [syncing, setSyncing] = useState(false);
  const [result, setResult] = useState<SyncResult | null>(null);
  const [message, setMessage] = useState("");
  const [providers, setProviders] = useState<Provider[]>([]);
  const [contextSyncing, setContextSyncing] = useState<string | null>(null);

  useEffect(() => {
    fetch("/api/workspaces").then(response => response.ok ? response.json() : Promise.reject()).then((items: Workspace[]) => {
      setWorkspaces(items);
      if (items[0]) setSelected(items[0].id);
    }).catch(() => setMessage("Sign in with GitHub and create a workspace before connecting integrations."));
  }, []);

  useEffect(() => {
    fetch("/api/integrations/providers").then(response => response.ok ? response.json() : Promise.reject()).then(setProviders).catch(() => setProviders([]));
  }, []);

  async function syncGitHub() {
    if (!selected) return;
    setSyncing(true); setMessage(""); setResult(null);
    try {
      const response = await fetch(`/api/integrations/github/workspaces/${selected}/sync`, { method: "POST" });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || "GitHub sync failed");
      let status = data as SyncResult;
      for (let attempt = 0; attempt < 30 && (status.status === "PENDING" || status.status === "RUNNING"); attempt++) {
        await new Promise(resolve => setTimeout(resolve, 1000));
        const statusResponse = await fetch(`/api/integrations/github/workspaces/${selected}/sync-jobs/${status.jobId}`);
        if (!statusResponse.ok) throw new Error("Unable to read sync status");
        status = await statusResponse.json();
      }
      if (status.status === "FAILED") throw new Error("GitHub synchronization failed");
      setResult(status);
    } catch (error) { setMessage(error instanceof Error ? error.message : "GitHub sync failed"); }
    finally { setSyncing(false); }
  }

  async function syncContext(provider: "jira" | "slack") {
    if (!selected) return;
    setContextSyncing(provider); setMessage(""); setResult(null);
    try {
      const response = await fetch(`/api/workspaces/${selected}/integrations/${provider}/sync`, { method: "POST" });
      const data = await response.json() as ContextSyncResult & { message?: string };
      if (!response.ok) throw new Error(data.message || `Unable to sync ${provider}`);
      setMessage(`${provider === "jira" ? "Jira" : "Slack"} imported ${data.importedMemories} context records.`);
    } catch (error) { setMessage(error instanceof Error ? error.message : `Unable to sync ${provider}`); }
    finally { setContextSyncing(null); }
  }

  return <main className="integrations-page"><div className="integration-top"><Link href="/">← Back to dashboard</Link><span>DevContext</span></div><section className="integration-card"><div className="integration-icon"><Github size={25} /></div><p className="eyebrow">Workspace integrations</p><h1>Connect your engineering systems</h1><p className="muted">Connect the systems your team already uses. DevContext keeps every provider scoped to the selected workspace.</p><div className="provider-list">{["GitHub", "Jira", "Slack"].map(name => { const provider = providers.find(item => item.name.toLowerCase() === name.toLowerCase()); return <div className="provider-row" key={name}><div><strong>{name}</strong><small>{name === "GitHub" ? "Repositories and pull requests" : name === "Jira" ? "Issues and delivery context" : "Channels and incident context"}</small></div><div className="provider-actions">{provider ? <a className="connect-link" href={provider.authorizationPath}>Connect</a> : <span className="not-configured">Not configured</span>}{provider && name !== "GitHub" && <button className="mini-sync" onClick={() => syncContext(name.toLowerCase() as "jira" | "slack")} disabled={!selected || contextSyncing !== null}>{contextSyncing === name.toLowerCase() ? "Syncing…" : "Sync context"}</button>}</div></div>; })}</div>{workspaces.length > 0 && <label className="workspace-select">Workspace<select value={selected} onChange={event => setSelected(event.target.value)}>{workspaces.map(workspace => <option key={workspace.id} value={workspace.id}>{workspace.name}</option>)}</select></label>}{message && <p className="integration-message" role="alert">{message}</p>}{result && <p className="integration-success" role="status"><CheckCircle2 size={16} /> Synced {result.repositories} repositories and {result.pullRequests} pull requests.</p>}<button className="sync-button" onClick={syncGitHub} disabled={syncing || !selected}><RefreshCw size={16} className={syncing ? "spin" : ""} />{syncing ? "Syncing GitHub…" : "Sync GitHub repositories"}</button></section></main>;
}
