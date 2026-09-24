"use client";

import { FormEvent, useEffect, useState } from "react";
import { CheckCircle2, Copy, Users } from "lucide-react";
import Link from "next/link";
import "./team.css";

type Workspace = { id: string; name: string };
type Invite = { email: string; role: string; token: string; expiresAt: string };

export default function TeamPage() {
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [workspaceId, setWorkspaceId] = useState("");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("DEVELOPER");
  const [invite, setInvite] = useState<Invite | null>(null);
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetch("/api/workspaces").then(response => response.ok ? response.json() : Promise.reject()).then((items: Workspace[]) => {
      setWorkspaces(items); if (items[0]) setWorkspaceId(items[0].id);
    }).catch(() => setMessage("Sign in and create a workspace before inviting teammates."));
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setSaving(true); setMessage(""); setInvite(null);
    try {
      const response = await fetch(`/api/workspaces/${workspaceId}/invites`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, role }) });
      const data = await response.json(); if (!response.ok) throw new Error(data.message || "Could not create invitation");
      setInvite(data); setEmail("");
    } catch (error) { setMessage(error instanceof Error ? error.message : "Could not create invitation"); }
    finally { setSaving(false); }
  }

  async function copyInvite() {
    if (!invite) return;
    await navigator.clipboard.writeText(`${window.location.origin}/accept-invite?token=${encodeURIComponent(invite.token)}`);
    setMessage("Invitation link copied.");
  }

  return <main className="team-page"><div className="team-top"><Link href="/">← Back to dashboard</Link><span>DevContext</span></div><section className="team-card"><div className="team-icon"><Users size={25} /></div><p className="eyebrow">Workspace administration</p><h1>Invite your team</h1><p className="muted">Invite developers, QA, DevOps, and engineering leads with the least access they need.</p>{workspaces.length > 0 && <label>Workspace<select value={workspaceId} onChange={event => setWorkspaceId(event.target.value)}>{workspaces.map(workspace => <option key={workspace.id} value={workspace.id}>{workspace.name}</option>)}</select></label>}<form onSubmit={submit}><label>Work email<input required type="email" value={email} onChange={event => setEmail(event.target.value)} placeholder="teammate@company.com" /></label><label>Role<select value={role} onChange={event => setRole(event.target.value)}><option>DEVELOPER</option><option>QA</option><option>DEVOPS</option><option>ENGINEERING_LEAD</option><option>VIEWER</option></select></label>{message && <p className="team-message" role="status">{message}</p>}<button disabled={saving || !workspaceId}>{saving ? "Creating invitation…" : "Create invitation"}</button></form>{invite && <div className="invite-result"><CheckCircle2 size={18} /><div><strong>Invitation ready for {invite.email}</strong><small>Expires {new Date(invite.expiresAt).toLocaleDateString()}</small></div><button className="copy-button" onClick={copyInvite}><Copy size={15} /> Copy link</button></div>}</section></main>;
}
