# Development Backlog

## Completed Items

### Code Quality & Compilation
- [x] Fix Kotlin compilation warnings in libapp module
  - [x] GuideInterface.kt: Fixed String nullability issues
  - [x] RingBuffer.kt: Fixed ByteArray null safety in System.arraycopy
  - [x] UsbBuffer.kt: Removed redundant null checks
  - [x] ByteUtils.kt: Added @OptIn for experimental unsigned types
  - [x] FileUtils.kt: Added null safety for file operations
  - [x] Applied fixes to com.mpdc4gsr package duplicates

## In Progress
- [ ] TBD

## Backlog Items

### High Priority
- [ ] TBD

### Medium Priority  
- [ ] TBD

### Low Priority
- [ ] TBD

## Technical Debt
- [ ] Continue monitoring for additional Kotlin compilation warnings
- [ ] Consider consolidating duplicate code between matrix and mpdc4gsr packages
- [ ] Evaluate removal of deprecated API usage warnings