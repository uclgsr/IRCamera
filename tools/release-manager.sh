#!/bin/bash

# IRCamera Release Manager
# Automated release management with version bumping and changelog generation

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
GRADLE_FILE="gradle/libs.versions.toml"
VERSION_PATTERN='versionName = "'
CHANGELOG_FILE="CHANGELOG.md"

show_header() {
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║              🚀 IRCamera Release Manager                      ║${NC}"
    echo -e "${BLUE}║           Version Bumping • Changelog • Automation           ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

show_help() {
    show_header
    echo "Usage: $0 [command] [options]"
    echo ""
    echo "Commands:"
    echo "  current              - Show current version"
    echo "  bump [major|minor|patch] - Bump version"
    echo "  prepare [version]    - Prepare release with version"
    echo "  create [version]     - Create release tag and artifacts"
    echo "  changelog           - Generate changelog from git history"
    echo "  validate            - Validate release readiness"
    echo "  help                - Show this help"
    echo ""
    echo "Examples:"
    echo "  $0 current"
    echo "  $0 bump minor"
    echo "  $0 prepare 1.2.0"
    echo "  $0 create 1.2.0"
    echo ""
}

get_current_version() {
    if [ -f "$GRADLE_FILE" ]; then
        grep "$VERSION_PATTERN" "$GRADLE_FILE" | sed "s/.*$VERSION_PATTERN//" | sed 's/".*//'
    else
        echo "0.0.0"
    fi
}

set_version() {
    local new_version="$1"
    
    if [ -f "$GRADLE_FILE" ]; then
        sed -i "s/${VERSION_PATTERN}[^\"]*\"/${VERSION_PATTERN}${new_version}\"/" "$GRADLE_FILE"
        echo -e "${GREEN}✅ Version updated to $new_version in $GRADLE_FILE${NC}"
    else
        echo -e "${RED}❌ $GRADLE_FILE not found${NC}"
        return 1
    fi
}

bump_version() {
    local bump_type="$1"
    local current_version
    current_version=$(get_current_version)
    
    echo -e "${CYAN}Current version: $current_version${NC}"
    
    # Parse version components
    IFS='.' read -ra VERSION_PARTS <<< "$current_version"
    local major="${VERSION_PARTS[0]:-0}"
    local minor="${VERSION_PARTS[1]:-0}"
    local patch="${VERSION_PARTS[2]:-0}"
    
    # Bump version based on type
    case "$bump_type" in
        "major")
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        "minor")
            minor=$((minor + 1))
            patch=0
            ;;
        "patch")
            patch=$((patch + 1))
            ;;
        *)
            echo -e "${RED}❌ Invalid bump type. Use: major, minor, or patch${NC}"
            return 1
            ;;
    esac
    
    local new_version="$major.$minor.$patch"
    echo -e "${CYAN}New version: $new_version${NC}"
    
    set_version "$new_version"
    echo "$new_version"
}

generate_changelog_entry() {
    local version="$1"
    local date_str
    date_str=$(date '+%Y-%m-%d')
    
    echo -e "${YELLOW}📝 Generating changelog entry for v$version${NC}"
    
    # Get commits since last tag
    local last_tag
    last_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
    
    local temp_changelog
    temp_changelog=$(mktemp)
    
    {
        echo "## [$version] - $date_str"
        echo ""
        
        # Categorize commits
        local added_changes=()
        local changed_changes=()
        local fixed_changes=()
        local security_changes=()
        
        if [ -n "$last_tag" ]; then
            echo "### Changes since $last_tag"
            echo ""
            
            # Parse git log for conventional commits
            while IFS= read -r commit; do
                if [[ $commit =~ ^feat ]]; then
                    added_changes+=("${commit#feat: }")
                elif [[ $commit =~ ^fix ]]; then
                    fixed_changes+=("${commit#fix: }")
                elif [[ $commit =~ ^security ]]; then
                    security_changes+=("${commit#security: }")
                elif [[ $commit =~ ^refactor|^perf|^style ]]; then
                    changed_changes+=("${commit#*: }")
                else
                    changed_changes+=("$commit")
                fi
            done < <(git log "$last_tag"..HEAD --pretty=format:"%s" --no-merges 2>/dev/null || git log --oneline -10 --pretty=format:"%s")
        else
            echo "### Initial release"
            echo ""
            added_changes=("Initial IRCamera release with thermal imaging and BLE support")
        fi
        
        # Format changelog sections
        if [ ${#added_changes[@]} -gt 0 ]; then
            echo "### Added"
            printf '%s\n' "${added_changes[@]}" | sed 's/^/- /'
            echo ""
        fi
        
        if [ ${#changed_changes[@]} -gt 0 ]; then
            echo "### Changed"
            printf '%s\n' "${changed_changes[@]}" | sed 's/^/- /'
            echo ""
        fi
        
        if [ ${#fixed_changes[@]} -gt 0 ]; then
            echo "### Fixed"
            printf '%s\n' "${fixed_changes[@]}" | sed 's/^/- /'
            echo ""
        fi
        
        if [ ${#security_changes[@]} -gt 0 ]; then
            echo "### Security"
            printf '%s\n' "${security_changes[@]}" | sed 's/^/- /'
            echo ""
        fi
        
    } > "$temp_changelog"
    
    # Insert into existing changelog
    if [ -f "$CHANGELOG_FILE" ]; then
        # Create backup
        cp "$CHANGELOG_FILE" "${CHANGELOG_FILE}.bak"
        
        # Find insertion point (after ## [Unreleased])
        if grep -q "## \[Unreleased\]" "$CHANGELOG_FILE"; then
            # Insert after unreleased section
            awk -v new_content="$(cat "$temp_changelog")" '
                /^## \[Unreleased\]/ { print; getline; print; print new_content; print ""; next }
                { print }
            ' "$CHANGELOG_FILE" > "${CHANGELOG_FILE}.tmp"
            mv "${CHANGELOG_FILE}.tmp" "$CHANGELOG_FILE"
        else
            # Insert at beginning of changelog
            {
                echo "# Changelog"
                echo ""
                cat "$temp_changelog"
                echo ""
                tail -n +1 "$CHANGELOG_FILE" 2>/dev/null || true
            } > "${CHANGELOG_FILE}.tmp"
            mv "${CHANGELOG_FILE}.tmp" "$CHANGELOG_FILE"
        fi
    else
        # Create new changelog
        {
            echo "# Changelog"
            echo ""
            echo "All notable changes to the IRCamera project will be documented in this file."
            echo ""
            echo "The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),"
            echo "and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)."
            echo ""
            cat "$temp_changelog"
        } > "$CHANGELOG_FILE"
    fi
    
    rm -f "$temp_changelog"
    echo -e "${GREEN}✅ Changelog updated${NC}"
}

validate_release_readiness() {
    echo -e "${YELLOW}🔍 Validating release readiness${NC}"
    echo "────────────────────────────────"
    
    local issues=0
    
    # Check git status
    if [ -n "$(git status --porcelain)" ]; then
        echo -e "${RED}❌ Working directory not clean${NC}"
        issues=$((issues + 1))
    else
        echo -e "${GREEN}✅ Working directory clean${NC}"
    fi
    
    # Check if we're on main/master branch
    local current_branch
    current_branch=$(git branch --show-current)
    if [[ "$current_branch" != "main" && "$current_branch" != "master" ]]; then
        echo -e "${YELLOW}⚠️  Not on main/master branch (current: $current_branch)${NC}"
    else
        echo -e "${GREEN}✅ On release branch${NC}"
    fi
    
    # Check build
    echo "🏗️  Testing build..."
    if ./gradlew :app:assembleRelease --quiet; then
        echo -e "${GREEN}✅ Release build successful${NC}"
    else
        echo -e "${RED}❌ Release build failed${NC}"
        issues=$((issues + 1))
    fi
    
    # Check tests
    echo "🧪 Running tests..."
    if ./gradlew testReleaseUnitTest --quiet; then
        echo -e "${GREEN}✅ Tests passed${NC}"
    else
        echo -e "${RED}❌ Tests failed${NC}"
        issues=$((issues + 1))
    fi
    
    # Check for required files
    local required_files=("README.md" "CHANGELOG.md" "build.gradle.kts")
    for file in "${required_files[@]}"; do
        if [ -f "$file" ]; then
            echo -e "${GREEN}✅ $file exists${NC}"
        else
            echo -e "${RED}❌ $file missing${NC}"
            issues=$((issues + 1))
        fi
    done
    
    echo ""
    if [ $issues -eq 0 ]; then
        echo -e "${GREEN}🎉 Release validation passed!${NC}"
        return 0
    else
        echo -e "${RED}❌ Release validation failed with $issues issues${NC}"
        return 1
    fi
}

prepare_release() {
    local version="$1"
    
    if [ -z "$version" ]; then
        echo -e "${RED}❌ Version required for release preparation${NC}"
        return 1
    fi
    
    echo -e "${PURPLE}🚀 Preparing release v$version${NC}"
    echo ""
    
    # Validate first
    if ! validate_release_readiness; then
        echo -e "${RED}❌ Release validation failed. Fix issues before preparing release.${NC}"
        return 1
    fi
    
    # Set version
    set_version "$version"
    
    # Generate changelog
    generate_changelog_entry "$version"
    
    # Commit changes
    git add "$GRADLE_FILE" "$CHANGELOG_FILE"
    git commit -m "chore: prepare release v$version"
    
    echo -e "${GREEN}✅ Release v$version prepared${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Review the changes"
    echo "  2. Run: $0 create $version"
    echo "  3. Push changes and tag"
}

create_release() {
    local version="$1"
    
    if [ -z "$version" ]; then
        echo -e "${RED}❌ Version required for release creation${NC}"
        return 1
    fi
    
    echo -e "${PURPLE}🏷️  Creating release v$version${NC}"
    echo ""
    
    # Create release builds
    echo "🏗️  Building release artifacts..."
    ./gradlew clean :app:assembleRelease --quiet
    
    # Create tag
    local tag_name="v$version"
    local release_notes
    
    # Extract release notes from changelog
    if [ -f "$CHANGELOG_FILE" ]; then
        release_notes=$(awk "/## \[$version\]/{flag=1; next} /## \[/{flag=0} flag" "$CHANGELOG_FILE" | head -20)
    else
        release_notes="Release v$version"
    fi
    
    git tag -a "$tag_name" -m "Release v$version

$release_notes"
    
    echo -e "${GREEN}✅ Tag $tag_name created${NC}"
    
    # Show release artifacts
    echo ""
    echo -e "${CYAN}📦 Release artifacts:${NC}"
    find . -name "*.apk" -path "*/release/*" | head -5 | sed 's/^/  • /'
    
    echo ""
    echo "Next steps:"
    echo "  1. Push changes: git push origin main"
    echo "  2. Push tag: git push origin $tag_name"
    echo "  3. Create GitHub release from tag"
    echo "  4. Upload APK artifacts to release"
}

# Main command handling
case "${1:-help}" in
    "current")
        echo -e "${CYAN}Current version: $(get_current_version)${NC}"
        ;;
    "bump")
        if [ -z "$2" ]; then
            echo -e "${RED}❌ Bump type required (major, minor, patch)${NC}"
            exit 1
        fi
        new_version=$(bump_version "$2")
        echo -e "${GREEN}🎉 Version bumped to $new_version${NC}"
        ;;
    "prepare")
        prepare_release "$2"
        ;;
    "create")
        create_release "$2"
        ;;
    "changelog")
        version=$(get_current_version)
        generate_changelog_entry "$version"
        ;;
    "validate")
        validate_release_readiness
        ;;
    "help"|*)
        show_help
        ;;
esac