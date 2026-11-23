<#
.SYNOPSIS
    Screen Awake & Mouse Jiggler in Pure PowerShell
.DESCRIPTION
    Keeps your screen awake, prevents sleep/screensaver, and maintains 'Active' status
    in collaboration apps (Teams, Slack, Zoom, Skype) without installing any tools.
.PARAMETER Interval
    Seconds between awake actions (Default: 30)
.PARAMETER IdleThreshold
    Seconds of inactivity before nudging (Default: 10)
.PARAMETER Mode
    Awake mode: 'Dual', 'Mouse', 'PowerLock', 'Key' (Default: 'Dual')
.PARAMETER Visible
    If set, moves the mouse visibly in a small circle instead of 1px stealth nudge.
.EXAMPLE
    .\awake.ps1
.EXAMPLE
    .\awake.ps1 -Interval 60 -Mode Mouse
.EXAMPLE
    .\awake.ps1 -Visible
#>
[CmdletBinding()]
param(
    [int]$Interval = 30,
    [int]$IdleThreshold = 10,
    [ValidateSet('Dual', 'Mouse', 'PowerLock', 'Key')]
    [string]$Mode = 'Dual',
    [switch]$Visible
)

# Set window title
$host.UI.RawUI.WindowTitle = "Screen Awake (Active) - Press Ctrl+C to Stop"

# Define Win32 Native APIs
if (-not ([System.Management.Automation.PSTypeName]'Win32Awake').Type) {
    Add-Type -TypeDefinition @"
    using System;
    using System.Runtime.InteropServices;

    public class Win32Awake {
        [StructLayout(LayoutKind.Sequential)]
        public struct LASTINPUTINFO {
            public uint cbSize;
            public uint dwTime;
        }

        [DllImport("user32.dll")]
        public static extern bool GetLastInputInfo(ref LASTINPUTINFO plii);

        [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        public static extern int SetThreadExecutionState(int esFlags);

        [DllImport("user32.dll")]
        public static extern void mouse_event(int dwFlags, int dx, int dy, int dwData, int dwExtraInfo);

        public static uint GetIdleTimeSeconds() {
            LASTINPUTINFO lii = new LASTINPUTINFO();
            lii.cbSize = (uint)Marshal.SizeOf(lii);
            if (GetLastInputInfo(ref lii)) {
                return ((uint)Environment.TickCount - lii.dwTime) / 1000;
            }
            return 0;
        }
    }
"@
}

# Win32 Execution State Flags
$ES_CONTINUOUS        = [int]0x80000000
$ES_SYSTEM_REQUIRED   = [int]0x00000001
$ES_DISPLAY_REQUIRED  = [int]0x00000002
$ES_AWAYMODE_REQUIRED = [int]0x00000040
$MOUSEEVENTF_MOVE     = 0x0001

# COM Object for safe virtual key press
$wscript = New-Object -ComObject Wscript.Shell

Clear-Host
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "         SCREEN AWAKE & MOUSE JIGGLER (PowerShell)           " -ForegroundColor Yellow
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host " Mode:           $Mode $(if ($Visible) {'(Visible Glide)'} else {'(Stealth 1px)'})" -ForegroundColor White
Write-Host " Interval:       $Interval seconds" -ForegroundColor White
Write-Host " Idle Threshold: $IdleThreshold seconds" -ForegroundColor White
Write-Host " Exit:           Press Ctrl + C at any time to stop" -ForegroundColor White
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host ""

# Activate Windows Power Lock (display stays on, no sleep)
if ($Mode -in @('Dual', 'PowerLock')) {
    $powerFlags = $ES_CONTINUOUS -bor $ES_SYSTEM_REQUIRED -bor $ES_DISPLAY_REQUIRED -bor $ES_AWAYMODE_REQUIRED
    [Win32Awake]::SetThreadExecutionState($powerFlags) | Out-Null
    Write-Host "[Power] Native Windows Awake Lock activated." -ForegroundColor Green
}

$nudgeCount = 0
$lastNudge = [DateTime]::UtcNow.AddSeconds(-$Interval)

try {
    while ($true) {
        $idleSec = [Win32Awake]::GetIdleTimeSeconds()
        $now = [DateTime]::UtcNow
        $secSinceLastNudge = ($now - $lastNudge).TotalSeconds

        # Check if system is idle and interval has passed
        if ($idleSec -ge $IdleThreshold -and $secSinceLastNudge -ge $Interval) {
            $timestamp = (Get-Date).ToString("HH:mm:ss")

            # 1. Mouse Action
            if ($Mode -in @('Dual', 'Mouse')) {
                if ($Visible) {
                    # Gentle visible roaming steps
                    for ($step = 0; $step -lt 10; $step++) {
                        [Win32Awake]::mouse_event($MOUSEEVENTF_MOVE, 2, 2, 0, 0)
                        Start-Sleep -Milliseconds 15
                    }
                    Start-Sleep -Milliseconds 50
                    for ($step = 0; $step -lt 10; $step++) {
                        [Win32Awake]::mouse_event($MOUSEEVENTF_MOVE, -2, -2, 0, 0)
                        Start-Sleep -Milliseconds 15
                    }
                } else {
                    # Stealth 1px relative nudge
                    [Win32Awake]::mouse_event($MOUSEEVENTF_MOVE, 1, 1, 0, 0)
                    Start-Sleep -Milliseconds 30
                    [Win32Awake]::mouse_event($MOUSEEVENTF_MOVE, -1, -1, 0, 0)
                }
            }

            # 2. Virtual Key Action (Safe F15 key)
            if ($Mode -in @('Dual', 'Key')) {
                $wscript.SendKeys("{F15}")
            }

            $nudgeCount++
            $lastNudge = [DateTime]::UtcNow

            Write-Host "[$timestamp] Nudged successfully (Count: $nudgeCount) | Idle was: ${idleSec}s" -ForegroundColor Green
        }

        # Status line update
        $remaining = [Math]::Max(0, [int]($Interval - ($now - $lastNudge).TotalSeconds))
        Write-Progress -Activity "Keeping Screen Awake" `
                       -Status "Idle: ${idleSec}s / ${IdleThreshold}s | Next nudge in: ${remaining}s | Total nudges: $nudgeCount" `
                       -PercentComplete ([Math]::Min(100, [int](($Interval - $remaining) / $Interval * 100)))

        Start-Sleep -Milliseconds 500
    }
}
finally {
    # Restore normal Windows Power State when exiting
    Write-Progress -Activity "Keeping Screen Awake" -Completed
    [Win32Awake]::SetThreadExecutionState($ES_CONTINUOUS) | Out-Null
    Write-Host "`n[Power] Awake Lock released. Windows power settings restored." -ForegroundColor Yellow
}
