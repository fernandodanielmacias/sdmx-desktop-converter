#ifndef MyAppVersion
  #define MyAppVersion "1.0.0"
#endif

#define MyAppName "Convertidor SDMX"
#define MyAppPublisher "Ordo Novus"
#define MyAppExecutable "Convertidor SDMX.exe"

[Setup]
AppId={{79B7628D-C05D-4CCA-A1B8-DAB0F6941EF2}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppVerName={#MyAppName} {#MyAppVersion}

DefaultDirName={localappdata}\Programs\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes

OutputDir=..\..\..\target\installer
OutputBaseFilename=Convertidor-SDMX-{#MyAppVersion}

SetupIconFile=..\resources\images\application-icon.ico
WizardImageFile=..\resources\images\installer-wizard.png
WizardSmallImageFile=..\resources\images\application-icon.png
UninstallDisplayIcon={app}\{#MyAppExecutable}

PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

Compression=lzma2
SolidCompression=yes

WizardStyle=modern
DisableWelcomePage=no
ShowLanguageDialog=no
LanguageDetectionMethod=uilanguage

CloseApplications=yes
RestartApplications=no
UsePreviousAppDir=yes

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; \
    Description: "Crear un acceso directo en el escritorio"; \
    GroupDescription: "Accesos directos adicionales:"

[Files]
Source: "..\..\..\target\distribution\Convertidor SDMX\*"; \
    DestDir: "{app}"; \
    Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; \
    Filename: "{app}\{#MyAppExecutable}"; \
    WorkingDir: "{app}"

Name: "{userdesktop}\{#MyAppName}"; \
    Filename: "{app}\{#MyAppExecutable}"; \
    WorkingDir: "{app}"; \
    Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExecutable}"; \
    Description: "Ejecutar {#MyAppName}"; \
    Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; \
    Name: "{app}\converter"