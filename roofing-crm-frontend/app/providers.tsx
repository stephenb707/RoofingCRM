"use client";

import React from "react";
import { ReactQueryProvider } from "../lib/ReactQueryProvider";
import { AuthProvider } from "../lib/AuthContext";

export default function Providers({ children }: { children: React.ReactNode }) {
  return (
    <ReactQueryProvider>
      <AuthProvider>{children}</AuthProvider>
    </ReactQueryProvider>
  );
}
