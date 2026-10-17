import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "CloudTrap AI | Cloud Security Intelligence",
    template: "%s | CloudTrap AI",
  },
  description:
      "Investigate cloud security posture, explore findings, and analyze risk with CloudTrap AI.",
  applicationName: "CloudTrap AI",
  robots: {
    index: false,
    follow: false,
  },
};

export const viewport: Viewport = {
  themeColor: "#0b0a16",
  colorScheme: "dark",
};

export default function RootLayout({
                                     children,
                                   }: Readonly<{
  children: React.ReactNode;
}>) {
  return (
      <html lang="en">
      <body>{children}</body>
      </html>
  );
}