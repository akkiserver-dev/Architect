"""Build Otogi's offline public handbook. Python 3 standard library only."""

from collections import Counter
from html import escape
from pathlib import Path
import json
import re


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/public"
RESOURCES = ROOT / "src/main/resources"
JAVA = ROOT / "src/main/java/jp/akkiserver/otogi/core/architect"
KINDS = {"park": ("式", "常時効果"), "passive": ("相", "自動発動"),
         "skill": ("術", "右クリック"), "tool": ("具", "武器")}
PAGES = {"guide.md": "index.html", "abilities.md": "abilities.html",
         "server-guide.md": "server-guide.html"}


def inline(text):
    """Render the limited inline syntax used by these checked-in manuscripts."""
    tokens = []

    def stash(value):
        tokens.append(value)
        return f"\x00{len(tokens) - 1}\x00"

    text = re.sub(r"`([^`]+)`", lambda m: stash(f"<code>{escape(m[1])}</code>"), text)

    def link(match):
        target = match[2]
        for source, dest in PAGES.items():
            if target == source or target.startswith(source + "#"):
                target = target.replace(source, dest, 1)
        # This renderer deliberately accepts no arbitrary HTML or active URLs.
        if re.match(r"(?:javascript|data|vbscript):", target, re.I):
            raise ValueError(f"Unsupported link: {target}")
        return stash(f'<a href="{escape(target, quote=True)}">{escape(match[1])}</a>')

    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", link, text)
    text = escape(text)
    text = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", text)
    return re.sub(r"\x00(\d+)\x00", lambda m: tokens[int(m[1])], text)


def markdown(source):
    """Small renderer for paragraphs, headings, flat lists, tables and code fences.

    Unsupported nested lists and raw HTML are not used by the manuscripts.
    Section IDs are stable for a given heading order and feed the page TOC.
    """
    lines = source.splitlines()
    output, toc = [], []
    i = 0
    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
            continue
        heading = re.match(r"^(#{1,3}) (.+)$", line)
        if heading:
            level, label = len(heading[1]), heading[2]
            if level > 1:
                anchor = f"section-{len(toc) + 1}"
                toc.append((level, label, anchor))
                output.append(f'<h{level} id="{anchor}">{inline(label)}</h{level}>')
            i += 1
        elif line.startswith("```"):
            block = []
            i += 1
            while i < len(lines) and not lines[i].startswith("```"):
                block.append(lines[i])
                i += 1
            if i == len(lines):
                raise ValueError("Unclosed code fence")
            output.append("<pre><code>" + escape("\n".join(block)) + "</code></pre>")
            i += 1
        elif line.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].startswith("|"):
                rows.append([cell.strip() for cell in lines[i].strip().strip("|").split("|")])
                i += 1
            if len(rows) < 2 or not all(re.fullmatch(r":?-+:?", cell) for cell in rows[1]):
                raise ValueError("Table requires a separator row")
            if any(len(row) != len(rows[0]) for row in rows):
                raise ValueError("Table columns differ")
            head = "".join(f'<th scope="col">{inline(cell)}</th>' for cell in rows[0])
            body = "".join("<tr>" + "".join(f"<td>{inline(cell)}</td>" for cell in row) + "</tr>"
                           for row in rows[2:])
            output.append('<div class="table-wrap" tabindex="0" role="region" aria-label="横にスクロールできる表">'
                          f"<table><thead><tr>{head}</tr></thead><tbody>{body}</tbody></table></div>")
        elif line.startswith("> "):
            block = []
            while i < len(lines) and lines[i].startswith("> "):
                block.append(lines[i][2:])
                i += 1
            output.append('<aside class="note">' + inline(" ".join(block)) + "</aside>")
        elif re.match(r"^(?:- |\d+\. )", line):
            ordered = bool(re.match(r"^\d+\. ", line))
            pattern = r"^\d+\. (.+)" if ordered else r"^- (.+)"
            tag = "ol" if ordered else "ul"
            items = []
            while i < len(lines) and (match := re.match(pattern, lines[i])):
                items.append("<li>" + inline(match[1]) + "</li>")
                i += 1
            output.append(f"<{tag}>" + "".join(items) + f"</{tag}>")
        else:
            block = [line]
            i += 1
            while i < len(lines) and lines[i].strip() and not re.match(r"^(?:#|\||>|```|- |\d+\. )", lines[i]):
                block.append(lines[i])
                i += 1
            output.append("<p>" + inline(" ".join(block)) + "</p>")
    return "\n".join(output), toc


def abilities():
    lang = json.loads((RESOURCES / "lang/ja_jp.json").read_text(encoding="utf-8"))
    result = []
    registry = (JAVA / "Architects.java").read_text(encoding="utf-8")
    registered = set(re.findall(r"new (\w+)\(\)", registry.split("public static void assign", 1)[0]))
    found = set()
    for kind in KINDS:
        for path in sorted((JAVA / kind).glob("*.java")):
            if path.stem not in registered:
                continue
            found.add(path.stem)
            match = re.search(r'super\("([a-z_]+)"', path.read_text(encoding="utf-8"))
            if not match:
                raise ValueError(f"Cannot find ability ID: {path}")
            ability_id = match[1]
            prefix = f"architect.{ability_id}.description."
            keys = sorted((key for key in lang if key.startswith(prefix)),
                          key=lambda key: int(key.rsplit(".", 1)[1]))
            result.append({"id": ability_id, "kind": kind, "name": lang[f"architect.{ability_id}.name"],
                           "description": [lang[key] for key in keys]})
    if found != registered:
        raise ValueError(f"Unresolved registered classes: {registered - found}")
    for path in sorted((RESOURCES / "architects").glob("*.json")):
        value = json.loads(path.read_text(encoding="utf-8"))
        if value.get("enabled") is False:
            continue
        name = lang[value["nameKey"]] if "nameKey" in value else value["name"]
        description = ([lang[key] for key in value["descriptionKeys"]]
                       if "descriptionKeys" in value else value["description"])
        result.append({"id": value["id"], "kind": value["kind"], "name": name,
                       "description": description})
    if len({value["id"] for value in result}) != len(result):
        raise ValueError("Duplicate ability IDs")
    for value in result:
        if value["kind"] not in KINDS or not value["description"]:
            raise ValueError(f"Incomplete ability: {value['id']}")
    return sorted(result, key=lambda value: (list(KINDS).index(value["kind"]), value["id"]))


def catalog(values):
    text = ["# 御伽の能力図鑑", "",
            f"同梱の全{len(values)}種を、式・相・術・具に分けて紹介します。説明はゲーム内の日本語原文です。",
            "数値は同梱設定を前提とした表示です。サーバー独自の調整や翻訳変更がある場合は、ゲーム内の説明を確認してください。",
            "", "CTは再使用までの待ち時間。2HP＝1ハートです。各項目のIDは術・具のお試し指定にも使えます。",
            "", "[遊び方へ戻る](guide.md)", ""]
    html = ['<p>同梱の全<strong>' + str(len(values)) + '種</strong>。ゲーム内と同じ日本語の説明を掲載しています。</p>',
            '<p>数値は同梱設定を前提とした表示です。サーバー独自の調整がある場合は、ゲーム内の説明を確認してください。CTは再使用までの待ち時間、2HP＝1ハートです。</p>',
            '<form class="catalog-tools" role="search" hidden><label for="ability-search">能力を探す</label>'
            '<input id="ability-search" type="search" placeholder="名前・ID・効果で検索" autocomplete="off">'
            '<label for="kind-filter">分類</label><select id="kind-filter"><option value="all">すべて</option>'
            + "".join(f'<option value="{kind}">{label[0]} / {label[1]}</option>' for kind, label in KINDS.items())
            + '</select><p id="catalog-count" role="status" aria-live="polite"></p></form>',
            '<p id="catalog-empty" hidden>見つかりませんでした。検索語や分類を変えてみてください。</p>']
    toc = []
    for kind, (label, action) in KINDS.items():
        group = [value for value in values if value["kind"] == kind]
        anchor = f"kind-{kind}"
        toc.append((2, f"{label} / {action}（{len(group)}）", anchor))
        text.extend([f"## {label} / {action}（{len(group)}種）", ""])
        html.append(f'<section class="ability-group" data-kind="{kind}" aria-labelledby="{anchor}">'
                    f'<h2 id="{anchor}">{label}<span> / {action}</span><small>{len(group)}種</small></h2>'
                    '<div class="ability-grid">')
        for value in group:
            ability_id = value["id"]
            name = value["name"]
            description = value["description"]
            text.extend([f"### {name} / `{ability_id}`", "", *[f"- {line}" for line in description], ""])
            if kind in ("skill", "tool"):
                text.extend([f"お試し：`/otogi try {ability_id}`", ""])
            html.append(f'<article class="ability-card {kind}" id="ability-{ability_id}" data-kind="{kind}">'
                        f'<div class="card-heading"><span class="kind-badge" aria-label="分類">{label}</span>'
                        f'<h3>{escape(name)}</h3></div><code class="ability-id">{ability_id}</code><ul>'
                        + "".join(f"<li>{escape(line)}</li>" for line in description) + "</ul>"
                        + (f'<p class="try-command"><code>/otogi try {ability_id}</code></p>' if kind in ("skill", "tool") else "")
                        + '</article>')
        html.append("</div></section>")
    (OUT / "abilities.md").write_text("\n".join(text), encoding="utf-8")
    return "\n".join(html), toc


def hero(total):
    return f'''<section class="hero" aria-labelledby="cover-title">
<div class="cover-copy"><p class="eyebrow">OTOGI / PLAYER'S HANDBOOK</p>
<h1 id="cover-title">御伽の<br><span>歩き方。</span></h1>
<p class="hero-lead">術を選び、間合いで勝つ。<br>自分の5枠で戦う、異能力PvP。</p>
<a class="pixel-button primary" href="#section-1">はじめての一試合 <span aria-hidden="true">→</span></a>
<a class="text-link" href="abilities.html">全{total}種の能力を見る →</a></div>
<div class="cover-art" aria-label="式・相・術・術・具の5枠を組み合わせるビルドの例">
<div class="art-top"><span>YOUR BUILD</span><span>5 / 5</span></div>
<svg class="sword-art" viewBox="0 0 160 128" aria-hidden="true" shape-rendering="crispEdges">
<path fill="#172b24" d="M16 104h8v-8h8v-8h8v-8h8v-8h8v-8h8v-8h8v-8h8v-8h8v-8h8v-8h8V8h32v32h-8v8h-8v8h-8v8h-8v8h-8v8h-8v8h8v8h8v16H88v-8h-8v-8h-8v8h-8v8h-8v8H32v-8H16z"/>
<path fill="#cbe5c4" d="M56 72l56-56h16v16L72 88z"/><path fill="#79b99c" d="M64 80l56-56h8v8L72 88z"/>
<path fill="#e5b553" d="M40 72h16v8h8v8h8v8h8v8h8v8H72v-8h-8v-8h-8v-8h-8v-8h-8z"/>
<path fill="#c47d4f" d="M48 88h8v8h8l-24 24H24v-16z"/><path fill="#ecdbaa" d="M24 112h16v8H24z"/>
<path fill="#e5b553" d="M16 24h8v8h8v8h-8v8h-8v-8H8v-8h8zM140 76h8v8h8v8h-8v8h-8v-8h-8v-8h8z"/>
</svg>
<p class="art-caption">選んだ能力が、戦い方になる。</p>
<div class="hotbar">{''.join(f'<div class="slot {kind}"><span>{label}</span><small>{number}</small></div>' for number, (kind, label) in enumerate([('park', '式'), ('passive', '相'), ('skill', '術'), ('skill', '術'), ('tool', '具')], 1))}</div>
<p class="art-foot">常時効果 ＋ 自動発動 ＋ 術 ＋ 武器</p></div></section>
<div class="facts"><p><strong>4</strong>つの能力分類</p><p><strong>{total}</strong>種の能力</p>
<p><strong>6</strong>つの対戦モード</p><p><strong>5</strong>枠でビルド</p></div>'''


def page(title, body, toc, active, total):
    top = hero(total) if active == "guide.md" else (
        f'<section class="page-cover"><p class="eyebrow">OTOGI / '
        f'{"ABILITY ARCHIVE" if active == "abilities.md" else "SERVER HANDBOOK"}</p>'
        f'<h1>{escape(title)}</h1><p>御伽 / Otogi　{("能力リファレンス" if active == "abilities.md" else "サーバー運営の手引き")}</p></section>')
    navigation = "".join(
        f'<a href="{file}"' + (' aria-current="page"' if source == active else '') + f'>{label}</a>'
        for source, file, label in [("guide.md", "index.html", "遊び方"), ("abilities.md", "abilities.html", "能力図鑑"),
                                    ("server-guide.md", "server-guide.html", "運営ガイド")])
    contents = "".join(f'<li class="depth-{level}"><a href="#{anchor}">{escape(label)}</a></li>'
                       for level, label, anchor in toc if level == 2)
    return f'''<!doctype html>
<html lang="ja"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="description" content="御伽 / Otogiの{escape(title)}。能力を組み合わせて戦うMinecraftの異能力PvP。">
<meta name="color-scheme" content="light"><title>{escape(title)} | 御伽 / Otogi</title>
<link rel="icon" href="assets/mark.svg" type="image/svg+xml"><link rel="stylesheet" href="assets/book.css">
<script src="assets/book.js" defer></script></head><body>
<a class="skip-link" href="#main">本文へ移動</a>
<header class="site-header"><a href="index.html" class="brand"><span class="brand-mark" aria-hidden="true">伽</span>
<span>御伽<small>OTOGI HANDBOOK</small></span></a><nav aria-label="ガイドの切り替え">{navigation}</nav>
<button class="font-toggle" type="button" aria-pressed="false" hidden>標準の字体にする</button></header>
<main id="main">{top}<div class="book-layout"><aside class="toc"><p class="eyebrow">このページの目次</p>
<nav aria-label="目次"><ol>{contents}</ol></nav><p class="edition">初期設定版 / v0.1.0<br>2026.10.22</p>
<a class="source-link" href="{active}">Markdown原稿 ↗</a></aside>
<article class="prose" aria-label="{escape(title)}">{body}</article></div></main>
<footer><a class="footer-brand" href="index.html">御伽 / Otogi</a><p>同梱の初期設定に基づくガイド。実際のルールはゲーム内でもご確認ください。</p>
<p>MinecraftはMojangおよびMicrosoftの商標です。御伽は非公式のコミュニティ制作プラグインです。</p>
<p class="font-credit">Font: <a href="https://github.com/fontworks-fonts/DotGothic16">DotGothic16</a> · <a href="assets/fonts/OFL.txt">SIL Open Font License 1.1</a></p>
<a href="#main">ページの先頭へ ↑</a></footer></body></html>'''


def main():
    values = abilities()
    total = len(values)
    # Count references in the introductory manuscript must be intentionally updated.
    for file in ("guide.md", "README.md"):
        source = (OUT / file).read_text(encoding="utf-8")
        if "54種" in source and total != 54:
            raise ValueError(f"Update ability counts in {file}: now {total}")
    generated_catalog, catalog_toc = catalog(values)
    for source, target in PAGES.items():
        manuscript = (OUT / source).read_text(encoding="utf-8")
        title = manuscript.splitlines()[0].removeprefix("# ")
        if source == "abilities.md":
            body, toc = generated_catalog, catalog_toc
        else:
            body, toc = markdown(manuscript)
        (OUT / target).write_text(page(title, body, toc, source, total), encoding="utf-8")
    print(f"Built 3 HTML pages and abilities.md: {total} abilities, {dict(Counter(v['kind'] for v in values))}")


if __name__ == "__main__":
    main()
