"""앱 소스에 들어 있는 한글 문자열을 JSON으로 뽑는다.

- res/values/strings.xml: 이름 → 문자열
- Kotlin 소스: 파일별로 줄 번호와 문자열. 주석은 건너뛰고, "a" + "b"처럼 이어 붙인 문자열은 하나로 합친다.
  문자열 템플릿(${...}, $name)은 원문 그대로 둔다.
사용법: python extract_strings.py <프로젝트 루트> <출력 json>
"""
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

HANGUL = re.compile(r"[가-힣]")


def parse_template(src, i):
    """src[i]는 '${'의 '{' 다음. 짝이 맞는 '}' 다음 위치를 돌려준다 (안쪽 문자열도 건너뛴다)."""
    depth = 1
    while i < len(src) and depth:
        c = src[i]
        if src.startswith('"""', i):
            _, i = parse_string(src, i)
            continue
        if c == '"':
            _, i = parse_string(src, i)
            continue
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
        i += 1
    return i


def parse_string(src, i):
    """src[i]에서 시작하는 문자열 리터럴을 읽어 (내용, 끝 다음 위치)를 돌려준다."""
    if src.startswith('"""', i):
        end = src.index('"""', i + 3)
        while src.startswith('"', end + 3):  # """...""""" 처럼 끝에 따옴표가 더 붙은 경우
            end += 1
        return src[i + 3:end], end + 3
    out = []
    i += 1
    while i < len(src):
        c = src[i]
        if c == "\\":
            nxt = src[i + 1]
            out.append({"n": "\n", "t": "\t", '"': '"', "\\": "\\", "$": "$", "'": "'"}.get(nxt, "\\" + nxt))
            i += 2
        elif c == '"':
            return "".join(out), i + 1
        elif src.startswith("${", i):
            end = parse_template(src, i + 2)
            out.append(src[i:end])
            i = end
        else:
            out.append(c)
            i += 1
    raise ValueError("닫히지 않은 문자열")


def kotlin_strings(src):
    """(줄 번호, 문자열) 목록. 주석과 문자 리터럴은 건너뛴다."""
    found = []
    i = 0
    while i < len(src):
        if src.startswith("//", i):
            i = src.find("\n", i)
            i = len(src) if i < 0 else i
        elif src.startswith("/*", i):
            i = src.index("*/", i + 2) + 2
        elif src[i] == "'":
            # 문자 리터럴: 'a', '\n', '\''
            i = src.index("'", i + 2 if src[i + 1] == "\\" else i + 1) + 1
        elif src[i] == '"':
            line = src.count("\n", 0, i) + 1
            text, i = parse_string(src, i)
            # 바로 뒤에 + "..." 가 이어지면 하나로 합친다
            while True:
                m = re.compile(r'\s*\+\s*(?=")').match(src, i)
                if not m:
                    break
                more, i = parse_string(src, m.end())
                text += more
            found.append((line, text))
        else:
            i += 1
    return found


def main(root, out_path):
    root = Path(root)
    main_dir = root / "app" / "src" / "main"
    result = {"resources": {}, "kotlin": {}}

    for el in ET.parse(main_dir / "res" / "values" / "strings.xml").getroot():
        if el.tag == "string":
            result["resources"][el.get("name")] = "".join(el.itertext())

    total = 0
    for path in sorted((main_dir / "java").rglob("*.kt")):
        src = path.read_text(encoding="utf-8")
        items = [{"line": line, "text": text} for line, text in kotlin_strings(src) if HANGUL.search(text)]
        if items:
            rel = path.relative_to(main_dir / "java" / "com" / "getfuckocus").as_posix()
            result["kotlin"][rel] = items
            total += len(items)

    result = {
        "summary": {
            "resources": len(result["resources"]),
            "kotlin_files": len(result["kotlin"]),
            "kotlin_strings": total,
        },
        **result,
    }
    Path(out_path).write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result["summary"], ensure_ascii=False))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
