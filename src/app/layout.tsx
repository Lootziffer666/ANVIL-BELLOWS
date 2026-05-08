import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Anvil Bellows — Runtime GUI",
  description: "Anvil Bellows: LLM Router, Multi-CustomOpenAI Toolkit & Quota Management",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="de" data-theme="light">
      <body>{children}</body>
    </html>
  );
}
