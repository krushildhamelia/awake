# Screen Awake & Mouse Jiggler

Keep your screen awake, prevent sleep/screensavers, and maintain your "Active" status on collaboration software (Microsoft Teams, Slack, Zoom, Skype, Discord) without being intrusive.

This repository provides **two standalone solutions**:
1. **Java Standalone Executable JAR (Maven)**: Runs with a System Tray icon or interactive terminal CLI, compatible with JDK 17+.
2. **Pure PowerShell Alternative ([awake.ps1](file:///D:/awake/awake.ps1))**: Zero dependencies, no Java or compilation required, works out of the box on Windows.

---

## Table of Contents
- [Why It's Better Than Basic Jigglers](#why-its-better-than-basic-jigglers)
- [Microsoft Teams & Collaboration Apps (How It Works)](#microsoft-teams--collaboration-apps-how-it-works)
  - [Will it keep Teams Green (Available)?](#will-it-keep-teams-green-available)
  - [How Teams Actually Checks Activity](#how-teams-actually-checks-activity)
  - [Can Teams or IT Detect It?](#can-teams-or-it-detect-it)
  - [Recommended Settings for Teams](#recommended-settings-for-teams)
- [Method 1: Java Standalone Executable JAR](#method-1-java-standalone-executable-jar)
  - [Prerequisites (JDK 17+)](#prerequisites-jdk-17)
  - [Building the JAR](#building-the-jar)
  - [Running Options](#running-options)
  - [CLI Arguments](#cli-arguments)
- [Method 2: Pure PowerShell Alternative](#method-2-pure-powershell-alternative)
  - [Running the Script (`awake.ps1`)](#running-the-script-awakeps1)
  - [PowerShell Parameters](#powershell-parameters)
  - [Examples](#powershell-examples)
  - [Instant 1-Liners (Copy & Paste)](#instant-1-liners-copy--paste)
- [Operating Modes Explained](#operating-modes-explained)

---

## Why It's Better Than Basic Jigglers

Traditional mouse jigglers erratically jerk your cursor across the screen, interrupting you while you are reading, typing, or watching videos.

This tool implements a **Smart Dual-Layer Awake Engine**:

1. **Smart User Detection (Zero Distraction)**:
   - Constantly monitors mouse coordinates.
   - If **you** are actively moving the mouse, it resets its timer and does **nothing**.
   - Only nudges when your computer has actually been idle for the configured threshold (default: 10 seconds).

2. **Stealth 1-Pixel Micro-Nudge (Default)**:
   - Moves the cursor by just **1 pixel** and immediately returns it.
   - Virtually invisible to the human eye, won't misclick or disrupt your work, yet Windows registers it as a full user input event (`GetLastInputInfo`), keeping your status green and active.

3. **Native Windows Power Management (`SetThreadExecutionState`)**:
   - Directly instructs the Windows Kernel power manager:
     `ES_CONTINUOUS | ES_SYSTEM_REQUIRED | ES_DISPLAY_REQUIRED | ES_AWAYMODE_REQUIRED`
   - Keeps your display and system awake regardless of aggressive corporate sleep/lock policies.

4. **Runs in System Tray or Console**:
   - Sits quietly in the Windows Taskbar Notification Area (System Tray) with a dynamic coffee cup icon (Green = Active, Amber = Paused).
   - Right-click tray menu to toggle pause, change modes, or adjust intervals.

---

## Microsoft Teams & Collaboration Apps (How It Works)

### Will it keep Teams Green (Available)?
**Yes, 100%.** Teams will recognize your computer as active and will not switch to "Away" (Yellow) or "Inactive".

### How Teams Actually Checks Activity
1. **The `GetLastInputInfo` Windows API**:
   - Microsoft Teams (both classic and the new Teams 2.1 / WebView2) does not spy on what you type or which app is focused.
   - Instead, it periodically calls the Windows API `GetLastInputInfo`, asking: *"How many milliseconds have elapsed since the last hardware input event?"*
   - If that duration exceeds the inactivity limit (usually **5 minutes**), Teams switches your status to **Away**.
2. **Display Standby & Session Lock**:
   - If Windows turns off the display or the screen locks, Teams intercepts the OS power/session notification (`WTSRegisterSessionNotification`) and immediately sets your status to **Away**.

### Why This Tool Works for Teams
- **Resets System Input Timer**: The mouse nudge and virtual key pulse emit real hardware input events to Windows, explicitly resetting `GetLastInputInfo`.
- **Prevents Screen Lock**: The native Windows power lock (`SetThreadExecutionState`) stops Windows from sleeping the monitor or engaging the lock screen.
- **No Chat Interruption**: The virtual `F15` key tap is an unassigned function key that does not output letters or characters into open chat boxes.

### Can Teams or IT Detect It?
- **Microsoft Teams**: **No.** Teams is not anti-cheat software; it has no kernel-level trajectory analysis. It simply checks Windows: *"Has there been input in the last 5 minutes? Yes -> Stay Green."*
- **Corporate IT & Activity Trackers (EDR / Hubstaff / Teramind)**:
  Crude jigglers get flagged because of obvious robotic patterns. This tool was designed specifically to avoid those flags:
  - **No Constant Spam**: Smart idle detection pauses while you are actively working, nudging only after genuine inactivity.
  - **No Unnatural Leaps**: Stealth mode moves the mouse by just 1 pixel and restores it in 30ms.
  - **No Blinking Keyboard LEDs**: It never toggles `ScrollLock` or `CapsLock` (which flash physical LEDs on your keyboard).
  - **Legitimate Process Names**: The tool runs under standard `powershell.exe` or `javaw.exe`, avoiding blacklisted executable names like `MouseJiggler.exe` or `Caffeine.exe`.

### Recommended Settings for Teams
For the most natural, reliable behavior with Teams:

```powershell
# PowerShell script (45-second interval, 15-second idle wait):
powershell -ExecutionPolicy Bypass -File .\awake.ps1 -Interval 45 -IdleThreshold 15
```
Or with the Java standalone JAR:
```powershell
java -jar target/screen-awake.jar --interval 45 --idle 15
```

---

## Method 1: Java Standalone Executable JAR

### Prerequisites (JDK 17+)
- **Java**: JDK 17 or higher (tested with JDK 17, 21, and 27).
- **Maven**: 3.6+

### Building the JAR
Open a terminal in this directory and run:

```powershell
mvn clean package
```

The self-contained standalone fat JAR will be generated at:
```
target/screen-awake.jar
```

### Running Options

#### Option A: Double-Click / System Tray
- Double-click [run.bat](file:///D:/awake/run.bat) or [run-tray-silent.vbs](file:///D:/awake/run-tray-silent.vbs) (opens directly into the System Tray with no visible command prompt).
- A coffee cup icon will appear in your Windows notification tray.
- **Double-click icon**: Pause / Resume.
- **Right-click icon**: Select modes, intervals, trigger immediate nudge, or exit.

#### Option B: From Command Line
```powershell
java -jar target/screen-awake.jar
```

#### Option C: Interactive Console Mode (CLI)
```powershell
java -jar target/screen-awake.jar --console
```
Interactive single-key shortcuts in console mode:
- `p` : Toggle Pause / Resume
- `m` : Cycle through modes (Stealth -> Roam -> Key -> Dual)
- `j` : Nudge / jiggle right now
- `s` : Show current status
- `q` : Quit

### CLI Arguments

| Option | Description | Default |
|---|---|---|
| `-i, --interval <sec>` | Seconds between awake actions | `30` |
| `--idle <sec>` | Inactivity period before triggering | `10` |
| `-m, --mode <mode>` | Operating mode (`stealth`, `roam`, `key`, `dual`) | `stealth` |
| `--console, --cli` | Force interactive console mode (no System Tray) | Disabled |
| `--no-native` | Disable Windows native power lock (`SetThreadExecutionState`) | Disabled |
| `-h, --help` | Display help screen | |

---

## Method 2: Pure PowerShell Alternative

If you do not have Java installed or are on a restricted machine where you cannot install build tools, use the native PowerShell script [awake.ps1](file:///D:/awake/awake.ps1).

### Running the Script (`awake.ps1`)

```powershell
powershell -ExecutionPolicy Bypass -File .\awake.ps1
```

### PowerShell Parameters

| Parameter | Type | Description | Default |
|---|---|---|---|
| `-Interval` | Integer | Seconds between awake actions | `30` |
| `-IdleThreshold` | Integer | Seconds of inactivity before nudging | `10` |
| `-Mode` | String | Awake mode: `Dual`, `Mouse`, `PowerLock`, `Key` | `Dual` |
| `-Visible` | Switch | Moves mouse visibly in a small arc instead of 1px nudge | False |

### PowerShell Examples

```powershell
# 1. Default run (Dual mode: Stealth 1px nudge + F15 key + Windows PowerLock)
.\awake.ps1

# 2. Check every 60 seconds after 15 seconds of idle
.\awake.ps1 -Interval 60 -IdleThreshold 15

# 3. Visible gentle mouse roaming (so you can see it moving)
.\awake.ps1 -Visible

# 4. Pure Windows Power Lock only (no mouse movements at all)
.\awake.ps1 -Mode PowerLock

# 5. Pure Mouse Jiggle only
.\awake.ps1 -Mode Mouse
```

To stop at any time, press `Ctrl + C`. The script automatically restores standard Windows power settings upon exit.

---

### Instant 1-Liners (Copy & Paste)

If execution policies prevent running `.ps1` files, copy and paste either of these one-liners directly into any open PowerShell prompt:

#### 1. Subtle Mouse Nudge (1-pixel nudge back and forth every 30 seconds):
```powershell
Add-Type -AssemblyName System.Windows.Forms; while($true){$p=[System.Windows.Forms.Cursor]::Position; [System.Windows.Forms.Cursor]::Position=New-Object System.Drawing.Point(($p.X+1),$p.Y); Start-Sleep -m 30; [System.Windows.Forms.Cursor]::Position=$p; Start-Sleep 30}
```

#### 2. Safe Key Tap (`F15` pulse every 60 seconds — zero mouse movement):
```powershell
$w=New-Object -ComObject Wscript.Shell; while($true){$w.SendKeys("{F15}"); Start-Sleep -Seconds 60}
```

#### 3. Pure Windows Power Lock (keeps screen on using Kernel32 without mouse moves):
```powershell
Add-Type -TypeDefinition 'using System.Runtime.InteropServices; public class P {[DllImport("kernel32.dll")] public static extern int SetThreadExecutionState(int f); }'; [P]::SetThreadExecutionState([int]0x80000003); Write-Host "Screen awake active. Press Ctrl+C to stop."; while($true){Start-Sleep 60}
```

---

## Operating Modes Explained

| Mode | What It Does | Best For |
|---|---|---|
| **Stealth (Default)** | Nudges mouse +1px and back in 30ms | Complete stealth; zero interference while watching videos or typing. |
| **Smooth Roam** | Smoothly glides cursor in a gentle arc (~25px) | Visible confirmation that the cursor is being kept alive. |
| **Key Pulse** | Emits harmless `F15` virtual key | Zero cursor movement; doesn't interfere with focus or active fields. |
| **Dual** | Stealth mouse nudge + `F15` key + Kernel power lock | Maximum reliability against strict corporate inactivity policies. |
| **PowerLock** | Uses Win32 `SetThreadExecutionState` | Prevents monitor standby and screen lock without generating input. |
