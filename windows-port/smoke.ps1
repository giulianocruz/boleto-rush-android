$ErrorActionPreference = 'Stop'
$exe = Join-Path ([Environment]::GetFolderPath('DesktopDirectory')) 'BoletoRush-WINDOWS-JOGAVEL-v1.4.exe'
$p = Start-Process -FilePath $exe -ArgumentList '--self-test' -Wait -PassThru
Write-Output ("SELFTEST_EXIT=" + $p.ExitCode)
if($p.ExitCode -ne 0) { throw "Self-test falhou" }
$window = Start-Process -FilePath $exe -PassThru
Start-Sleep -Seconds 3
$window.Refresh()
Write-Output ("RUNNING=" + (-not $window.HasExited))
Write-Output ("WINDOW=" + $window.MainWindowTitle)
Write-Output ("PID=" + $window.Id)
if($window.HasExited) { throw "Jogo fechou prematuramente" }
