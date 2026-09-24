"use client";

import { useEffect, useState } from "react";
import { ArrowLeft, CheckCircle2, GitPullRequest, ShieldAlert } from "lucide-react";
import Link from "next/link";
import "./pull-requests.css";

type Workspace = { id: string; name: string };
type PullRequest = { id: string; repository: string; number: number; title: string; author: string; state: string; url: string; verificationStatus: string };

export default function PullRequestsPage() {
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [selected, setSelected] = useState("");
  const [pullRequests, setPullRequests] = useState<PullRequest[]>([]);
  const [message, setMessage] = useState("");
  const [verifying, setVerifying] = useState("");

  useEffect(() => {
    fetch("/api/workspaces").then(response => response.ok ? response.json() : Promise.reject()).then((items: Workspace[]) => {
      setWorkspaces(items);
      if (items[0]) setSelected(items[0].id);
    }).catch(() => setMessage("Sign in and create a workspace to view pull requests."));
  }, []);

  useEffect(() => {
    if (!selected) return;
    fetch(`/api/workspaces/${selected}/pull-requests`).then(response => response.ok ? response.json() : Promise.reject()).then(setPullRequests).catch(() => setMessage("Unable to load pull requests. Sync GitHub first."));
  }, [selected]);

  async function verify(id: string) {
    setVerifying(id); setMessage("");
    try {
      const response = await fetch(`/api/workspaces/${selected}/pull-requests/${id}/verify`, { method: "POST" });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || "Verification failed");
      setPullRequests(items => items.map(item => item.id === id ? { ...item, verificationStatus: data.status } : item));
    } catch (error) { setMessage(error instanceof Error ? error.message : "Verification failed"); }
    finally { setVerifying(""); }
  }

  return <main className="prs-page"><div className="prs-top"><Link href="/">← Back to dashboard</Link><span>DevContext</span></div><section className="prs-card"><div className="prs-heading"><div><p className="eyebrow">Release verification</p><h1>Pull requests</h1><p className="muted">Review repository changes, record evidence, and keep each workspace isolated.</p></div><GitPullRequest size={30} /></div>{workspaces.length > 0 && <label className="workspace-select">Workspace<select value={selected} onChange={event => setSelected(event.target.value)}>{workspaces.map(workspace => <option key={workspace.id} value={workspace.id}>{workspace.name}</option>)}</select></label>}{message && <p className="prs-message" role="alert">{message}</p>}{pullRequests.length === 0 && !message && <p className="empty-state">No synced pull requests yet. Open Integrations and sync GitHub.</p>}<div className="pr-list">{pullRequests.map(pr => <article className="pr-row" key={pr.id}><div className="pr-icon">{pr.verificationStatus === "REVIEW_REQUIRED" ? <ShieldAlert size={20} /> : <CheckCircle2 size={20} />}</div><div className="pr-content"><strong>{pr.title}</strong><small>{pr.repository} · #{pr.number} · {pr.author}</small></div><span className={`pr-status ${pr.verificationStatus.toLowerCase()}`}>{pr.verificationStatus.replace("_", " ")}</span><button onClick={() => verify(pr.id)} disabled={verifying === pr.id}>{verifying === pr.id ? "Checking…" : "Verify"}</button></article>)}</div></section></main>;
}
