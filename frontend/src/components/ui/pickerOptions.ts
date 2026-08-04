import * as LucideIcons from "lucide-react";
import type { LucideIcon } from "lucide-react";

export const CATEGORY_ICONS = [
  "shopping-cart",
  "utensils",
  "coffee",
  "gift",
  "film",
  "music",
  "gamepad-2",
  "shirt",
  "bus",
  "car",
  "bike",
  "plane",
  "home",
  "zap",
  "wifi",
  "smartphone",
  "briefcase",
  "coins",
  "banknote",
  "piggy-bank",
  "trending-up",
  "wallet",
  "credit-card",
  "school",
  "dumbbell",
  "heart-pulse",
  "pill",
  "book-open",
  "laptop",
  "landmark",
  "scissors",
  "fuel",
] as const;

export const ICON_MAP: Record<string, LucideIcon> = Object.fromEntries(
  CATEGORY_ICONS.map((name) => [
    name,
    (LucideIcons as unknown as Record<string, LucideIcon>)[name],
  ]),
);

export const CATEGORY_COLORS = [
  "#EF4444",
  "#F97316",
  "#F59E0B",
  "#84CC16",
  "#22C55E",
  "#10B981",
  "#06B6D4",
  "#0EA5E9",
  "#3B82F6",
  "#6366F1",
  "#8B5CF6",
  "#D946EF",
  "#EC4899",
  "#F43F5E",
  "#64748B",
  "#0F172A",
];
