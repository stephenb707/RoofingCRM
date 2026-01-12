"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "../../lib/AuthContext";
import { useEffect } from "react";

export default function SelectTenantPage() {
  const { auth, selectTenant, logout } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!auth.token) {
      router.replace("/login");
      return;
    }
    if (auth.tenants.length === 1 && auth.selectedTenantId) {
      router.replace("/app/customers");
    }
  }, [auth, router]);

  if (!auth.token) {
    return null;
  }

  if (auth.tenants.length === 0) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gradient-to-br from-slate-100 to-sky-50">
        <div className="bg-white rounded-2xl shadow-xl p-8 max-w-md text-center border border-slate-200">
          <div className="w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg
              className="w-8 h-8 text-amber-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
          </div>
          <h2 className="text-lg font-semibold text-slate-800 mb-2">
            No Organizations Found
          </h2>
          <p className="text-sm text-slate-500 mb-6">
            Your account is not associated with any organizations yet.
          </p>
          <button
            onClick={() => {
              logout();
              router.push("/login");
            }}
            className="px-6 py-2.5 text-sm font-medium rounded-lg bg-slate-100 hover:bg-slate-200 transition-colors"
          >
            Log out
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4 bg-gradient-to-br from-slate-100 to-sky-50">
      <div className="w-full max-w-md bg-white shadow-xl rounded-2xl p-8 border border-slate-200">
        <div className="text-center mb-6">
          <h1 className="text-xl font-bold text-slate-800">
            Select Organization
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Choose which organization to work with
          </p>
        </div>

        <div className="space-y-3">
          {auth.tenants.map((t) => (
            <button
              key={t.tenantId}
              onClick={() => {
                selectTenant(t.tenantId);
                router.push("/app/customers");
              }}
              className="w-full border border-slate-200 rounded-xl px-4 py-4 text-left hover:bg-sky-50 hover:border-sky-200 transition-colors group"
            >
              <div className="flex items-center justify-between">
                <div>
                  <div className="font-medium text-slate-800 group-hover:text-sky-700">
                    {t.tenantName}
                  </div>
                  <div className="text-xs text-slate-500 mt-0.5">
                    Role: {t.role ?? "USER"}
                  </div>
                </div>
                <svg
                  className="w-5 h-5 text-slate-400 group-hover:text-sky-600 transition-colors"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 5l7 7-7 7"
                  />
                </svg>
              </div>
            </button>
          ))}
        </div>

        <div className="mt-6 pt-4 border-t border-slate-100">
          <button
            onClick={() => {
              logout();
              router.push("/login");
            }}
            className="w-full text-sm text-slate-500 hover:text-slate-700 transition-colors"
          >
            Log out
          </button>
        </div>
      </div>
    </div>
  );
}
