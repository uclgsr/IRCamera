#!/usr/bin/env python3
# kotlin_balance_check.py
from __future__ import annotations
import argparse, json, os, fnmatch
from pathlib import Path
from typing import Dict, List, Tuple

OPENERS = {'(' : ')', '[' : ']', '{' : '}'}
CLOSERS = {')' : '(', ']' : '[', '}' : '{'}

def _is_ident_char(ch: str) -> bool:
    return ch == '_' or ch.isalnum()

def _skip_kotlin_template_expr(text: str, i: int, line: int, col: int) -> Tuple[int,int,int]:
    """
    Called with i at position just after '${' (i points to first char after '{').
    Scans until the matching '}' accounting for nested braces, strings, and comments.
    Returns new (i, line, col) positioned just AFTER the matching '}'.
    """
    n = len(text)
    depth = 1
    in_line_comment = False
    block_comment_depth = 0
    s = None  # 'normal' | 'raw' | 'char'
    escape = False

    while i < n:
        ch = text[i]
        if ch == '\n':
            line += 1; col = 0; in_line_comment = False; i += 1; continue
        else:
            col += 1

        # inside block comment
        if block_comment_depth > 0:
            if ch == '/' and i + 1 < n and text[i+1] == '*':
                block_comment_depth += 1; i += 2; col += 1; continue
            if ch == '*' and i + 1 < n and text[i+1] == '/':
                block_comment_depth -= 1; i += 2; col += 1; continue
            i += 1; continue

        if in_line_comment:
            i += 1; continue

        # string states within template code
        if s == 'raw':
            # allow templates inside raw strings too
            if ch == '$':
                if i + 1 < n and text[i+1] == '{':
                    i, line, col = _skip_kotlin_template_expr(text, i + 2, line, col + 1); continue
                else:
                    # $name
                    j = i + 1
                    while j < n and _is_ident_char(text[j]): j += 1
                    col += (j - i - 1); i = j; continue
            if ch == '"' and i + 2 < n and text[i+1] == '"' and text[i+2] == '"':
                s = None; i += 3; col += 2; continue
            i += 1; continue

        if s == 'normal':
            if escape: escape = False; i += 1; continue
            if ch == '\\': escape = True; i += 1; continue
            if ch == '$':
                if i + 1 < n and text[i+1] == '{':
                    i, line, col = _skip_kotlin_template_expr(text, i + 2, line, col + 1); continue
                else:
                    j = i + 1
                    while j < n and _is_ident_char(text[j]): j += 1
                    col += (j - i - 1); i = j; continue
            if ch == '"': s = None; i += 1; continue
            i += 1; continue

        if s == 'char':
            if escape: escape = False; i += 1; continue
            if ch == '\\': escape = True; i += 1; continue
            if ch == '\'': s = None; i += 1; continue
            i += 1; continue

        # not in string/comment
        if ch == '/' and i + 1 < n:
            if text[i+1] == '/': in_line_comment = True; i += 2; col += 1; continue
            if text[i+1] == '*': block_comment_depth += 1; i += 2; col += 1; continue

        if ch == '"' and i + 2 < n and text[i+1] == '"' and text[i+2] == '"':
            s = 'raw'; i += 3; col += 2; continue
        if ch == '"': s = 'normal'; i += 1; continue
        if ch == '\'': s = 'char'; i += 1; continue

        if ch == '{': depth += 1; i += 1; continue
        if ch == '}':
            depth -= 1; i += 1
            if depth == 0: return i, line, col
            continue

        i += 1

    # unterminated; return best-effort
    return i, line, col

def scan_text(text: str, file_path: str) -> List[Dict]:
    issues: List[Dict] = []
    stack: List[Tuple[str, int, int]] = []
    line = 1; col = 0; i = 0; n = len(text)
    in_line_comment = False; block_comment_depth = 0
    in_string = None  # 'normal', 'raw', 'char'

    lines = text.splitlines()
    def ctx(ln: int) -> str:
        return lines[ln-1][:200] if 1 <= ln <= len(lines) else ''

    def report(kind, msg, line_no, col_no, expected=None, found=None, open_line=None, open_col=None):
        issues.append({
            'file': file_path, 'kind': kind, 'message': msg,
            'line': line_no, 'col': col_no,
            'expected': expected, 'found': found,
            'open_line': open_line, 'open_col': open_col,
            'context': ctx(line_no),
        })

    while i < n:
        ch = text[i]

        if ch == '\n':
            line += 1; col = 0; in_line_comment = False; i += 1; continue
        else:
            col += 1

        # block comments
        if block_comment_depth > 0:
            if ch == '/' and i + 1 < n and text[i+1] == '*':
                block_comment_depth += 1; i += 2; col += 1; continue
            if ch == '*' and i + 1 < n and text[i+1] == '/':
                block_comment_depth -= 1; i += 2; col += 1; continue
            i += 1; continue

        if in_line_comment:
            i += 1; continue

        # strings
        if in_string == 'raw':
            if ch == '$':
                if i + 1 < n and text[i+1] == '{':
                    i, line, col = _skip_kotlin_template_expr(text, i + 2, line, col + 1); continue
                else:
                    j = i + 1
                    while j < n and _is_ident_char(text[j]): j += 1
                    col += (j - i - 1); i = j; continue
            if ch == '"' and i + 2 < n and text[i+1] == '"' and text[i+2] == '"':
                in_string = None; i += 3; col += 2; continue
            i += 1; continue

        if in_string == 'normal':
            if ch == '\\':
                i += 1;  # consume escape
                if i < n: col += 1; i += 1
                continue
            if ch == '$':
                if i + 1 < n and text[i+1] == '{':
                    i, line, col = _skip_kotlin_template_expr(text, i + 2, line, col + 1); continue
                else:
                    j = i + 1
                    while j < n and _is_ident_char(text[j]): j += 1
                    col += (j - i - 1); i = j; continue
            if ch == '"':
                in_string = None; i += 1; continue
            i += 1; continue

        if in_string == 'char':
            if ch == '\\':
                i += 1
                if i < n: col += 1; i += 1
                continue
            if ch == '\'':
                in_string = None; i += 1; continue
            i += 1; continue

        # comment starts
        if ch == '/' and i + 1 < n:
            nxt = text[i+1]
            if nxt == '/': in_line_comment = True; i += 2; col += 1; continue
            if nxt == '*': block_comment_depth += 1; i += 2; col += 1; continue

        # string starts
        if ch == '"' and i + 2 < n and text[i+1] == '"' and text[i+2] == '"':
            in_string = 'raw'; i += 3; col += 2; continue
        if ch == '"':
            in_string = 'normal'; i += 1; continue
        if ch == '\'':
            in_string = 'char'; i += 1; continue

        # structural tokens
        if ch in OPENERS:
            stack.append((ch, line, col)); i += 1; continue

        if ch in CLOSERS:
            if not stack:
                report('unexpected_closer', f'unexpected {ch}', line, col, found=ch)
            else:
                open_ch, open_line, open_col = stack.pop()
                if CLOSERS[ch] != open_ch:
                    report('mismatch', f'mismatched {ch} closes {open_ch}', line, col,
                           expected=OPENERS[open_ch], found=ch, open_line=open_line, open_col=open_col)
            i += 1; continue

        i += 1

    # EOF checks
    if in_string:    report('unterminated_string', f'unterminated {in_string} string', line, col)
    if block_comment_depth > 0: report('unterminated_comment', 'unterminated block comment', line, col)
    while stack:
        open_ch, open_line, open_col = stack.pop()
        report('unclosed', f'unclosed {open_ch}', open_line, open_col, expected=OPENERS[open_ch])

    return issues

def scan_file(path: Path) -> List[Dict]:
    try:
        text = path.read_text(encoding='utf-8', errors='replace')
    except Exception as e:
        return [{'file': str(path), 'kind':'read_error','message': f'failed to read file: {e}',
                 'line':0,'col':0,'expected':None,'found':None,'open_line':None,'open_col':None,'context':None}]
    return scan_text(text, file_path=str(path))

def scan_repo(root: Path,
              extensions=('.kt', '.kts'),
              exclude_dirs=('build', '.gradle', '.git', '.idea', 'out', '.mvn', '.venv'),
              include_globs: List[str] | None = None,
              exclude_globs: List[str] | None = None) -> List[Dict]:
    issues: List[Dict] = []
    for dirpath, dirnames, filenames in os.walk(root, topdown=True):
        dirnames[:] = [d for d in dirnames if d not in exclude_dirs]
        for fn in filenames:
            if not fn.lower().endswith(extensions): continue
            path = Path(dirpath) / fn
            rel = str(path.relative_to(root)).replace(os.sep, '/')
            if include_globs and not any(fnmatch.fnmatch(rel, g) for g in include_globs): continue
            if exclude_globs and any(fnmatch.fnmatch(rel, g) for g in exclude_globs): continue
            issues.extend(scan_file(path))
    return issues

def main():
    ap = argparse.ArgumentParser(description='Detect unbalanced (), [], {} and unterminated strings/comments in Kotlin sources.')
    ap.add_argument('path', nargs='?', default='.', help='repository root')
    ap.add_argument('--exclude', nargs='*', default=['build','.gradle','.git','.idea','out','.mvn','.venv'])
    ap.add_argument('--extensions', nargs='*', default=['.kt','.kts'])
    ap.add_argument('--include-glob', nargs='*', default=None, help='only scan files matching these globs (relative to root)')
    ap.add_argument('--exclude-glob', nargs='*', default=None, help='skip files matching these globs (relative to root)')
    ap.add_argument('--json', dest='json_out', default=None)
    ap.add_argument('--show-ok', action='store_true')
    ap.add_argument('--exit-zero', action='store_true')
    args = ap.parse_args()

    root = Path(args.path).resolve()
    exts = tuple(e if e.startswith('.') else '.'+e for e in args.extensions)
    issues = scan_repo(root, extensions=exts,
                       exclude_dirs=tuple(args.exclude),
                       include_globs=args.include_glob,
                       exclude_globs=args.exclude_glob)

    by_file: Dict[str, List[Dict]] = {}
    for it in issues: by_file.setdefault(it['file'], []).append(it)

    # stats
    total_scanned = sum(1 for _ in (p for p in root.rglob('*') if p.suffix.lower() in exts))
    print(f'Root: {root}')
    print(f'Scanned Kotlin files: {len(by_file)} of ~{total_scanned}')
    print(f'Issues found: {sum(len(v) for v in by_file.values())}\n')

    for file in sorted(by_file.keys()):
        items = by_file[file]
        if not items and not args.show_ok: continue
        if items:
            print(file)
            for it in items:
                loc = f"{it['line']}:{it['col']}"
                print(f"  {loc:<7} {it['kind']:<22} {it['message']}")
                if it.get('context'): print(f"    {it['context']}")
            print('')
        elif args.show_ok:
            print(f'{file}\n  OK\n')

    if args.json_out:
        with open(args.json_out,'w',encoding='utf-8') as f: json.dump(issues, f, ensure_ascii=False, indent=2)
        print(f'Wrote JSON report to {args.json_out}')

    raise SystemExit(0 if (args.exit_zero or not any(by_file.values())) else 1)

if __name__ == '__main__':
    main()
