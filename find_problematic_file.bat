@echo off
echo Finding problematic Kotlin file causing dangling modifier error...

REM First, let's try the most common suspects
echo Checking common problematic patterns...

REM Look for files ending with modifiers
echo Searching for files with potential dangling modifiers...
findstr /E /C:"private" app\src\main\java\*.kt 2>nul
findstr /E /C:"public" app\src\main\java\*.kt 2>nul  
findstr /E /C:"protected" app\src\main\java\*.kt 2>nul
findstr /E /C:"internal" app\src\main\java\*.kt 2>nul

REM Look for files with incomplete declarations
echo Searching for incomplete declarations...
findstr /E /C:"class" app\src\main\java\*.kt 2>nul
findstr /E /C:"interface" app\src\main\java\*.kt 2>nul
findstr /E /C:"fun" app\src\main\java\*.kt 2>nul
findstr /E /C:"val" app\src\main\java\*.kt 2>nul
findstr /E /C:"var" app\src\main\java\*.kt 2>nul

echo Search completed. Check results above for any suspicious patterns.
pause