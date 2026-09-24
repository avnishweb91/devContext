import type { NextConfig } from "next";

const configuredBackendUrl = process.env.BACKEND_URL?.trim().replace(/\/$/, "");
if (process.env.VERCEL === "1" && !configuredBackendUrl) {
  throw new Error("BACKEND_URL must be configured for the Vercel deployment");
}
const backendUrl = configuredBackendUrl && /^https?:\/\//i.test(configuredBackendUrl)
  ? configuredBackendUrl
  : "http://localhost:8080";

if (process.env.VERCEL === "1" && !/^https?:\/\//i.test(backendUrl)) {
  throw new Error("BACKEND_URL must start with https:// (or http:// for a local deployment)");
}

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    return [
      { source: "/api/:path*", destination: `${backendUrl}/api/:path*` },
      { source: "/oauth2/:path*", destination: `${backendUrl}/oauth2/:path*` },
      { source: "/login/:path*", destination: `${backendUrl}/login/:path*` },
    ];
  },
};

export default nextConfig;
