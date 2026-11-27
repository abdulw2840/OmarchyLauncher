# OmarchyLauncher

An opinionated Android home launcher combining **Lawnchair / Pixel Launcher** ergonomics with a sharp, zero-radius **Karui / Omarchy** terminal aesthetic.

## Highlights

- **Omarchy Workspace:** Clean `tokyo-night` solid background out of the box (or switch to translucent System Wallpaper with adjustable dim overlay).
- **25 Built-in Themes:** Switch between `tokyo-night`, `gruvbox`, `catppuccin`, `rose-pine`, `everforest`, `nord`, `crt-amber`, `amoled`, `paper`, and more, plus UI scaling (`0.85x`–`1.3x`).
- **At a Glance Bar:** Monospace clock, date, battery percentage, and active privilege badge (`[ROOT]` / `[SHIZUKU]` / `[USER]`).
- **Command & Search Prompt (`>_`):**
  - Instant app filtering by name or package ID
  - Inline math evaluation (`=2+2*5`)
  - Quick commands (`:settings`, `:lock`, `:refresh`)
  - Web search fallback
- **App Drawer:** Toggle between customizable Grid view and **Omarchy Terminal List** view, with `all` / `user` / `system` / `hidden` tabs.
- **App Context Menu:** Long-press any app for dynamic App Shortcuts, Pin to Home, Pin to Dock, Hide, App Info, and **Root actions** (Force Stop, Freeze, Root Uninstall).
- **Gestures & Root / Shizuku:** Swipe up for drawer, swipe down for notifications, double-tap empty space to lock screen (via Root `input keyevent 223` or Accessibility fallback).

## Building from Source

```bash
git clone https://github.com/abdulw2840/OmarchyLauncher.git
cd OmarchyLauncher
./gradlew assembleDebug
```
