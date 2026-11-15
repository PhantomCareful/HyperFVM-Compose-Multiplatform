; HyperFVM 桌面端安装包（Inno Setup 6）
;
; 为什么不用 jpackage 的 MSI：那套界面是 Windows Installer 自带的老式对话框，
; 停在 Win95 风格，没有「创建桌面快捷方式」选项，装完不能直接启动，
; 卸载也只能走控制面板，没有向导。Inno Setup 这四条一次解决。
;
; 编译入口：Gradle 任务 :desktop:packageInnoSetup，或直接用 Inno Setup 的 IDE 打开本文件。

#ifndef AppVersion
  #define AppVersion "1.0.0"
#endif

#define AppName "HyperFVM"
#define AppPublisher "HyperFVM"
#define AppExeName "HyperFVM.exe"
; createDistributable 的产物目录；本文件位于 desktop/installer 下。
#define DistDir "..\build\compose\binaries\main\app\HyperFVM"

[Setup]
; AppId 是卸载条目的唯一身份：一旦发布就不要改，否则升级会被当成另一个程序重新装一份。
AppId={{9C1F6E52-4A73-4C1E-9B0D-7B2E5A8D3F14}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
; 开始菜单文件夹自动建，不再单独占一页向导。
DisableProgramGroupPage=yes
; 装进 Program Files 需要提权；卸载项也只登记到本机。
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir=..\build\compose\binaries\main\inno
OutputBaseFilename={#AppName}-{#AppVersion}-setup
Compression=lzma2/max
SolidCompression=yes
; Inno 6 的现代向导样式：单页式配色 + 大图标页头，和 MSI 那套完全不是一回事。
WizardStyle=modern
; 高分屏按比例放大，4K 下不至于一行小字看不清。
WizardSizePercent=110
UninstallDisplayName={#AppName}
UninstallDisplayIcon={app}\{#AppExeName}
; 由 tool/make_desktop_icon.ps1 从 Android 自适应图标生成，和 exe 内嵌的是同一张。
SetupIconFile=HyperFVM.ico

[Languages]
; 简体中文的 .isl 没有随 Inno Setup 主程序分发（只有日韩等 30 种），所以随脚本存放一份，
; 相对路径由 ISCC 按本文件所在目录解析。
Name: "chinesesimplified"; MessagesFile: "ChineseSimplified.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
; 「是否创建桌面快捷方式」由用户勾选，默认勾上。
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "{#DistDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; 快捷方式指向这个独立的 ico，而不取 exe 内嵌图标：
; 资源管理器把图标按「文件路径」缓存，同一个路径被旧版本占过时会一直显示旧图标，
; 换一个图标来源路径就不会串。
Source: "HyperFVM.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExeName}"; IconFilename: "{app}\HyperFVM.ico"
Name: "{group}\{cm:UninstallProgram,{#AppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; IconFilename: "{app}\HyperFVM.ico"; Tasks: desktopicon

[Run]
; 结束页的「立即运行」勾选框；静默安装时跳过，避免装完就弹窗。
Filename: "{app}\{#AppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(AppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Code]
// 应用是托盘常驻的，窗口关了进程还在：覆盖安装时旧 exe 一直被占用，复制文件会失败。
// 所以在开始写文件前把旧进程结束掉（应用没有未保存的文档，强杀是安全的）。
// HyperFVM.exe 没在运行时 taskkill 返回 128，忽略即可。
procedure CurStepChanged(CurStep: TSetupStep);
var
  ResultCode: Integer;
begin
  if CurStep = ssInstall then
    Exec('taskkill.exe', '/IM {#AppExeName} /F', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;
