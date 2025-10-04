#!/bin/bash
# Example usage of thesis evaluation tests

# Replace with your Android device IP address
DEVICE_IP="192.168.1.100"

echo "=========================================="
echo "Thesis Evaluation Tests - Example Usage"
echo "=========================================="
echo ""
echo "Device IP: $DEVICE_IP"
echo ""

# Check if device IP is provided as argument
if [ $# -eq 1 ]; then
    DEVICE_IP=$1
    echo "Using device IP from argument: $DEVICE_IP"
fi

echo ""
echo "Choose a test to run:"
echo "  1) Test 1: Remote Start/Stop Command Test"
echo "  2) Test 2: Command Latency and Throughput Test"
echo "  3) Test 3: Edge-case Command Handling Test"
echo "  4) Test 4: Multi-command Sequence Automation Test"
echo "  5) Run all tests"
echo "  q) Quit"
echo ""
read -p "Enter your choice: " choice

case $choice in
    1)
        echo ""
        echo "Running Test 1: Remote Start/Stop Command Test"
        echo "----------------------------------------------"
        python3 test_1_remote_start_stop.py --device-ip $DEVICE_IP --duration 10
        ;;
    2)
        echo ""
        echo "Running Test 2: Command Latency and Throughput Test"
        echo "---------------------------------------------------"
        python3 test_2_command_latency_throughput.py --device-ip $DEVICE_IP --iterations 10
        ;;
    3)
        echo ""
        echo "Running Test 3: Edge-case Command Handling Test"
        echo "-----------------------------------------------"
        python3 test_3_edge_case_commands.py --device-ip $DEVICE_IP
        ;;
    4)
        echo ""
        echo "Running Test 4: Multi-command Sequence Automation Test"
        echo "------------------------------------------------------"
        python3 test_4_multi_command_sequence.py --device-ip $DEVICE_IP --scenario all
        ;;
    5)
        echo ""
        echo "Running All Tests"
        echo "-----------------"
        python3 run_all_tests.py --device-ip $DEVICE_IP
        ;;
    q|Q)
        echo "Exiting..."
        exit 0
        ;;
    *)
        echo "Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "Test execution completed!"
echo "Check the generated report files in the current directory."
