#!/usr/bin/env python3
"""
Material Symbols 字体子集化脚本。

从全量可变字体中裁剪需要的 codepoints，并生成两个静态字体文件：
- material_symbols_outlined.ttf（默认字形）
- material_symbols_filled.ttf（应用 FILL=1 的 GSUB 替换后）

使用方法：
    python3 scripts/subset_font.py

依赖：fonttools
    pip install fonttools
"""

import os
import sys
from copy import deepcopy
from pathlib import Path

from fontTools.subset import Subsetter, Options
from fontTools.ttLib import TTFont

PROJECT_ROOT = Path(__file__).parent.parent
FONT_DIR = PROJECT_ROOT / "android" / "app" / "src" / "main" / "res" / "font"
FULL_FONT = PROJECT_ROOT / "scripts" / "material_symbols_rounded_full.ttf"
CODEPOINTS_FILE = PROJECT_ROOT / "scripts" / "icon_codepoints.txt"

OUTLINED_FONT = FONT_DIR / "material_symbols_outlined.ttf"
FILLED_FONT = FONT_DIR / "material_symbols_filled.ttf"


def load_codepoints(path: Path) -> set[int]:
    """从 codepoints 文件加载需要的 Unicode codepoints。"""
    codepoints = set()
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line.startswith("U+"):
                codepoints.add(int(line[2:], 16))
    return codepoints


def subset_variable_font(font_path: Path, codepoints: set[int]) -> TTFont:
    """从全量字体中裁剪出子集化可变字体。"""
    font = TTFont(font_path)
    options = Options()
    options.layout_features = ["*"]
    options.notdef_outline = True
    options.recommended_glyphs = True
    options.hinting = False
    options.desubroutinize = True

    subsetter = Subsetter(options=options)
    subsetter.populate(unicodes=codepoints)
    subsetter.subset(font)
    return font


def remove_variable_tables(font: TTFont) -> None:
    """移除可变字体相关表，得到静态字体。"""
    for tag in ["gvar", "fvar", "avar", "HVAR", "STAT"]:
        if tag in font:
            del font[tag]


def apply_gsub_replacements(font: TTFont) -> None:
    """应用 GSUB FeatureVariations 中的字形替换（FILL=1 状态）。"""
    gsub = font["GSUB"].table

    # 检查是否存在 FeatureVariations
    if not hasattr(gsub, "FeatureVariations") or not gsub.FeatureVariations:
        return

    fv = gsub.FeatureVariations.FeatureVariationRecord[0]
    sub_rec = fv.FeatureTableSubstitution.SubstitutionRecord[0]
    feat = sub_rec.Feature
    lookup_idx = feat.LookupListIndex[0]
    lookup = gsub.LookupList.Lookup[lookup_idx]
    subtable = lookup.SubTable[0]
    mapping = subtable.mapping

    glyf = font["glyf"]
    for orig_name, repl_name in mapping.items():
        if orig_name in glyf.glyphs and repl_name in glyf.glyphs:
            glyf.glyphs[orig_name] = deepcopy(glyf.glyphs[repl_name])

    # 移除已应用的 FeatureVariations
    del gsub.FeatureVariations


def main() -> int:
    if not FULL_FONT.exists():
        print(f"错误: 全量字体文件不存在: {FULL_FONT}")
        print("请先从 Google 官方仓库下载 MaterialSymbolsRounded[FILL,GRAD,opsz,wght].ttf")
        return 1

    if not CODEPOINTS_FILE.exists():
        print(f"错误: codepoints 文件不存在: {CODEPOINTS_FILE}")
        return 1

    codepoints = load_codepoints(CODEPOINTS_FILE)
    print(f"需要裁剪 {len(codepoints)} 个 codepoints")

    # 子集化可变字体
    print("正在子集化可变字体...")
    subset_font = subset_variable_font(FULL_FONT, codepoints)

    # 生成 outlined 静态字体
    print("正在生成 outlined 静态字体...")
    font_outlined = deepcopy(subset_font)
    remove_variable_tables(font_outlined)
    OUTLINED_FONT.parent.mkdir(parents=True, exist_ok=True)
    font_outlined.save(OUTLINED_FONT)

    # 生成 filled 静态字体
    print("正在生成 filled 静态字体...")
    font_filled = deepcopy(subset_font)
    apply_gsub_replacements(font_filled)
    remove_variable_tables(font_filled)
    FILLED_FONT.parent.mkdir(parents=True, exist_ok=True)
    font_filled.save(FILLED_FONT)

    # 报告体积
    for path in [OUTLINED_FONT, FILLED_FONT]:
        size = path.stat().st_size
        print(f"  {path.name}: {size / 1024:.1f} KB")

    total = sum(p.stat().st_size for p in [OUTLINED_FONT, FILLED_FONT])
    print(f"总计: {total / 1024:.1f} KB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
