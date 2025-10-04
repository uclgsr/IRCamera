# Quick Start Guide: Thesis Diagrams

## For Immediate Viewing

All diagrams render natively on GitHub. Simply open any `.md` file:

- [Figure 1.1: Multi-Sensor System Overview](chapter1-multisensor-overview.md)
- [Figure 1.2: Use-Case Scenario Timeline](chapter1-usecase-timeline.md)
- [Table 2.1: Hardware Specifications](chapter2-hardware-specs-table.md)
- [Table 2.2: Related Systems Comparison](chapter2-related-systems-comparison.md)
- [Figure 2.1: GSR and Thermal Data Examples](chapter2-gsr-thermal-examples.md)

## For LaTeX Compilation

### Quick Method (Copy-Paste)

1. Open any diagram file
2. Copy the Mermaid code block (starts with ` ```mermaid `)
3. Go to https://mermaid.live/
4. Paste the code
5. Click "Export" → "PNG" or "SVG"
6. Save to `docs/latex/figures/`
7. Update LaTeX file:
   ```latex
   \includegraphics[width=\textwidth]{figures/your-image.png}
   ```

### Automated Method (Command Line)

```bash
# Install mermaid-cli once
npm install -g @mermaid-js/mermaid-cli

# Navigate to diagrams directory
cd docs/thesis-diagrams

# Convert all Chapter 1 diagrams
mmdc -i chapter1-multisensor-overview.md -o ../latex/figures/fig1-1.png -w 1200
mmdc -i chapter1-usecase-timeline.md -o ../latex/figures/fig1-2.png -w 1200

# Convert Chapter 2 diagrams
mmdc -i chapter2-gsr-thermal-examples.md -o ../latex/figures/fig2-1.png -w 1200

# Compile LaTeX
cd ../latex
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
```

## What's Included

### Chapter 1: Introduction
- **System Architecture**: Complete overview showing PC, Android, sensors, network
- **Use-Case Timeline**: Step-by-step recording session workflow

### Chapter 2: Background
- **Hardware Specs**: Detailed specifications table for all components
- **Systems Comparison**: How this work compares to PhysioKit, FLIR, etc.
- **Data Examples**: Visual representation of GSR signals and thermal images

## File Sizes

- Each Mermaid source: 4-8 KB (text)
- Exported PNG (1200px width): ~100-300 KB each
- Exported SVG: ~50-150 KB each

## Quality Settings

For best LaTeX output:
- Width: 1200-1600 pixels
- Format: PNG or SVG (vector preferred)
- DPI: 300 for print quality

## Need Help?

- **Full documentation**: [README.md](README.md)
- **Quick reference**: [INDEX.md](INDEX.md)
- **Implementation details**: [../THESIS_FIGURES_IMPLEMENTATION.md](../THESIS_FIGURES_IMPLEMENTATION.md)

## LaTeX Integration Status

✅ References added to chapters 1 and 2
✅ Labels defined for cross-referencing
✅ Appendix Z updated with descriptions
⏳ Pending: Image conversion (use methods above)

## Troubleshooting

**Mermaid syntax error?**
- Test on https://mermaid.live/
- Check for missing backticks or braces
- Verify "mermaid" language tag

**LaTeX compilation error?**
- Ensure `figures/` directory exists
- Check image file paths are correct
- Verify `graphicx` package is loaded

**Image quality issues?**
- Increase export width (1600px+)
- Use vector format (SVG) when possible
- Set higher DPI in LaTeX (300 or 600)

## Tips

1. **Preview First**: Always check on mermaid.live before exporting
2. **Batch Export**: Use mmdc CLI for multiple files at once
3. **Version Control**: Keep Mermaid source files, regenerate images as needed
4. **Consistency**: Use same width/format for all diagrams in thesis
5. **Backup**: Save high-res versions (SVG) for future modifications

---

**Last Updated**: 2024-10-04  
**Status**: All diagrams complete and ready for export
