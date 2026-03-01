"use client";

export interface NextStepAction {
  label: string;
  onClick: () => void | Promise<void>;
  variant?: "primary" | "secondary" | "ghost";
  disabled?: boolean;
  testId?: string;
}

interface NextStepPromptDialogProps {
  title: string;
  description: string;
  actions: NextStepAction[];
  onClose?: () => void;
  dismissLabel?: string;
  showDismissButton?: boolean;
}

export function NextStepPromptDialog({
  title,
  description,
  actions,
  onClose,
  dismissLabel = "Not now",
  showDismissButton = true,
}: NextStepPromptDialogProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
        <h3 className="text-lg font-semibold text-slate-800">{title}</h3>
        <p className="mt-2 text-sm text-slate-600">{description}</p>

        <div className="mt-5 space-y-2">
          {actions.map((action) => (
            <button
              key={action.label}
              type="button"
              onClick={action.onClick}
              disabled={action.disabled}
              data-testid={action.testId}
              className={`w-full rounded-lg px-4 py-2.5 text-sm font-medium transition-colors disabled:opacity-60 ${
                action.variant === "secondary"
                  ? "border border-slate-300 bg-white text-slate-700 hover:bg-slate-50"
                  : action.variant === "ghost"
                    ? "bg-transparent text-slate-600 hover:bg-slate-100"
                  : "bg-sky-600 text-white hover:bg-sky-700"
              }`}
            >
              {action.label}
            </button>
          ))}
        </div>

        {onClose && showDismissButton && (
          <button
            type="button"
            onClick={onClose}
            className="mt-4 w-full rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            {dismissLabel}
          </button>
        )}
      </div>
    </div>
  );
}
