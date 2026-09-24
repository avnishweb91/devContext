"use client";

import { FormEvent, useEffect, useState } from "react";
import { ArrowRight, Building2, CheckCircle2 } from "lucide-react";
import "./onboarding.css";

export default function OnboardingPage() {
  const [companyName, setCompanyName] = useState("");
  const [ownerName, setOwnerName] = useState("");
  const [ownerEmail, setOwnerEmail] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    fetch("/api/workspaces")
      .then(response => response.ok ? response.json() : Promise.reject())
      .then((items: { id: string }[]) => { if (items.length > 0) window.location.assign("/"); })
      .catch(() => undefined);
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const response = await fetch("/api/onboarding/workspaces", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ companyName, ownerName, ownerEmail }),
      });
      if (!response.ok) throw new Error("We could not create your workspace.");
      window.location.assign("/");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Something went wrong.");
      setSaving(false);
    }
  }

  return <main className="onboarding-page"><section className="onboarding-card"><div className="onboarding-mark"><Building2 size={22} /></div><p className="eyebrow">DevContext workspace</p><h1>Set up your engineering workspace</h1><p className="intro">Connect your company’s engineering systems and give your team one trusted place for context, verification, and release readiness.</p><div className="onboarding-benefits"><span><CheckCircle2 size={16} /> Workspace-isolated data</span><span><CheckCircle2 size={16} /> GitHub, Jira, and Slack ready</span><span><CheckCircle2 size={16} /> Invite your engineering team later</span></div><form onSubmit={submit}><label>Company or team name<input required maxLength={180} value={companyName} onChange={event => setCompanyName(event.target.value)} placeholder="Acme Technologies" /></label><label>Your name<input required maxLength={180} value={ownerName} onChange={event => setOwnerName(event.target.value)} placeholder="Avnish Sharma" /></label><label>Work email<input required type="email" maxLength={320} value={ownerEmail} onChange={event => setOwnerEmail(event.target.value)} placeholder="you@company.com" /></label>{error && <p className="form-error" role="alert">{error}</p>}<button type="submit" disabled={saving}>{saving ? "Creating workspace…" : "Create workspace"}<ArrowRight size={16} /></button></form></section></main>;
}
