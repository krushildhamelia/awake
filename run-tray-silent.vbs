' Run Screen Awake directly into the System Tray with NO command prompt window
Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
strPath = fso.GetParentFolderName(WScript.ScriptFullName)
jarPath = strPath & "\target\screen-awake.jar"

If Not fso.FileExists(jarPath) Then
    MsgBox "target\screen-awake.jar not found! Please run 'mvn clean package' first.", vbExclamation, "Screen Awake"
    WScript.Quit 1
End If

WshShell.Run "javaw -jar """ & jarPath & """", 0, False
