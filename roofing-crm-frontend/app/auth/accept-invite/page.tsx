"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/lib/AuthContext";
import { acceptInvite } from "@/lib/teamApi";
import { getApiErrorMessage } from "@/lib/apiError";

export default function AcceptInvitePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const { api, auth, addTenant, selectTenant } = useAuth();

  const [status, setStatus] = useState<"loading" | "success" | "error">(
    "loading"
  );
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setStatus("error");
      setErrorMessage("Invalid invite link: missing token");
      return;
    }

    const tokenStr: string = token;

    if (!auth.token) {
      const nextUrl = `/auth/accept-invite?token=${encodeURIComponent(tokenStr)}`;
      router.replace(`/auth/login?next=${encodeURIComponent(nextUrl)}`);
      return;
    }

    let cancelled = false;

    async function doAccept() {
      try {
        const response = await acceptInvite(api, tokenStr);
        if (cancelled) return;

        addTenant({
          tenantId: response.tenantId,
          tenantName: response.tenantName,
          tenantSlug: response.tenantSlug ?? response.tenantName?.toLowerCase().replace(/\s+/g, "-") ?? "",
          role: response.role,
        });

        selectTenant(response.tenantId);
        setStatus("success");
        router.replace("/app/customers");
      } catch (err) {
        if (cancelled) return;
        setStatus("error");
        setErrorMessage(getApiErrorMessage(err, "Failed to accept invite"));
      }
    }

    doAccept();
    return () => {
      cancelled = true;
    };
  }, [token, auth.token, api, addTenant, selectTenant, router]);

  if (status === "error") {
    return (
      <div className="min-h-screen flex items-center justify-center px-4 bg-gradient-to-br from-slate-100 to-sky-50">
        <div className="w-full max-w-md bg-white shadow-xl rounded-2xl p-8 border border-slate-200 text-center">
          <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg
              className="w-8 h-8 text-red-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </div>
          <h2 className="text-lg font-semibold text-slate-800 mb-2">
            Unable to Accept Invite
          </h2>
          <p className="text-sm text-slate-500 mb-6">{errorMessage}</p>
          <button
            onClick={() => router.push("/app/customers")}
            className="px-6 py-2.5 bg-sky-600 text-white text-sm font-medium rounded-lg hover:bg-sky-700"
          >
            Go to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4 bg-gradient-to-br from-slate-100 to-sky-50">
      <div className="w-full max-w-md bg-white shadow-xl rounded-2xl p-8 border border-slate-200 text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-sky-600 mx-auto mb-4"></div>
        <p className="text-sm text-slate-500">Accepting invite...</p>
      </div>
    </div>
  );
}
