"use client";

import { useState, useRef, useLayoutEffect, useCallback } from "react";

const PADDING = 12;
const GAP = 8;
const MIN_HEIGHT = 240;
const MAX_HEIGHT = 520;

export interface PopoverPlacementOptions {
  /** Max width in px (e.g. 320 for DatePicker, 640 for DateRangePicker) */
  maxWidthPx: number;
  /** Fallback height when popover not yet measured */
  fallbackHeightPx: number;
}

export interface PopoverPlacementStyle {
  position: "fixed";
  top: number;
  left: number;
  maxHeight: number;
  zIndex: number;
}

function computePlacement(
  buttonRect: DOMRect,
  popoverWidth: number,
  popoverHeight: number,
  fallbackHeight: number
): PopoverPlacementStyle {
  const vw = typeof window !== "undefined" ? window.innerWidth : 1024;
  const vh = typeof window !== "undefined" ? window.innerHeight : 768;

  const spaceBelow = vh - buttonRect.bottom - GAP;
  const spaceAbove = buttonRect.top - GAP;

  const height = popoverHeight > 0 ? popoverHeight : fallbackHeight;

  const useBottom =
    spaceBelow >= MIN_HEIGHT || (spaceBelow >= spaceAbove && spaceBelow > 0);

  let topPx: number;
  let maxHeightPx: number;

  if (useBottom) {
    topPx = buttonRect.bottom + GAP;
    maxHeightPx = Math.min(
      MAX_HEIGHT,
      Math.max(MIN_HEIGHT, spaceBelow - GAP)
    );
  } else {
    topPx = buttonRect.top - height - GAP;
    maxHeightPx = Math.min(
      MAX_HEIGHT,
      Math.max(MIN_HEIGHT, spaceAbove - GAP)
    );
  }

  topPx = Math.max(PADDING, topPx);

  let leftPx = buttonRect.left;
  if (leftPx + popoverWidth > vw - PADDING) {
    leftPx = vw - popoverWidth - PADDING;
  }
  if (leftPx < PADDING) {
    leftPx = PADDING;
  }

  return {
    position: "fixed",
    top: topPx,
    left: leftPx,
    maxHeight: maxHeightPx,
    zIndex: 50,
  };
}

export function usePopoverPlacement(
  open: boolean,
  options: PopoverPlacementOptions
) {
  const buttonRef = useRef<HTMLButtonElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);
  const [style, setStyle] = useState<PopoverPlacementStyle | null>(null);

  const recompute = useCallback(() => {
    if (!open || !buttonRef.current) return;

    const rect = buttonRef.current.getBoundingClientRect();
    const popoverEl = popoverRef.current;
    const popoverWidth = popoverEl?.offsetWidth ?? Math.min(0.95 * (typeof window !== "undefined" ? window.innerWidth : 1024), options.maxWidthPx);
    const popoverHeight = popoverEl?.offsetHeight ?? options.fallbackHeightPx;

    setStyle(
      computePlacement(
        rect,
        popoverWidth,
        popoverHeight,
        options.fallbackHeightPx
      )
    );
  }, [open, options.maxWidthPx, options.fallbackHeightPx]);

  useLayoutEffect(() => {
    if (!open) {
      setStyle(null);
      return;
    }

    recompute();
    const raf = requestAnimationFrame(() => {
      recompute();
    });

    const onScrollOrResize = () => recompute();
    window.addEventListener("resize", onScrollOrResize);
    window.addEventListener("scroll", onScrollOrResize, true);

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", onScrollOrResize);
      window.removeEventListener("scroll", onScrollOrResize, true);
    };
  }, [open, recompute]);

  return { buttonRef, popoverRef, style };
}
