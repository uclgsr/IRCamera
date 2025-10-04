# Chapter 6 Evaluation Framework - Usage Examples

This document provides practical examples of using the requirements evaluation framework.

## Quick Start

### Generate All Chapter 6 Materials

```bash
cd /home/runner/work/IRCamera/IRCamera
python3 docs/chapter6/requirements_evaluation.py
```

This generates all evaluation materials in `docs/chapter6/`:
- 3 Mermaid diagram files (.md)
- 4 CSV data files (.csv)
- 7 analysis and report files (.md)
- 1 comprehensive table file (.md)

## Viewing Mermaid Diagrams

### On GitHub

Mermaid diagrams render automatically in GitHub markdown preview:

1. Navigate to `docs/chapter6/objectives_fulfillment_diagram.md`
2. The diagram renders automatically in the GitHub UI
3. Color coding shows achievement status at a glance

### In VS Code

Install the "Markdown Preview Mermaid Support" extension:

```bash
code --install-extension bierner.markdown-mermaid
```

Then open any `.md` file with Mermaid diagrams and use Markdown preview.

### Converting to Images

For LaTeX integration, convert Mermaid to PNG/SVG:

```bash
# Install mermaid-cli
npm install -g @mermaid-js/mermaid-cli

# Convert objectives diagram to PNG
mmdc -i docs/chapter6/objectives_fulfillment_diagram.md -o figures/objectives.png

# Convert requirements diagram to SVG
mmdc -i docs/chapter6/requirements_status_diagram.md -o figures/requirements.svg

# Convert roadmap to PNG
mmdc -i docs/chapter6/future_work_roadmap.md -o figures/roadmap.png
```

## Data Analysis Examples

### Python - Analyzing Objectives

```python
import pandas as pd

# Load objectives fulfillment data
objectives = pd.read_csv('docs/chapter6/objectives_fulfillment_table.csv')

# Count achievements by status
status_counts = objectives['status'].value_counts()
print("Objectives Status Distribution:")
print(status_counts)

# Calculate achievement rate
achieved = len(objectives[objectives['status'].isin(['Achieved', 'Exceeded'])])
total = len(objectives)
rate = (achieved / total) * 100
print(f"\nAchievement Rate: {rate:.1f}%")

# Filter by status
exceeded = objectives[objectives['status'] == 'Exceeded']
print("\nObjectives that exceeded expectations:")
print(exceeded[['id', 'objective', 'outcome']])
```

### Python - Requirements Analysis

```python
import pandas as pd

# Load requirements evaluation data
requirements = pd.read_csv('docs/chapter6/requirements_vs_outcomes_table.csv')

# Group by category
by_category = requirements.groupby('category').size()
print("Requirements by Category:")
print(by_category)

# Group by priority
by_priority = requirements.groupby('priority').size()
print("\nRequirements by Priority:")
print(by_priority)

# Filter critical requirements
critical = requirements[requirements['priority'] == 'Critical']
print("\nCritical Requirements:")
print(critical[['Requirement ID', 'Requirement', 'Outcome']])
```

### Performance Metrics

```python
import pandas as pd

# Load performance comparison data
performance = pd.read_csv('docs/chapter6/performance_comparison.csv')

# Display metrics
print("Performance Metrics:")
print(performance[['Metric', 'Target', 'Achieved', 'Status']])

# Calculate percentage of targets met
met = len(performance[performance['Status'] == 'Met'])
total = len(performance)
print(f"\nTargets Met: {met}/{total} ({(met/total)*100:.1f}%)")
```

## LaTeX Integration

### Including Generated Tables

```latex
% In your thesis 6.tex file

\section{Evaluation Against Project Objectives}

\input{../docs/chapter6/objectives_latex_table.tex}

% Or reference the generated content
\begin{table}[htbp]
\centering
\caption{Fulfillment of Project Objectives}
\label{tab:objectives_fulfillment}
\csvreader[
    tabular=|l|p{3cm}|p{3cm}|p{4cm}|c|,
    table head=\hline ID & Objective & Target & Outcome & Status \\\hline,
    late after line=\\\hline
]{../docs/chapter6/objectives_fulfillment_table.csv}
{id=\id, objective=\objective, target=\target, outcome=\outcome, status=\status}
{\id & \objective & \target & \outcome & \status}
\end{table}
```

### Including Mermaid Diagrams as Figures

```latex
% First convert Mermaid to PNG using mmdc
% Then include in LaTeX

\begin{figure}[htbp]
\centering
\includegraphics[width=0.9\textwidth]{figures/objectives_fulfillment.png}
\caption{Visual representation of objectives vs outcomes showing achievement status with color-coded indicators.}
\label{fig:objectives_fulfillment}
\end{figure}
```

## Customizing the Evaluation

### Adding New Objectives

Edit `requirements_evaluation.py`:

```python
def generate_objectives_fulfillment_table(self):
    objectives = [
        # ... existing objectives ...
        {
            'id': 'OBJ-5',
            'objective': 'Your new objective',
            'target': 'Target description',
            'outcome': 'ACHIEVED/PARTIAL/NOT ACHIEVED - Details',
            'evidence': 'Evidence supporting the outcome',
            'status': 'Achieved'  # or Exceeded, Partial, Not Achieved
        }
    ]
    # ... rest of the method
```

### Adding New Requirements

Edit the `_define_project_requirements()` method:

```python
def _define_project_requirements(self) -> List[Dict[str, Any]]:
    return [
        # ... existing requirements ...
        {
            'req_id': 'REQ-011',
            'category': 'Functional',
            'requirement': 'Your requirement description',
            'priority': 'High',
            'success_criteria': 'Measurable success criteria',
            'measurement_method': 'How to measure',
            'target_value': 'Numeric target',
            'test_mapping': 'test_name'
        }
    ]
```

## Automated Updates

### Regeneration Script

Create a script to automatically regenerate on code changes:

```bash
#!/bin/bash
# regenerate_chapter6.sh

echo "Regenerating Chapter 6 evaluation materials..."
cd /path/to/IRCamera
python3 docs/chapter6/requirements_evaluation.py

if [ $? -eq 0 ]; then
    echo "Generation successful!"
    echo "Files updated in docs/chapter6/"
else
    echo "Generation failed!"
    exit 1
fi
```

### Integration with CI/CD

```yaml
# .github/workflows/generate-docs.yml
name: Generate Documentation

on:
  push:
    branches: [ dev, main ]
  pull_request:
    branches: [ dev, main ]

jobs:
  generate-chapter6:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.9'
      
      - name: Generate Chapter 6 materials
        run: |
          python3 docs/chapter6/requirements_evaluation.py
      
      - name: Check for changes
        run: |
          git diff --exit-code docs/chapter6/ || echo "Documentation updated"
```

## Tips and Best Practices

### 1. Regular Regeneration

Regenerate evaluation materials after significant milestones:
- After completing a major objective
- When test results change
- Before thesis submission
- For progress reviews

### 2. Version Control

All generated files are tracked in git, allowing you to:
- See historical changes in objectives status
- Compare achievement rates over time
- Document progress for supervisors

### 3. Presentation

Use the generated materials for:
- Thesis defense presentations (convert Mermaid to slides)
- Progress reports (CSV data in spreadsheets)
- Supervisor meetings (visual diagrams)
- Final thesis document (LaTeX tables and figures)

### 4. Validation

Before finalizing:
- Verify all CSV files open correctly in Excel/LibreOffice
- Check Mermaid diagrams render in GitHub preview
- Ensure color coding is visible in converted images
- Validate data accuracy against actual results

## Troubleshooting

### Mermaid Diagrams Not Rendering

**Problem**: Diagrams show as code blocks instead of visualizations

**Solutions**:
- Ensure viewing in GitHub (not raw view)
- Use VS Code with Mermaid extension
- Convert to images using mermaid-cli

### CSV Import Issues

**Problem**: CSV files don't import correctly in Excel

**Solutions**:
- Use UTF-8 encoding
- Check for special characters in data
- Use LibreOffice for better CSV handling

### LaTeX Integration Issues

**Problem**: Tables don't format correctly in LaTeX

**Solutions**:
- Use `csvsimple` package for CSV import
- Manually create LaTeX tables from CSV data
- Convert to images if tables are too complex

## Additional Resources

- Mermaid Documentation: https://mermaid.js.org/
- Mermaid Live Editor: https://mermaid.live/
- CSV to LaTeX Converter: https://www.tablesgenerator.com/
- GitHub Mermaid Support: https://github.blog/2022-02-14-include-diagrams-markdown-files-mermaid/
