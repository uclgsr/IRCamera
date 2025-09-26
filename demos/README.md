# IRCamera Demo Scripts

This directory contains demonstration scripts showing various aspects of the IRCamera platform.

## Available Demos

### Network and Protocol Demos

- **demo_networking.py** - PC-orchestrated multi-modal recording demonstration
- **demo_pc_controller.py** - Standardized networking protocol implementation demo  
- **demo_thermal_integration.py** - PC-Android command interface for thermal camera integration

## Usage

All demo scripts are standalone and can be run directly:

```bash
python3 demo_networking.py
python3 demo_pc_controller.py
python3 demo_thermal_integration.py
```

## Requirements

Demo scripts may require additional dependencies. Install them with:

```bash
pip install -r ../pc-controller/requirements.txt
```

## Note

These are demonstration scripts for understanding platform capabilities. For production use, see the main PC controller application in the `pc-controller/` directory.