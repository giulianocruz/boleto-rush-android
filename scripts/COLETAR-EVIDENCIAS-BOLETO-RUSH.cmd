@echo off
setlocal
chcp 65001 >nul
title Boleto Rush - Evidencias pos-partida 2.3.15
set "ROOT=%~dp0"
set "APK=%ROOT%BoletoRush-2.3.15-VIDEO-SEGURO-QA.apk"
set "SCRIPT=%ROOT%qa_android_windows.ps1"
if not exist "%SCRIPT%" (
  echo ERRO: roteiro de coleta nao encontrado.
  pause
  exit /b 2
)
if not exist "%APK%" (
  echo ERRO: APK da versao QA 2.3.15 nao encontrado.
  pause
  exit /b 2
)
echo.
echo BOLETO RUSH - EVIDENCIAS POS-PARTIDA
echo.
echo 1. Conecte o celular com depuracao USB autorizada.
echo 2. Deixe o Boleto Rush QA ABERTO na tela antes de coletar.
echo 3. Este modo NAO reinstala, reinicia ou apaga o aplicativo.
echo 4. A tela atual, logs do processo e relatorio ficam somente neste PC.
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" -ApkPath "%APK%" -CollectOnly
set "CODE=%ERRORLEVEL%"
echo.
if "%CODE%"=="0" (
  echo Coleta concluida. Veja a pasta QA-Evidencias.
) else (
  echo Coleta nao realizada. Confira o erro mostrado acima.
)
pause
exit /b %CODE%
