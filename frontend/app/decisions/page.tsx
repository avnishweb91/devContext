"use client";

import { FormEvent, useEffect, useState } from "react";
import { BookOpen, CheckCircle2 } from "lucide-react";
import Link from "next/link";
import "./decisions.css";

type Workspace = { id: string; name: string };
type Memory = { id: string; title: string; source: string; content: string; createdAt: string };

export default function DecisionsPage() {
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]); const [workspaceId, setWorkspaceId] = useState("");
  const [memories, setMemories] = useState<Memory[]>([]); const [title, setTitle] = useState(""); const [content, setContent] = useState("");
  const [message, setMessage] = useState(""); const [saving, setSaving] = useState(false);
  useEffect(() => { fetch("/api/workspaces").then(r => r.ok ? r.json() : Promise.reject()).then((items: Workspace[]) => { setWorkspaces(items); if (items[0]) setWorkspaceId(items[0].id); }).catch(() => setMessage("Sign in and create a workspace to manage engineering memory.")); }, []);
  useEffect(() => { if (!workspaceId) return; fetch(`/api/workspaces/${workspaceId}/memories`).then(r => r.ok ? r.json() : Promise.reject()).then(setMemories).catch(() => setMessage("Unable to load engineering memory.")); }, [workspaceId]);
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setSaving(true); setMessage(""); try { const response = await fetch(`/api/workspaces/${workspaceId}/memories`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ title, source: "decision", content }) }); const data = await response.json(); if (!response.ok) throw new Error(data.message || "Unable to save decision"); setMemories(items => [data, ...items]); setTitle(""); setContent(""); } catch (error) { setMessage(error instanceof Error ? error.message : "Unable to save decision"); } finally { setSaving(false); } }
  return <main className="decisions-page"><div className="decisions-top"><Link href="/">← Back to dashboard</Link><span>DevContext</span></div><section className="decisions-card"><div className="decisions-icon"><BookOpen size={24} /></div><p className="eyebrow">Engineering memory</p><h1>Decisions your team can trust</h1><p className="muted">Capture why a decision was made so future pull requests and AI reviews have reliable context.</p>{workspaces.length > 0 && <label>Workspace<select value={workspaceId} onChange={event => setWorkspaceId(event.target.value)}>{workspaces.map(workspace => <option key={workspace.id} value={workspace.id}>{workspace.name}</option>)}</select></label>}<form onSubmit={submit}><input required maxLength={300} placeholder="Decision title" value={title} onChange={event => setTitle(event.target.value)} /><textarea required maxLength={12000} placeholder="What was decided, and why?" value={content} onChange={event => setContent(event.target.value)} /><button disabled={saving || !workspaceId}>{saving ? "Saving…" : "Save decision"}</button></form>{message && <p className="decision-message" role="alert">{message}</p>}<div className="memory-list">{memories.map(memory => <article key={memory.id}><CheckCircle2 size={18} /><div><strong>{memory.title}</strong><small>{memory.source} · {new Date(memory.createdAt).toLocaleString()}</small><p>{memory.content}</p></div></article>)}</div></section></main>;
}
