import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = { title: "DevContext", description: "Engineering context and release verification" };

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}

