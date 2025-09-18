# Over-engineered Components to Remove for MVP Focus

## Files to Remove (Not Needed for MVP):

1. **`enhanced_tcp_server.py`** (499 lines, 25 classes/functions)
   - Over-engineered with TLS, SSL, complex error handling
   - asyncio complexity not needed for MVP
   - Dataclasses and advanced features

2. **`realtime_visualization.py`** (572 lines, 31 classes/functions) 
   - Complex PyQtGraph visualization
   - Real-time plotting with multiple data types
   - Advanced GUI components not needed for basic MVP

3. **`security_manager.py`** (190 lines, 7 classes/functions)
   - TLS certificate generation
   - Authentication system 
   - Security features beyond MVP scope

4. **`headless_demo.py`** (438 lines, 17 classes/functions)
   - Duplicate functionality
   - Complex demonstration features

5. **Complex Native Backend Integration**
   - C++ PyBind11 complexity
   - Advanced performance optimization
   - Native GSR processing (can be Python for MVP)

## MVP Core Requirements:

✅ **Keep**:
- Basic TCP server (simple socket handling)
- Device registration and data reception
- Simple session management
- Basic logging
- Command-line interface

❌ **Remove**:
- TLS/SSL security layers
- Complex real-time visualization
- Advanced GUI components
- Native C++ backend
- Certificate management
- Complex error handling and recovery
- Advanced networking features
- Async/await patterns
- Dataclasses and type annotations complexity
- Multiple inheritance patterns

## Recommended Structure:

```
pc-controller/
├── mvp_pc_controller.py     # Single MVP application (~300 lines)
├── test_mvp.py              # Simple tests
├── README.md                # Basic usage
└── config.yaml              # Simple configuration
```

The current implementation has ~2000+ lines across multiple files when MVP needs ~300 lines in a single file.