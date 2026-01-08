@echo off
REM Script pour compiler et executer Retroconception avec architecture MVC

REM Se placer dans le dossier du script
cd /d "%~dp0"

REM Compiler tous les fichiers Java avec les packages
echo Compilation du projet...
javac -d class @compile.list

if %errorlevel% neq 0 (
    echo.
    echo X Erreur de compilation
    exit /b 1
)

echo. 
echo √ Compilation reussie

REM Executer le programme
java -cp class controleur.Controleur %*