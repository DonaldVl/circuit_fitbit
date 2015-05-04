@echo off
cls
rem Storing current directory first
rem echo "Using script at '%0'"
pushd %0\..

set HOME=%~d0%~p0
set ABE_HOME=%HOME%\..\..\..\Ansible\abe

set HTTP_PROXY=http://proxy.cycos.com:8080
set HTTPS_PROXY=https://proxy.cycos.com:8080

call %ABE_HOME%\abe