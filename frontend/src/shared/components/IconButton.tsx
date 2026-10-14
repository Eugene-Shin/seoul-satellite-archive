"use client";

type IconButtonProps = {
    label: string;
    onClick: () => void;
    disabled?: boolean;
    className?: string;
    children: React.ReactNode;
};

export function IconButton({
    label,
    onClick,
    disabled,
    className,
    children,
}: IconButtonProps) {
    return (
        <button
            type="button"
            aria-label={label}
            disabled={disabled}
            onClick={onClick}
            className={`rounded border px-2 py-1 hover:bg-zinc-100 disabled:opacity-40 disabled:hover:bg-transparent dark:hover:bg-zinc-800 ${className ?? ""}`}
        >
            {children}
        </button>
    );
}
