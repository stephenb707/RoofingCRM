"use client";

import { useState, useRef, useLayoutEffect, useCallback } from "react";

const GAP = 8;
const MIN_HEIGHT = 240;
const MAX_HEIGHT = 520;

export interface PopoverPlacementOptions {
  /** Max width in px (e.g. 320 for DatePicker, 640 for DateRangePicker) */
  maxWidthPx: number;
  /** Fallback height when popover not yet measured */
  fallbackHeightPx: number;
  /** Padding from viewport edges when clamping placement */
  collisionPaddingPx?: number;
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
  fallbackHeight: number,
  collisionPaddingPx = 24
): PopoverPlacementStyle {
  const vw = typeof window !== "undefined" ? window.innerWidth : 1024;
  const vh = typeof window !== "undefined" ? window.innerHeight : 768;
  const pad = collisionPaddingPx;

  const heightRaw = popoverHeight > 0 ? popoverHeight : fallbackHeight;
  const effectiveHeight = Math.min(heightRaw, Math.max(0, vh - pad * 2));

  const availableBelow = vh - pad - (buttonRect.bottom + GAP);
  const availableAbove = (buttonRect.top - GAP) - pad;

  const canFitBelow = availableBelow >= effectiveHeight;
  const canFitAbove = availableAbove >= effectiveHeight;
  const useBottom = canFitBelow ? true : canFitAbove ? false : availableBelow >= availableAbove;

  let topPx: number;
  let maxHeightPx: number;

  if (useBottom) {
    topPx = buttonRect.bottom + GAP;
    maxHeightPx = Math.min(
      MAX_HEIGHT,
      Math.max(MIN_HEIGHT, Math.max(0, availableBelow))
    );
  } else {
    topPx = buttonRect.top - GAP - effectiveHeight;
    maxHeightPx = Math.min(
      MAX_HEIGHT,
      Math.max(MIN_HEIGHT, Math.max(0, availableAbove))
    );
  }

  const maxTop = Math.max(pad, vh - pad - effectiveHeight);
  topPx = Math.max(pad, Math.min(topPx, maxTop));

  let leftPx = buttonRect.left;
  const maxLeft = Math.max(pad, vw - pad - popoverWidth);
  leftPx = Math.max(pad, Math.min(leftPx, maxLeft));

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
        options.fallbackHeightPx,
        options.collisionPaddingPx ?? 24
      )
    );
  }, [open, options.maxWidthPx, options.fallbackHeightPx, options.collisionPaddingPx]);

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
    const popoverEl = popoverRef.current;
    const resizeObserver =
      popoverEl && typeof ResizeObserver !== "undefined"
        ? new ResizeObserver(() => recompute())
        : null;
    if (popoverEl && resizeObserver) {
      resizeObserver.observe(popoverEl);
    }

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", onScrollOrResize);
      window.removeEventListener("scroll", onScrollOrResize, true);
      resizeObserver?.disconnect();
    };
  }, [open, recompute]);

  return { buttonRef, popoverRef, style };
}
