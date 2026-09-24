import type { NextConfig } from "next";

const configuredBackendUrl = process.env.BACKEND_URL?.trim().replace(/\/$/, "");
const backendUrl = configuredBackendUrl && /^https?:\/\//i.test(configuredBackendUrl)
  ? configuredBackendUrl
  : "http://localhost:8080";

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backendUrl}/api/:path*` }];
  },
};

export default nextConfig;
