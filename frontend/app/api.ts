let csrfRequest: Promise<void> | null = null;

function cookieValue(name: string): string | null {
  if (typeof document === "undefined") return null;
  const prefix = `${name}=`;
  const value = document.cookie.split("; ").find(item => item.startsWith(prefix))?.slice(prefix.length);
  return value ? decodeURIComponent(value) : null;
}

async function ensureCsrfToken() {
  if (cookieValue("XSRF-TOKEN")) return;
  csrfRequest ??= fetch("/api/auth/csrf", { credentials: "include" })
    .then(response => { if (!response.ok) throw new Error("Unable to initialize secure session"); })
    .finally(() => { csrfRequest = null; });
  await csrfRequest;
}

export async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? "GET").toString().toUpperCase();
  if (process.env.NODE_ENV === "production" && !["GET", "HEAD", "OPTIONS"].includes(method)) {
    await ensureCsrfToken();
  }
  const headers = new Headers(init.headers);
  const token = cookieValue("XSRF-TOKEN");
  if (token) headers.set("X-XSRF-TOKEN", token);
  return fetch(input, { ...init, headers, credentials: "include" });
}
