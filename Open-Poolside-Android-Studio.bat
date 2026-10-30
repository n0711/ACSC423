@echo off
setlocal
set "STUDIO_EXE=%ProgramFiles%\Android\Android Studio\bin\studio64.exe"
if exist "%STUDIO_EXE%" (
  start "" "%STUDIO_EXE%" "%~dp0PoolsideAR"
) else (
  echo Open Android Studio, choose Open, and select:
  echo %~dp0PoolsideAR
  pause
)
