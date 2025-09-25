#!/bin/bash
# Installation script for Enhanced PC Session Controller

echo "Installing Enhanced PC Session Controller..."
echo "============================================"

# Check for Python 3
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is required but not installed"
    exit 1
fi

echo "✅ Python 3 found: $(python3 --version)"

# Install Python dependencies
echo "📦 Installing Python dependencies..."
pip3 install --user PyQt6 pyqtgraph numpy matplotlib pillow pybind11

# Check for build tools
if ! command -v gcc &> /dev/null; then
    echo "⚠️  GCC not found. Installing build tools..."
    sudo apt-get update
    sudo apt-get install -y build-essential cmake
fi

# Build C++ backend
echo "🔨 Building C++ backend..."
cd "$(dirname "$0")/../pc-controller/enhanced_native_backend"

if [ -f "setup.py" ]; then
    python3 setup.py build_ext --inplace
    if [ $? -eq 0 ]; then
        echo "✅ C++ backend built successfully"
    else
        echo "⚠️  C++ backend build failed (controller will work without it)"
    fi
else
    echo "⚠️  C++ backend setup.py not found"
fi

# Create desktop shortcut
echo "🖥️  Creating desktop shortcut..."
cd "$(dirname "$0")"

cat > ~/Desktop/IRCamera_Enhanced_Controller.desktop << EOD
[Desktop Entry]
Version=1.0
Type=Application
Name=IRCamera Enhanced PC Controller
Comment=Enhanced PC Session Controller for IRCamera multi-modal recording
Exec=cd "$(pwd)/src" && python3 enhanced_pc_controller.py
Icon=applications-multimedia
Terminal=false
Categories=Multimedia;Audio;Video;
EOD

chmod +x ~/Desktop/IRCamera_Enhanced_Controller.desktop

echo "✅ Installation complete!"
echo ""
echo "Usage:"
echo "  cd $(pwd)/src"
echo "  python3 enhanced_pc_controller.py"
echo ""
echo "Or use the desktop shortcut: ~/Desktop/IRCamera_Enhanced_Controller.desktop"
echo ""
echo "For troubleshooting, see README_ENHANCED.md"
