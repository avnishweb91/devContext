"use client";

import { FormEvent, useEffect, useState } from "react";
import { CheckCircle2, UserPlus } from "lucide-react";
import { apiFetch } from "../api";
import "./accept-invite.css";

export default function AcceptInvitePage() {
  const [token, setToken] = useState(""); const [message, setMessage] = useState(""); const [accepted, setAccepted] = useState(false);
  useEffect(() => setToken(new URLSearchParams(window.location.search).get("token") || ""), []);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setMessage("");
    const response = await apiFetch("/api/invites/accept", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ token }) });
    const data = await response.json(); if (!response.ok) { setMessage(data.message || "This invitation could not be accepted."); return; } setAccepted(true);
  }
  return <main className="accept-page"><section className="accept-card"><div className="accept-icon">{accepted ? <CheckCircle2 /> : <UserPlus />}</div><p className="eyebrow">DevContext team invitation</p><h1>{accepted ? "You are on the team" : "Accept your invitation"}</h1>{accepted ? <p className="muted">Your workspace access is ready. You can return to the dashboard.</p> : <form onSubmit={submit}><label>Invitation token<input required value={token} onChange={event => setToken(event.target.value)} /></label>{message && <p className="accept-error" role="alert">{message}</p>}<button>Accept invitation</button></form>}</section></main>;
}
