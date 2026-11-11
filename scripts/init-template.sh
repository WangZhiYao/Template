#!/usr/bin/env bash
#
# init-template.sh - bash port of scripts/init-template.ps1.
#
# Initialises this template project as a concrete application: replaces the package name,
# the app name, the class-name prefix and the Gradle convention plugin prefix.
#
# Markers rewritten:
#   1. Package     com.example.template                     -> <package>
#                  (also updates applicationId, every module namespace, package/import
#                   statements, the build-logic group, and renames the
#                   com/example/template directories)
#   2. App name    <string name="app_name">Template</string> -> <app name>
#                  rootProject.name = "Template"              -> <root project name>
#                  README H1 "# Android Application Template" -> "# <app name>"
#   3. Theme       Theme.Template                           -> Theme.<root project name>
#   4. Plugins     template.android.application / .library / .feature /
#                  template.compose / .hilt / .room / .serialization /
#                  template.kotlin.library                   -> <plugin prefix>.xxx
#                  README sample database name "template.db" -> "<plugin prefix>.db"
#   5. Classes     TemplateApplication / TemplateApp / TemplateAppState /
#                  TemplateNavHost / rememberTemplateAppState
#                                                           -> <class prefix>Application / ...
#                  Source file names are renamed as well: TemplateApp.kt -> <class prefix>App.kt
#
# Only text files are touched. build, .gradle, .idea, .kotlin, .git and other generated
# directories are skipped, binary files are ignored, and the original bytes (BOM, CRLF/LF,
# UTF-8) are preserved: perl edits the bytes in place, so a Windows checkout keeps its CRLF
# endings and a Linux checkout keeps its LF endings. The directory holding this script is
# never modified, so the script can be re-run safely. Keep it in a subdirectory (scripts/),
# not in the project root.
#
# Requires: bash 3.2+ (Linux, macOS, Git Bash on Windows), perl, find, grep, cmp.
#
# Usage:
#   ./scripts/init-template.sh --package com.acme.myapp --app "My App"
#   ./scripts/init-template.sh --package com.acme.myapp --app "My App" --dry-run
#
set -eu

OLD_PACKAGE='com.example.template'
OLD_CLASS='Template'
OLD_PLUGIN='template'

PRUNE_DIRS='.git .gradle .idea .kotlin .fleet .vs .cxx build out node_modules'
EXCLUDED_FILES='local.properties'

PACKAGE_NAME=''
APP_NAME=''
CLASS_PREFIX=''
ROOT_PROJECT_NAME=''
PLUGIN_PREFIX=''
PROJECT_ROOT=''
DRY_RUN=0
ASSUME_YES=0

usage() {
    sed -n '2,40p' "$0" | sed -e 's/^# \{0,1\}//'
    cat <<'EOF'

Options:
  -p, --package <name>        New package name / applicationId, e.g. com.acme.myapp (required).
  -a, --app <name>            App display name, e.g. "My App" (required).
      --class-prefix <name>   Class-name prefix. Defaults to the PascalCase form of the app
                              name with a trailing App/Application removed.
      --root-project <name>   rootProject.name and theme suffix. Defaults to the PascalCase
                              app name.
      --plugin-prefix <name>  Gradle convention plugin id prefix. Defaults to the last
                              package segment.
      --project-root <dir>    Project root. Defaults to the parent of this script's directory.
      --dry-run               Print the plan without writing anything.
  -y, --yes                   Skip the interactive confirmation.
  -h, --help                  Show this help.
EOF
}

die() {
    printf 'ERROR: %s\n' "$1" >&2
    exit 1
}

# PascalCase: "My App" -> "MyApp"
pascal_case() {
    printf '%s' "$1" \
        | LC_ALL=C sed -e 's/[^A-Za-z0-9][^A-Za-z0-9]*/ /g' -e 's/^ *//' -e 's/ *$//' \
        | LC_ALL=C awk '{
            out = ""
            for (i = 1; i <= NF; i++) {
                w = $i
                out = out toupper(substr(w, 1, 1)) substr(w, 2)
            }
            print out
        }'
}

is_text_file() {
    case "$1" in
        *.kt | *.kts | *.gradle | *.xml | *.toml | *.properties | *.pro | *.md | *.json \
            | *.txt | *.yml | *.yaml | *.cfg | *.conf | *.html | *.gitignore \
            | *.gitattributes | *.editorconfig | *.bat | *.sh) return 0 ;;
        *) return 1 ;;
    esac
}

# Collect the text files to process (excludes generated dirs and this script's directory).
collect_text_files() {
    prune=''
    for d in $PRUNE_DIRS; do
        prune="$prune -name $d -o"
    done
    # shellcheck disable=SC2086
    find "$ROOT" \( $prune -false \) -prune -o -type f -print 2>/dev/null \
        | LC_ALL=C sort \
        | while IFS= read -r f; do
            case "$f" in
                "$SCRIPT_DIR"/*) continue ;;
            esac
            base=${f##*/}
            case " $EXCLUDED_FILES " in *" $base "*) continue ;; esac
            is_text_file "$f" && printf '%s\n' "$f"
        done
}

collect_package_dirs() {
    prune=''
    for d in $PRUNE_DIRS; do
        prune="$prune -name $d -o"
    done
    # shellcheck disable=SC2086
    find "$ROOT" \( $prune -false \) -prune -o -type d -print 2>/dev/null \
        | LC_ALL=C sort -r \
        | while IFS= read -r d; do
            case "$d" in
                "$SCRIPT_DIR"/*) continue ;;
                */com/example/template) printf '%s\n' "$d" ;;
            esac
        done
}

# ---------------------------------------------------------------------------
# Arguments
# ---------------------------------------------------------------------------

while [ $# -gt 0 ]; do
    case "$1" in
        -p | --package) PACKAGE_NAME=${2:-}; shift 2 ;;
        -a | --app) APP_NAME=${2:-}; shift 2 ;;
        --class-prefix) CLASS_PREFIX=${2:-}; shift 2 ;;
        --root-project) ROOT_PROJECT_NAME=${2:-}; shift 2 ;;
        --plugin-prefix) PLUGIN_PREFIX=${2:-}; shift 2 ;;
        --project-root) PROJECT_ROOT=${2:-}; shift 2 ;;
        --dry-run) DRY_RUN=1; shift ;;
        -y | --yes) ASSUME_YES=1; shift ;;
        -h | --help) usage; exit 0 ;;
        *) die "Unknown argument: $1 (try --help)" ;;
    esac
done

[ -n "$PACKAGE_NAME" ] || die 'Missing --package. Try --help.'
[ -n "$APP_NAME" ] || die 'Missing --app. Try --help.'

command -v perl >/dev/null 2>&1 || die 'perl is required (it edits files byte-wise so CRLF/LF and BOM are preserved).'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -z "$PROJECT_ROOT" ]; then
    PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
fi
ROOT="$(cd "$PROJECT_ROOT" && pwd)"

if [ "$SCRIPT_DIR" = "$ROOT" ]; then
    die 'This script must live in a subdirectory (e.g. scripts/), not in the project root.'
fi

# ---------------------------------------------------------------------------
# Validation and derived values
# ---------------------------------------------------------------------------

printf '%s' "$PACKAGE_NAME" | LC_ALL=C grep -Eq '^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)+$' \
    || die "Invalid package name: '$PACKAGE_NAME'. Example: com.acme.myapp (at least two segments)."

if [ "$PACKAGE_NAME" = "$OLD_PACKAGE" ] && [ "$APP_NAME" = "$OLD_CLASS" ]; then
    die 'Package name and app name are still the template defaults, nothing to replace.'
fi

PASCAL_APP_NAME="$(pascal_case "$APP_NAME")"
[ -n "$PASCAL_APP_NAME" ] || die "Cannot derive a class prefix from app name '$APP_NAME'."

if [ -z "$CLASS_PREFIX" ]; then
    case "$PASCAL_APP_NAME" in
        *Application) CLASS_PREFIX=${PASCAL_APP_NAME%Application} ;;
        *App) CLASS_PREFIX=${PASCAL_APP_NAME%App} ;;
        *) CLASS_PREFIX=$PASCAL_APP_NAME ;;
    esac
    [ -n "$CLASS_PREFIX" ] || CLASS_PREFIX=$PASCAL_APP_NAME
fi

printf '%s' "$CLASS_PREFIX" | LC_ALL=C grep -Eq '^[A-Za-z][A-Za-z0-9_]*$' \
    || die "Invalid class prefix: '$CLASS_PREFIX'. It must be a valid identifier."

[ -n "$ROOT_PROJECT_NAME" ] || ROOT_PROJECT_NAME=$PASCAL_APP_NAME
printf '%s' "$ROOT_PROJECT_NAME" | LC_ALL=C grep -Eq '^[A-Za-z][A-Za-z0-9_.-]*$' \
    || die "Invalid root project name: '$ROOT_PROJECT_NAME'."

if [ -z "$PLUGIN_PREFIX" ]; then
    PLUGIN_PREFIX=$(printf '%s' "${PACKAGE_NAME##*.}" | LC_ALL=C tr 'A-Z' 'a-z' | LC_ALL=C tr -cd 'a-z0-9')
    [ -n "$PLUGIN_PREFIX" ] || PLUGIN_PREFIX='app'
fi
printf '%s' "$PLUGIN_PREFIX" | LC_ALL=C grep -Eq '^[a-z][a-z0-9]*$' \
    || die "Invalid plugin prefix: '$PLUGIN_PREFIX'. Lowercase letters and digits only."

NEW_PACKAGE_PATH=$(printf '%s' "$PACKAGE_NAME" | LC_ALL=C tr '.' '/')

# ---------------------------------------------------------------------------
# Plan and confirmation
# ---------------------------------------------------------------------------

if [ "$DRY_RUN" -eq 1 ]; then MODE='DRY RUN (nothing is written)'; else MODE='APPLY'; fi

printf '\n================ TEMPLATE INIT PLAN ================\n'
printf 'Project root   : %s\n' "$ROOT"
printf 'Mode           : %s\n' "$MODE"
printf '\n'
printf 'Package        : %s -> %s\n' "$OLD_PACKAGE" "$PACKAGE_NAME"
printf 'App name       : %s -> %s\n' "$OLD_CLASS" "$APP_NAME"
printf 'rootProject    : %s -> %s\n' "$OLD_CLASS" "$ROOT_PROJECT_NAME"
printf 'Theme          : Theme.%s -> Theme.%s\n' "$OLD_CLASS" "$ROOT_PROJECT_NAME"
printf 'Plugin prefix  : %s.* -> %s.*\n' "$OLD_PLUGIN" "$PLUGIN_PREFIX"
printf 'Class prefix   : %s* -> %s*\n' "$OLD_CLASS" "$CLASS_PREFIX"
printf '====================================================\n\n'

if [ "$DRY_RUN" -eq 0 ] && [ "$ASSUME_YES" -eq 0 ]; then
    printf 'Continue? (y/N) '
    read -r answer || answer=''
    case "$answer" in
        y | Y | yes | YES) ;;
        *) printf 'Cancelled, nothing was modified.\n'; exit 0 ;;
    esac
fi

# ---------------------------------------------------------------------------
# Step 1: replace text content
# ---------------------------------------------------------------------------

CHANGED_COUNT=0
CHANGED_LIST=''
RENAMED_COUNT=0
DIR_COUNT=0
FILE_LIST=$(collect_text_files)

# Rules are applied in this order: theme -> plugins -> db -> package -> class prefix.
# Values are passed through the environment so no shell quoting can leak into the regexes.
REPLACE_CODE='
s{<string name="app_name">[^<]*</string>}{<string name="app_name">$ENV{INIT_APP}</string>};
s{rootProject\.name\s*=\s*"Template"}{rootProject.name = "$ENV{INIT_ROOT}"};
s{^#\s+Android Application Template}{# $ENV{INIT_APP}};
s{Theme\.Template}{Theme.$ENV{INIT_ROOT}}g;
s{template\.(android\.application|android\.library|android\.feature|compose|hilt|room|serialization|kotlin\.library)}{$ENV{INIT_PLUGIN}.$1}g;
s{template\.db}{$ENV{INIT_PLUGIN}.db}g;
s{com\.example\.template}{$ENV{INIT_PACKAGE}}g;
s{Template}{$ENV{INIT_CLASS}}g;
'

if [ -n "$FILE_LIST" ]; then
    while IFS= read -r file; do
        [ -n "$file" ] || continue
        tmp="$file.inittmp.$$"

        if INIT_APP="$APP_NAME" INIT_ROOT="$ROOT_PROJECT_NAME" INIT_PLUGIN="$PLUGIN_PREFIX" \
            INIT_PACKAGE="$PACKAGE_NAME" INIT_CLASS="$CLASS_PREFIX" \
            perl -pe "$REPLACE_CODE" "$file" >"$tmp" 2>/dev/null; then
            if cmp -s "$file" "$tmp"; then
                rm -f "$tmp"
            else
                CHANGED_COUNT=$((CHANGED_COUNT + 1))
                CHANGED_LIST="$CHANGED_LIST$file
"
                if [ "$DRY_RUN" -eq 0 ]; then
                    cat "$tmp" >"$file"
                fi
                rm -f "$tmp"
            fi
        else
            rm -f "$tmp"
            printf 'WARNING: could not process %s, skipped\n' "$file" >&2
        fi
    done <<EOF
$FILE_LIST
EOF
fi

# ---------------------------------------------------------------------------
# Step 2: rename files carrying the class-name prefix (TemplateApp.kt -> MyApp.kt)
# ---------------------------------------------------------------------------

if [ -n "$FILE_LIST" ]; then
    while IFS= read -r file; do
        [ -n "$file" ] || continue
        base=${file##*/}
        case "$base" in
            *Template*) ;;
            *) continue ;;
        esac

        new_base=$(printf '%s' "$base" | LC_ALL=C sed -e "s|Template|$CLASS_PREFIX|g")
        dir=${file%/*}
        new_path="$dir/$new_base"

        printf '\nRename file    : %s\n' "$base"
        printf '              -> %s\n' "$new_base"

        [ "$DRY_RUN" -eq 1 ] && continue
        if [ -e "$new_path" ]; then
            printf 'WARNING: target already exists, skipping rename: %s\n' "$new_path" >&2
            continue
        fi
        mv "$file" "$new_path"
        RENAMED_COUNT=$((RENAMED_COUNT + 1))
    done <<EOF
$FILE_LIST
EOF
fi

# ---------------------------------------------------------------------------
# Step 3: rename the com/example/template package directories
# ---------------------------------------------------------------------------

DIR_LIST=$(collect_package_dirs)

if [ -n "$DIR_LIST" ]; then
    while IFS= read -r dir; do
        [ -n "$dir" ] || continue
        base_root=${dir%/com/example/template}
        target="$base_root/$NEW_PACKAGE_PATH"

        printf '\nRename dir     : %s\n' "$dir"
        printf '              -> %s\n' "$target"

        DIR_COUNT=$((DIR_COUNT + 1))
        [ "$DRY_RUN" -eq 1 ] && continue

        mkdir -p "$(dirname "$target")"
        if [ ! -e "$target" ]; then
            mv "$dir" "$target"
        else
            # Old and new package share a prefix: merge item by item.
            mkdir -p "$target"
            find "$dir" -mindepth 1 -maxdepth 1 -exec mv {} "$target"/ \;
            rmdir "$dir" 2>/dev/null || rm -rf "$dir"
        fi

        # Remove the now-empty leftovers (com, example, ...).
        cursor=${dir%/*}
        while [ -n "$cursor" ] && [ "$cursor" != "$ROOT" ]; do
            case "$cursor" in
                "$ROOT"/*) ;;
                *) break ;;
            esac
            if [ -z "$(ls -A "$cursor" 2>/dev/null)" ]; then
                rmdir "$cursor" 2>/dev/null || break
                cursor=${cursor%/*}
            else
                break
            fi
        done
    done <<EOF
$DIR_LIST
EOF
fi

# ---------------------------------------------------------------------------
# Step 4: check for leftovers (file content and file names)
# ---------------------------------------------------------------------------

LEFTOVER=''
if [ "$DRY_RUN" -eq 0 ]; then
    for file in $(collect_text_files); do
        [ -n "$file" ] || continue
        base=${file##*/}
        case "$base" in
            *Template*)
                LEFTOVER="$LEFTOVER$file
"
                continue
                ;;
        esac
        if LC_ALL=C grep -q -e 'Template' -e 'com\.example\.template' -e 'template\.' "$file" 2>/dev/null; then
            LEFTOVER="$LEFTOVER$file
"
        fi
    done
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------

printf '\n================ RESULT ================\n'
printf 'Files updated  : %s\n' "$CHANGED_COUNT"
printf 'Files renamed  : %s\n' "$RENAMED_COUNT"
printf 'Dirs renamed   : %s\n' "$DIR_COUNT"

if [ -n "$CHANGED_LIST" ]; then
    printf '\n'
    printf '%s' "$CHANGED_LIST" | while IFS= read -r f; do
        [ -n "$f" ] || continue
        printf '  %s\n' "${f#"$ROOT"/}"
    done
fi

if [ -n "$LEFTOVER" ]; then
    printf '\nWARNING: these files still contain template markers, please review them manually:\n'
    printf '%s' "$LEFTOVER" | while IFS= read -r f; do
        [ -n "$f" ] || continue
        printf '  %s\n' "${f#"$ROOT"/}"
    done
fi

printf '\n'
if [ "$DRY_RUN" -eq 1 ]; then
    printf 'DRY RUN finished, nothing was written. Re-run without --dry-run to apply.\n'
else
    printf 'Done. Suggested next steps:\n'
    printf '  1. Restart Android Studio (package directories changed, re-indexing is required)\n'
    printf '  2. ./gradlew :app:assembleDebug to verify the build\n'
    printf '  3. git add -A and commit yourself (this script never commits)\n'
fi
printf '========================================\n'
