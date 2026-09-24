"use client";

import { useEffect, useState } from "react";
import { ArrowLeft, CheckCircle2, Github, RefreshCw } from "lucide-react";
import Link from "next/link";
import "./integrations.css";

type Workspace = { id: string; name: string };
type SyncResult = { repositories: number; pullRequests: number; status: string };

export default function IntegrationsPage() {
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [selected, setSelected] = useState("");
  const [syncing, setSyncing] = useState(false);
  const [result, setResult] = useState<SyncResult | null>(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    fetch("/api/workspaces").then(response => response.ok ? response.json() : Promise.reject()).then((items: Workspace[]) => {
      setWorkspaces(items);
      if (items[0]) setSelected(items[0].id);
    }).catch(() => setMessage("Sign in with GitHub and create a workspace before connecting integrations."));
  }, []);

  async function syncGitHub() {
    if (!selected) return;
    setSyncing(true); setMessage(""); setResult(null);
    try {
      const response = await fetch(`/api/integrations/github/workspaces/${selected}/sync`, { method: "POST" });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || "GitHub sync failed");
      setResult(data);
    } catch (error) { setMessage(error instanceof Error ? error.message : "GitHub sync failed"); }
    finally { setSyncing(false); }
  }

  return <main className="integrations-page"><div className="integration-top"><Link href="/">← Back to dashboard</Link><span>DevContext</span></div><section className="integration-card"><div className="integration-icon"><Github size={25} /></div><p className="eyebrow">Workspace integrations</p><h1>Connect your engineering systems</h1><p className="muted">Start with GitHub. DevContext will persist repositories and pull requests inside the selected workspace for verification and engineering memory.</p><div className="provider-row"><div><strong>GitHub</strong><small>Repositories and pull requests</small></div><span className="connected"><CheckCircle2 size={16} /> OAuth ready</span></div>{workspaces.length > 0 && <label className="workspace-select">Workspace<select value={selected} onChange={event => setSelected(event.target.value)}>{workspaces.map(workspace => <option key={workspace.id} value={workspace.id}>{workspace.name}</option>)}</select></label>}{message && <p className="integration-message" role="alert">{message}</p>}{result && <p className="integration-success" role="status"><CheckCircle2 size={16} /> Synced {result.repositories} repositories and {result.pullRequests} pull requests.</p>}<button className="sync-button" onClick={syncGitHub} disabled={syncing || !selected}><RefreshCw size={16} className={syncing ? "spin" : ""} />{syncing ? "Syncing GitHub…" : "Sync GitHub repositories"}</button></section></main>;
}

