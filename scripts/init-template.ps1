#Requires -Version 7.0
<#
.SYNOPSIS
    Initialises this template project as a concrete application: replaces the package name,
    the app name, the class-name prefix and the Gradle convention plugin prefix.

.DESCRIPTION
    One-shot "de-templating" of the project. The following markers are rewritten:

      1. Package     com.example.template                    -> <PackageName>
                     (also updates applicationId, every module namespace, package/import
                      statements, the build-logic group, and renames the
                      com/example/template directories)
      2. App name    <string name="app_name">Template</string> -> <AppName>
                     rootProject.name = "Template"              -> <RootProjectName>
                     README H1 "# Android Application Template" -> "# <AppName>"
      3. Theme       Theme.Template                           -> Theme.<RootProjectName>
      4. Plugins     template.android.application / .library / .feature /
                     template.compose / .hilt / .room / .serialization /
                     template.kotlin.library                  -> <PluginPrefix>.xxx
                     README sample database name "template.db" -> "<PluginPrefix>.db"
      5. Classes     TemplateApplication / TemplateApp / TemplateAppState /
                     TemplateNavHost / rememberTemplateAppState
                                                              -> <ClassPrefix>Application / ...
                     Source file names are renamed as well: TemplateApp.kt -> <ClassPrefix>App.kt

    Only text files are touched (.kt, .kts, .xml, .toml, .properties, .md, .json, .txt, .pro,
    .yml, ...). build, .gradle, .idea, .kotlin, .git and other generated directories are skipped,
    binary files are ignored, and the original file encoding (including BOM) is preserved.
    The directory holding this script is never modified, so the script can be re-run safely.
    Keep the script in a subdirectory (e.g. scripts/), not in the project root.

.PARAMETER PackageName
    New package name / applicationId, e.g. com.acme.myapp. At least two segments.

.PARAMETER AppName
    Display name of the app, e.g. "My App". Written to strings.xml (app_name) and the README title.

.PARAMETER ClassPrefix
    Prefix for class names. Derived from AppName by default: PascalCase with a trailing
    App / Application removed. For AppName "My App" the prefix is "My", so TemplateApp becomes
    MyApp and TemplateApplication becomes MyApplication.

.PARAMETER RootProjectName
    rootProject.name and the theme name suffix. Defaults to the PascalCase form of AppName.

.PARAMETER PluginPrefix
    Prefix of the Gradle convention plugin ids. Defaults to the last package segment, e.g.
    com.acme.myapp -> myapp.*. Pass "template" to keep the plugin ids unchanged.

.PARAMETER ProjectRoot
    Project root directory. Defaults to the parent directory of this script.

.PARAMETER DryRun
    Print the planned replacements and directory renames without writing anything.

.PARAMETER Yes
    Skip the interactive confirmation prompt.

.EXAMPLE
    pwsh ./scripts/init-template.ps1 -PackageName com.acme.myapp -AppName "My App"

.EXAMPLE
    pwsh ./scripts/init-template.ps1 -PackageName com.acme.myapp -AppName "My App" -DryRun

.EXAMPLE
    pwsh ./scripts/init-template.ps1 -PackageName com.acme.myapp -AppName "My App" `
        -ClassPrefix My -RootProjectName MyApp -PluginPrefix myapp -Yes
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$PackageName,

    [Parameter(Mandatory = $true, Position = 1)]
    [string]$AppName,

    [string]$ClassPrefix,
    [string]$RootProjectName,
    [string]$PluginPrefix,

    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),

    [switch]$DryRun,
    [switch]$Yes
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# Existing markers of the template. Change only this block if the template
# naming ever changes.
# ---------------------------------------------------------------------------

$OldPackage = 'com.example.template'
$OldClassToken = 'Template'
$OldPluginPrefix = 'template'

# Text file extensions that take part in the replacement.
$TextExtensions = @(
    '.kt', '.kts', '.gradle', '.xml', '.toml', '.properties', '.pro',
    '.md', '.json', '.txt', '.yml', '.yaml', '.cfg', '.conf', '.html',
    '.gitignore', '.gitattributes', '.editorconfig', '.bat', '.sh'
)

# Directory names that are never traversed.
$ExcludedDirNames = @(
    '.git', '.gradle', '.idea', '.kotlin', '.fleet', '.vs', '.cxx',
    'build', 'out', 'node_modules'
)

# File names that are never touched.
$ExcludedFileNames = @('local.properties')

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

function ConvertTo-PascalCase {
    param([string]$Value)

    $parts = @($Value -split '[^A-Za-z0-9]+' | Where-Object { $_ })
    $pascal = ($parts | ForEach-Object {
            if ($_.Length -eq 1) { $_.ToUpperInvariant() }
            else { $_.Substring(0, 1).ToUpperInvariant() + $_.Substring(1) }
        }) -join ''

    if ([string]::IsNullOrWhiteSpace($pascal)) { return '' }
    if ($pascal -notmatch '^[A-Za-z]') { $pascal = 'App' + $pascal }
    return $pascal
}

function Read-TextFileWithEncoding {
    param([string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $skip = 0
    $encoding = [System.Text.UTF8Encoding]::new($false)

    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        $encoding = [System.Text.UTF8Encoding]::new($true)
        $skip = 3
    }
    elseif ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE) {
        $encoding = [System.Text.UnicodeEncoding]::new($false, $true)
        $skip = 2
    }
    elseif ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFE -and $bytes[1] -eq 0xFF) {
        $encoding = [System.Text.UnicodeEncoding]::new($true, $true)
        $skip = 2
    }

    return [pscustomobject]@{
        Text     = $encoding.GetString($bytes, $skip, $bytes.Length - $skip)
        Encoding = $encoding
    }
}

function Write-TextFileWithEncoding {
    param(
        [string]$Path,
        [string]$Text,
        [System.Text.Encoding]$Encoding
    )

    [System.IO.File]::WriteAllText($Path, $Text, $Encoding)
}

# Case-sensitive literal replacement. Returns the (possibly) updated text.
function Replace-Literal {
    param(
        [string]$Text,
        [string]$Old,
        [string]$New
    )

    if ([string]::IsNullOrEmpty($Old) -or [string]::IsNullOrEmpty($New) -or $Old -ceq $New) { return $Text }

    $count = [regex]::Matches($Text, [regex]::Escape($Old)).Count
    if ($count -eq 0) { return $Text }

    $script:hitCount += $count
    return $Text.Replace($Old, $New)
}

# Regex replacement. Returns the (possibly) updated text.
function Replace-Pattern {
    param(
        [string]$Text,
        [string]$Pattern,
        [string]$Replacement
    )

    $count = [regex]::Matches($Text, $Pattern).Count
    if ($count -eq 0) { return $Text }

    $script:hitCount += $count
    return [regex]::Replace($Text, $Pattern, $Replacement)
}

# Collect the text files to process, skipping excluded directories.
function Get-ProjectTextFiles {
    param(
        [string]$Root,
        [string[]]$ExcludedDirs
    )

    $files = [System.Collections.Generic.List[string]]::new()
    $stack = [System.Collections.Generic.Stack[string]]::new()
    $stack.Push($Root)

    while ($stack.Count -gt 0) {
        $current = $stack.Pop()

        foreach ($entry in [System.IO.Directory]::EnumerateFileSystemEntries($current)) {
            if ([System.IO.Directory]::Exists($entry)) {
                $name = [System.IO.Path]::GetFileName($entry)
                if ($ExcludedDirNames -contains $name) { continue }
                if ($ExcludedDirs -contains $entry) { continue }
                $stack.Push($entry)
                continue
            }

            $fileName = [System.IO.Path]::GetFileName($entry)
            if ($ExcludedFileNames -contains $fileName) { continue }

            $ext = [System.IO.Path]::GetExtension($fileName).ToLowerInvariant()
            if ($TextExtensions -contains $ext) { $files.Add($entry) }
        }
    }

    return $files
}

# ---------------------------------------------------------------------------
# Validation and derived values
# ---------------------------------------------------------------------------

$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$ScriptDirectory = (Resolve-Path -LiteralPath (Split-Path -Parent $PSCommandPath)).Path

# The whole directory holding the scripts is excluded, so the .sh/.ps1 ports never
# rewrite each other. If that directory is the project root, nothing would be replaced.
$ExcludedDirs = @($ScriptDirectory)
if ($ScriptDirectory -eq $ProjectRoot) {
    throw 'This script must live in a subdirectory (e.g. scripts/), not in the project root.'
}

if ($PackageName -notmatch '^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)+$') {
    throw "Invalid package name: '$PackageName'. Example: com.acme.myapp (at least two segments, letters/digits/underscores only)."
}

if ([string]::IsNullOrWhiteSpace($AppName)) {
    throw 'App name must not be empty.'
}

if ($PackageName -eq $OldPackage -and $AppName -eq $OldClassToken) {
    throw 'Package name and app name are still the template defaults, nothing to replace.'
}

$pascalAppName = ConvertTo-PascalCase $AppName

if (-not $ClassPrefix) {
    $derived = $pascalAppName
    if ($derived.EndsWith('Application') -and $derived.Length -gt 'Application'.Length) {
        $derived = $derived.Substring(0, $derived.Length - 'Application'.Length)
    }
    elseif ($derived.EndsWith('App') -and $derived.Length -gt 'App'.Length) {
        $derived = $derived.Substring(0, $derived.Length - 'App'.Length)
    }
    $ClassPrefix = $derived
}

if ($ClassPrefix -notmatch '^[A-Za-z][A-Za-z0-9_]*$') {
    throw "Invalid class prefix: '$ClassPrefix'. It must be a valid identifier (letters, digits, underscore)."
}

if (-not $RootProjectName) { $RootProjectName = $pascalAppName }
if ($RootProjectName -notmatch '^[A-Za-z][A-Za-z0-9_.-]*$') {
    throw "Invalid rootProject name: '$RootProjectName'."
}

if (-not $PluginPrefix) {
    $last = ($PackageName -split '\.')[-1].ToLowerInvariant()
    $PluginPrefix = ($last -replace '[^a-z0-9]', '')
    if ([string]::IsNullOrEmpty($PluginPrefix)) { $PluginPrefix = 'app' }
}
if ($PluginPrefix -notmatch '^[a-z][a-z0-9]*$') {
    throw "Invalid plugin prefix: '$PluginPrefix'. Lowercase letters and digits only, starting with a letter."
}

$xmlAppName = [System.Security.SecurityElement]::Escape($AppName)
$newPackagePath = $PackageName.Replace('.', [System.IO.Path]::DirectorySeparatorChar)

# ---------------------------------------------------------------------------
# Plan and confirmation
# ---------------------------------------------------------------------------

$mode = if ($DryRun) { 'DRY RUN (nothing is written)' } else { 'APPLY' }

Write-Host ''
Write-Host '================ TEMPLATE INIT PLAN ================' -ForegroundColor Cyan
Write-Host ("Project root   : {0}" -f $ProjectRoot)
Write-Host ("Mode           : {0}" -f $mode)
Write-Host ''
Write-Host ("Package        : {0} -> {1}" -f $OldPackage, $PackageName)
Write-Host ("App name       : {0} -> {1}" -f $OldClassToken, $AppName)
Write-Host ("rootProject    : {0} -> {1}" -f $OldClassToken, $RootProjectName)
Write-Host ("Theme          : Theme.{0} -> Theme.{1}" -f $OldClassToken, $RootProjectName)
Write-Host ("Plugin prefix  : {0}.* -> {1}.*" -f $OldPluginPrefix, $PluginPrefix)
Write-Host ("Class prefix   : {0}* -> {1}*" -f $OldClassToken, $ClassPrefix)
Write-Host '====================================================' -ForegroundColor Cyan
Write-Host ''

if (-not $DryRun -and -not $Yes) {
    $answer = Read-Host 'Continue? (y/N)'
    if ($answer -notmatch '^[Yy]') {
        Write-Host 'Cancelled, nothing was modified.' -ForegroundColor Yellow
        return
    }
}

# ---------------------------------------------------------------------------
# Step 1: replace text content
# ---------------------------------------------------------------------------

$script:hitCount = 0
$changedFiles = [System.Collections.Generic.List[string]]::new()

$files = Get-ProjectTextFiles -Root $ProjectRoot -ExcludedDirs $ExcludedDirs

foreach ($file in $files) {
    try {
        $loaded = Read-TextFileWithEncoding -Path $file
    }
    catch {
        Write-Warning ("Skipping unreadable file: {0} ({1})" -f $file, $_.Exception.Message)
        continue
    }

    $text = $loaded.Text
    $original = $text

    # 1.1 app_name display name
    $text = Replace-Pattern $text '<string\s+name="app_name">[^<]*</string>' ('<string name="app_name">' + $xmlAppName + '</string>')

    # 1.2 rootProject.name
    $text = Replace-Pattern $text 'rootProject\.name\s*=\s*"Template"' ('rootProject.name = "' + $RootProjectName + '"')

    # 1.3 README H1 (lookahead so the line ending is never consumed)
    $text = Replace-Pattern $text '(?m)^#\s+Android Application Template(?=\s*$)' ('# ' + $AppName)

    # 1.4 Theme name (before the generic class token, otherwise it becomes Theme.<ClassPrefix>)
    $text = Replace-Literal $text ('Theme.' + $OldClassToken) ('Theme.' + $RootProjectName)

    # 1.5 Convention plugin ids (before the package name, in case the new package contains "template")
    $pluginPattern = '\b' + [regex]::Escape($OldPluginPrefix) +
    '\.(android\.application|android\.library|android\.feature|compose|hilt|room|serialization|kotlin\.library)\b'
    $text = Replace-Pattern $text $pluginPattern ($PluginPrefix + '.$1')

    # 1.6 Sample database name in the README ("template.db")
    $text = Replace-Literal $text ($OldPluginPrefix + '.db') ($PluginPrefix + '.db')

    # 1.7 Package name
    $text = Replace-Literal $text $OldPackage $PackageName

    # 1.8 Remaining class-name prefix
    $text = Replace-Literal $text $OldClassToken $ClassPrefix

    if ($text -cne $original) {
        $changedFiles.Add($file)
        if (-not $DryRun) {
            Write-TextFileWithEncoding -Path $file -Text $text -Encoding $loaded.Encoding
        }
    }
}

# ---------------------------------------------------------------------------
# Step 2: rename files carrying the class-name prefix (TemplateApp.kt -> MyApp.kt)
# ---------------------------------------------------------------------------

$renamedFiles = [System.Collections.Generic.List[string]]::new()

foreach ($file in $files) {
    $fileName = [System.IO.Path]::GetFileName($file)
    if (-not $fileName.Contains($OldClassToken)) { continue }

    $newName = $fileName.Replace($OldClassToken, $ClassPrefix)
    if ($newName -ceq $fileName) { continue }

    $newPath = Join-Path (Split-Path -Parent $file) $newName
    Write-Host ''
    Write-Host ("Rename file    : {0}" -f $fileName)
    Write-Host ("              -> {0}" -f $newName)

    if ($DryRun) { continue }

    if (Test-Path -LiteralPath $newPath) {
        Write-Warning ("Target already exists, skipping rename: {0}" -f $newPath)
        continue
    }

    Move-Item -LiteralPath $file -Destination $newPath -Force
    $renamedFiles.Add($newPath)
}

# ---------------------------------------------------------------------------
# Step 3: rename the com/example/template package directories
# ---------------------------------------------------------------------------

$oldSegments = $OldPackage.Split('.')
$oldTailPath = $oldSegments -join [System.IO.Path]::DirectorySeparatorChar
$dirPattern = '[\\/]' + [regex]::Escape($oldTailPath) + '$'

$packageDirs = [System.Collections.Generic.List[string]]::new()
$stack = [System.Collections.Generic.Stack[string]]::new()
$stack.Push($ProjectRoot)

while ($stack.Count -gt 0) {
    $current = $stack.Pop()
    foreach ($entry in [System.IO.Directory]::EnumerateDirectories($current)) {
        $name = [System.IO.Path]::GetFileName($entry)
        if ($ExcludedDirNames -contains $name) { continue }
        if ($ExcludedDirs -contains $entry) { continue }

        if ($entry -match $dirPattern) { $packageDirs.Add($entry) }
        else { $stack.Push($entry) }
    }
}

foreach ($dir in $packageDirs) {
    # Keep the source root in front of the package path (e.g. <module>/src/main/kotlin)
    # and swap only the com/example/template part.
    $baseRoot = $dir.Substring(0, $dir.Length - $oldTailPath.Length).TrimEnd('\', '/')
    $target = Join-Path $baseRoot $newPackagePath

    Write-Host ''
    Write-Host ("Rename dir     : {0}" -f $dir)
    Write-Host ("              -> {0}" -f $target)

    if ($DryRun) { continue }

    if (-not (Test-Path -LiteralPath $target)) {
        New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
        Move-Item -LiteralPath $dir -Destination $target -Force
    }
    else {
        # The target already exists (old and new package share a prefix): merge item by item.
        New-Item -ItemType Directory -Path $target -Force | Out-Null
        foreach ($child in [System.IO.Directory]::EnumerateFileSystemEntries($dir)) {
            Move-Item -LiteralPath $child -Destination $target -Force
        }
        Remove-Item -LiteralPath $dir -Recurse -Force
    }

    # Remove the now-empty leftovers (com, example, ...).
    $cursor = Split-Path -Parent $dir
    while ($cursor -and $cursor.StartsWith($ProjectRoot) -and $cursor -ne $ProjectRoot) {
        $hasChild = @([System.IO.Directory]::EnumerateFileSystemEntries($cursor)).Count -gt 0
        if ($hasChild) { break }
        Remove-Item -LiteralPath $cursor -Force
        $cursor = Split-Path -Parent $cursor
    }
}

# ---------------------------------------------------------------------------
# Step 4: check for leftovers (file content and file names)
# ---------------------------------------------------------------------------

$leftover = [System.Collections.Generic.List[string]]::new()

if (-not $DryRun) {
    foreach ($file in (Get-ProjectTextFiles -Root $ProjectRoot -ExcludedDirs $ExcludedDirs)) {
        $text = (Read-TextFileWithEncoding -Path $file).Text
        $hasMarker = $text.Contains($OldPackage) -or
        $text.Contains($OldClassToken) -or
        $text.Contains($OldPluginPrefix + '.')
        $badName = [System.IO.Path]::GetFileName($file).Contains($OldClassToken)

        if ($hasMarker -or $badName) { $leftover.Add($file) }
    }
}

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------

Write-Host ''
Write-Host '================ RESULT ================' -ForegroundColor Cyan
Write-Host ("Replacements   : {0}" -f $script:hitCount)
Write-Host ("Files updated  : {0}" -f $changedFiles.Count)
Write-Host ("Files renamed  : {0}" -f $renamedFiles.Count)
Write-Host ("Dirs renamed   : {0}" -f $packageDirs.Count)

if ($changedFiles.Count -gt 0) {
    Write-Host ''
    $changedFiles | ForEach-Object { Write-Host ("  {0}" -f $_.Substring($ProjectRoot.Length).TrimStart('\', '/')) }
}

if ($leftover.Count -gt 0) {
    Write-Host ''
    Write-Warning 'These files still contain template markers, please review them manually:'
    $leftover | ForEach-Object { Write-Host ("  {0}" -f $_.Substring($ProjectRoot.Length).TrimStart('\', '/')) }
}

Write-Host ''
if ($DryRun) {
    Write-Host 'DRY RUN finished, nothing was written. Re-run without -DryRun to apply.' -ForegroundColor Yellow
}
else {
    Write-Host 'Done. Suggested next steps:' -ForegroundColor Green
    Write-Host '  1. Restart Android Studio (package directories changed, re-indexing is required)'
    Write-Host '  2. ./gradlew :app:assembleDebug to verify the build'
    Write-Host '  3. git add -A and commit yourself (this script never commits)'
}
Write-Host '========================================' -ForegroundColor Cyan
