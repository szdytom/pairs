$ErrorActionPreference = 'Stop'

# This script formats Java source files under src/ using clang-format.

$clangFormat = (Get-Command clang-format -ErrorAction SilentlyContinue).Source
if (-not $clangFormat) {
	$candidates = @(
		"C:\Program Files\LLVM\bin\clang-format.exe",
		"C:\Program Files (x86)\LLVM\bin\clang-format.exe",
		"$env:LOCALAPPDATA\Programs\LLVM\bin\clang-format.exe"
	)
	$clangFormat = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}

if (-not $clangFormat) {
	Write-Error "Error: clang-format is not installed."
	exit 1
}

$clangVersion = & $clangFormat --version 2>$null

if ($clangVersion -notmatch 'version\s+2[2-9]') {
	Write-Error "Error: clang-format version 22 or higher is required."
	exit 1
}

Get-ChildItem -Path "src" -Recurse -File -Filter "*.java" |
	ForEach-Object {
		& $clangFormat -i $_.FullName
	}
