#!/bin/bash



ANDROID_IP=""
PORT=8080
DURATION=30


while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            echo "Usage: $0 <android_ip> [--port PORT] [--duration SECONDS]"
            echo ""
            echo "Connect to Android device for live preview streaming"
            echo ""
            echo "Arguments:"
            echo "  android_ip          IP address of Android device"
            echo "  --port PORT         Server port (default: 8080)"
            echo "  --duration SECONDS  Test duration (default: 30)"
            echo ""
            echo "Example:"
            echo "  $0 192.168.1.100"
            echo "  $0 192.168.1.100 --port 8080 --duration 60"
            exit 0
            ;;
        --port)
            PORT="$2"
            shift 2
            ;;
        --duration)
            DURATION="$2"
            shift 2
            ;;
        *)
            if [[ -z "$ANDROID_IP" ]]; then
                ANDROID_IP="$1"
            else
                echo "Unknown argument: $1"
                exit 1
            fi
            shift
            ;;
    esac
done


if [[ -z "$ANDROID_IP" ]]; then
    echo "Error: Android IP address is required"
    echo "Usage: $0 <android_ip> [--port PORT] [--duration SECONDS]"
    exit 1
fi


if [[ ! "$ANDROID_IP" =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
    echo "Warning: '$ANDROID_IP' doesn't look like a valid IP address"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "Connecting to Android device at $ANDROID_IP:$PORT"
echo "Test duration: ${DURATION} seconds"
echo "Press Ctrl+C to stop early"
echo ""


SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"


python3 "$SCRIPT_DIR/test_android_preview_client.py" "$ANDROID_IP" --port "$PORT" --duration "$DURATION"

echo ""
echo "Connection test completed."