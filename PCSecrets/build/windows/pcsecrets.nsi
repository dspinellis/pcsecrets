; installer.nsi
;
; This script is based on example2.nsi, but it remembers the directory, 
; has uninstall support and (optionally) installs start menu shortcuts.
;
; It will install pcsecrets into a directory that the user selects,

;--------------------------------

; The name of the installer
Name "PCSecrets"

; The file to write
OutFile "/quadra-shared/Chris/build/pcsecrets/windows/pcsecrets-install.exe"

; The default installation directory
InstallDir $PROGRAMFILES\PCSecrets

; Registry key to check for directory (so if you install again, it will 
; overwrite the old one automatically)
InstallDirRegKey HKLM "Software\PCSecrets" "Install_Dir"

; Request application privileges for Windows Vista
RequestExecutionLevel admin

;--------------------------------

; Pages

Page components
Page directory
Page instfiles

UninstPage uninstConfirm
UninstPage instfiles

;--------------------------------

; The stuff to install
Section "PCSecrets (required)"

  SectionIn RO
  
  ; Set output path to the installation directory.
  SetOutPath $INSTDIR
  
  ; Files to install
  File "/quadra-shared/Chris/build/pcsecrets/windows/pcsecrets.exe"
  File "/home/chris/git/PCSecrets/PCSecrets/build/web/doc.html"
  File "/home/chris/git/PCSecrets/PCSecrets/history.txt"
  File "/home/chris/git/PCSecrets/PCSecrets/legal.txt"
  File "/home/chris/git/PCSecrets/PCSecrets/notes.txt"
  File "/quadra-shared/Chris/build/pcsecrets/build.properties"
  
  ; Write the installation path into the registry
  WriteRegStr HKLM SOFTWARE\PCSecrets "Install_Dir" "$INSTDIR"
  
  ; Write the uninstall keys for Windows
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PCSecrets" "DisplayName" "PC Secrets"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PCSecrets" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PCSecrets" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PCSecrets" "NoRepair" 1
  WriteUninstaller "uninstall.exe"
  
SectionEnd

; Optional section (can be disabled by the user)
Section "Start Menu Shortcuts"

  CreateDirectory "$SMPROGRAMS\PCSecrets"
  CreateShortCut "$DESKTOP\PCSecrets.lnk" "$INSTDIR\pcsecrets.exe" ""
  CreateShortCut "$SMPROGRAMS\PCSecrets\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\PCsecrets\PCsecrets.lnk" "$INSTDIR\pcsecrets.exe" "" "$INSTDIR\pcsecrets.exe" 0
  
SectionEnd

;--------------------------------

; Uninstaller

Section "Uninstall"
  
  ; Remove registry keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PCSecrets"
  DeleteRegKey HKLM SOFTWARE\PCSecrets

  ; Remove files and uninstaller
  Delete $INSTDIR\pcsecrets.exe
  Delete $INSTDIR\doc.txt
  Delete $INSTDIR\history.txt
  Delete $INSTDIR\legal.txt
  Delete $INSTDIR\notes.txt
  Delete $INSTDIR\uninstall.exe

  ; Remove shortcuts, if any
  Delete "$SMPROGRAMS\PCSecrets\*.*"
  Delete "$DESKTOP\PCSecrets.lnk"

  ; Remove directories used
  RMDir "$SMPROGRAMS\PCSecrets"
  
  RMDir "$INSTDIR"
  MessageBox MB_YESNO "Delete PCSecrets data (including the encrypted secrets)? If you intend to re-install, choose No" IDNO bypass
  Delete "$APPDATA\PCSecrets\*.*"
  RMDir "$APPDATA\PCSecrets"
  
  bypass:
  

SectionEnd
