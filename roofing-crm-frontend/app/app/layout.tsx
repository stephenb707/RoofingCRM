"use client";

import { useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/lib/AuthContext";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { auth, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!auth.token) {
      router.replace("/auth/login");
      return;
    }
    if (!auth.selectedTenantId) {
      router.replace("/auth/select-tenant");
      return;
    }
  }, [auth, router]);

  if (!auth.token || !auth.selectedTenantId) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600"></div>
      </div>
    );
  }

  const currentTenant = auth.tenants.find(
    (t) => t.tenantId === auth.selectedTenantId
  );

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      {/* Header */}
      <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-sky-600 rounded-lg flex items-center justify-center">
            <svg
              className="w-5 h-5 text-white"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
              />
            </svg>
          </div>
          <div>
            <div className="font-semibold text-slate-800 text-sm">
              Roofing CRM
            </div>
            {currentTenant && (
              <div className="text-xs text-slate-500">
                {currentTenant.tenantName}
              </div>
            )}
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="text-right">
            <div className="text-sm font-medium text-slate-700">
              {auth.fullName || auth.email}
            </div>
            <div className="text-xs text-slate-500">{auth.email}</div>
          </div>
          <button
            onClick={() => {
              logout();
              router.push("/auth/login");
            }}
            className="px-3 py-1.5 text-xs font-medium border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
          >
            Log out
          </button>
        </div>
      </header>

      {/* Navigation */}
      <nav className="bg-white border-b border-slate-200 px-6">
        <div className="flex gap-1">
          <Link
            href="/app/customers"
            className={`px-4 py-3 text-sm font-medium transition-colors ${
              pathname.startsWith("/app/customers")
                ? "text-sky-600 border-b-2 border-sky-600"
                : "text-slate-600 hover:text-slate-800 border-b-2 border-transparent"
            }`}
          >
            Customers
          </Link>
          <Link
            href="/app/leads"
            className={`px-4 py-3 text-sm font-medium transition-colors ${
              pathname.startsWith("/app/leads")
                ? "text-sky-600 border-b-2 border-sky-600"
                : "text-slate-600 hover:text-slate-800 border-b-2 border-transparent"
            }`}
          >
            Leads
          </Link>
        </div>
      </nav>

      {/* Main Content */}
      <main className="flex-1 p-6">{children}</main>
    </div>
  );
}
