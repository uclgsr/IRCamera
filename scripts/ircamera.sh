#!/bin/bash

# IRCamera Master Script
# Main entry point for all IRCamera scripts

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

usage() {
    echo "IRCamera Command Line Interface"
    echo "================================"
    echo ""
    echo "Usage: $0 <command> [options]"
    echo ""
    echo "Commands:"
    echo "  test [type]         Run tests (accessibility, integration, performance, comprehensive, validate, all)"
    echo "  verify [type]       Verify build/migration (build, dependencies, migration, all)"
    echo "  connect <ip>        Connect to Android device for live streaming"
    echo "  help                Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 test all                    Run all tests"
    echo "  $0 test integration            Run integration tests only"
    echo "  $0 verify build                Verify build fixes"
    echo "  $0 connect 192.168.1.100       Connect to Android device"
    echo ""
    exit 0
}

# Check if no arguments provided
if [ $# -eq 0 ]; then
    usage
fi

COMMAND="$1"
shift

case "$COMMAND" in
    test)
        "$SCRIPT_DIR/test.sh" "$@"
        ;;
    verify)
        "$SCRIPT_DIR/verify.sh" "$@"
        ;;
    connect)
        "$SCRIPT_DIR/connect.sh" "$@"
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        echo "Unknown command: $COMMAND"
        echo ""
        usage
        ;;
esac
