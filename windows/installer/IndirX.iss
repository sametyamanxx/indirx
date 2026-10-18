#ifndef AppVersion
  #define AppVersion "0.2.11"
#endif
#ifndef PublishDir
  #define PublishDir "..\..\artifacts\windows\publish"
#endif
#ifndef OutputDir
  #define OutputDir "..\..\artifacts\windows\dist"
#endif

[Setup]
AppId={{57350182-9069-4C1B-B3CA-307CF7008F38}
AppName=İndirX
AppVersion={#AppVersion}
AppPublisher=İndirX
AppPublisherURL=https://github.com/sametyamanxx/indirx
AppSupportURL=https://github.com/sametyamanxx/indirx/issues
AppUpdatesURL=https://github.com/sametyamanxx/indirx/releases
DefaultDirName={localappdata}\Programs\IndirX
DefaultGroupName=İndirX
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0.19045
OutputDir={#OutputDir}
OutputBaseFilename=IndirX-{#AppVersion}-Windows-x64-Setup
Compression=lzma2/fast
SolidCompression=yes
WizardStyle=modern
SetupIconFile={#PublishDir}\Assets\IndirX.ico
WizardImageFile={#PublishDir}\Assets\WizardLarge.bmp
WizardSmallImageFile={#PublishDir}\Assets\WizardSmall.bmp
UninstallDisplayIcon={app}\IndirX.exe
LicenseFile={#PublishDir}\LICENSE.txt
AppMutex=Local\IndirX.Windows
CloseApplications=yes
CloseApplicationsFilter=IndirX.exe,yt-dlp.exe,ffmpeg.exe
RestartApplications=no
SetupLogging=yes

[Languages]
Name: "turkish"; MessagesFile: "compiler:Languages\Turkish.isl"

[Tasks]
Name: "desktopicon"; Description: "Masaüstü kısayolu oluştur"; Flags: unchecked

[Files]
Source: "{#PublishDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\İndirX"; Filename: "{app}\IndirX.exe"; IconFilename: "{app}\IndirX.exe"
Name: "{autodesktop}\İndirX"; Filename: "{app}\IndirX.exe"; IconFilename: "{app}\IndirX.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\IndirX.exe"; Description: "İndirX'i başlat"; Flags: nowait postinstall skipifsilent
