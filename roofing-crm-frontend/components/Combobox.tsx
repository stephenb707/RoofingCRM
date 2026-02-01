"use client";

import { useState, useRef, useEffect, useCallback } from "react";

export interface ComboboxProps<T> {
  items: T[];
  getItemLabel: (item: T) => string;
  getItemKey: (item: T) => string;
  onSelect: (item: T) => void;
  onSearchChange: (query: string) => void;
  value: string;
  placeholder?: string;
  renderItem?: (item: T) => React.ReactNode;
  id?: string;
  className?: string;
  disabled?: boolean;
}

export function Combobox<T>({
  items,
  getItemLabel,
  getItemKey,
  onSelect,
  onSearchChange,
  value,
  placeholder = "Search…",
  renderItem,
  id,
  className = "",
  disabled = false,
}: ComboboxProps<T>) {
  const [isOpen, setIsOpen] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  const close = useCallback(() => {
    setIsOpen(false);
    setHighlightedIndex(-1);
  }, []);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        close();
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [close]);

  useEffect(() => {
    if (isOpen && highlightedIndex >= 0 && listRef.current) {
      const el = listRef.current.children[highlightedIndex] as HTMLElement;
      if (el?.scrollIntoView) {
        el.scrollIntoView({ block: "nearest" });
      }
    }
  }, [isOpen, highlightedIndex]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isOpen) {
      if (e.key === "ArrowDown" || e.key === "Enter") {
        setIsOpen(true);
        setHighlightedIndex(items.length > 0 ? 0 : -1);
        e.preventDefault();
      }
      return;
    }
    if (e.key === "Escape") {
      close();
      e.preventDefault();
      return;
    }
    if (e.key === "ArrowDown") {
      setHighlightedIndex((i) => (i < items.length - 1 ? i + 1 : 0));
      e.preventDefault();
      return;
    }
    if (e.key === "ArrowUp") {
      setHighlightedIndex((i) => (i > 0 ? i - 1 : items.length - 1));
      e.preventDefault();
      return;
    }
    if (e.key === "Enter" && highlightedIndex >= 0 && items[highlightedIndex]) {
      onSelect(items[highlightedIndex]);
      close();
      e.preventDefault();
    }
  };

  const handleSelect = (item: T) => {
    onSelect(item);
    close();
  };

  const showDropdown = isOpen && (value.length > 0 || items.length > 0);

  return (
    <div ref={containerRef} className={`relative ${className}`}>
      <input
        id={id}
        type="text"
        value={value}
        onChange={(e) => {
          onSearchChange(e.target.value);
          setIsOpen(true);
          setHighlightedIndex(0);
        }}
        onFocus={() => {
          setIsOpen(true);
          setHighlightedIndex(items.length > 0 ? 0 : -1);
        }}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        disabled={disabled}
        autoComplete="off"
        className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
      />

      {showDropdown && (
        <ul
          ref={listRef}
          role="listbox"
          className="absolute z-50 mt-1 w-full max-h-60 overflow-auto bg-white border border-slate-200 rounded-lg shadow-lg py-1"
        >
          {items.length === 0 ? (
            <li className="px-4 py-3 text-sm text-slate-500">No results</li>
          ) : (
            items.map((item, idx) => (
              <li
                key={getItemKey(item)}
                role="option"
                aria-selected={idx === highlightedIndex}
                onClick={() => handleSelect(item)}
                onMouseEnter={() => setHighlightedIndex(idx)}
                className={`px-4 py-2.5 text-sm cursor-pointer ${
                  idx === highlightedIndex ? "bg-sky-50 text-sky-800" : "text-slate-800 hover:bg-slate-50"
                }`}
              >
                {renderItem ? renderItem(item) : getItemLabel(item)}
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  );
}
