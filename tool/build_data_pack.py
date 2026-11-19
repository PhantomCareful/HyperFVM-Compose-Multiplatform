"""打包 hyperfvm_data.zip：App 运行所需的全部图片与数据文件。

数据包结构（与 DataPackRepository 的约定一致）：

    manifest.json                   版本信息 { version, builtAt, fileCount }
    data/card_desc.xml              官方道具表（套装 / 翅膀条目来源）
    data/market_item.xml            官方续费表
    data/odds_boxes.json            宝箱概率公示
    data/odds_draws.json            抽奖概率公示
    data/odds_sources.json          官方公示合集（概率公示页的来源列表）
    images/{id}.webp|png            全部图片，id 即原资源名（去扩展名）

图片取 tool/data_pack_src/images 下的全部文件（原来放在 composeResources/drawable，
拆分后安装包里只留 12 个 XML 矢量图，图片只随数据包分发）。更新数据包时
把新图放进这个目录再重跑脚本即可。

概率公示的图标（宝箱封面 / 奖品图标）已改为运行时按道具 id 从官方图床下载，
不再放进这里；数据包只保留图鉴 / 食神谱等页面离线展示要用的图。

用法：
    python tool/build_data_pack.py --version 1

产物写到 tool/_datapack_out/：
    hyperfvm_data.zip               上传到 Gitee 仓库 DataPack/ 目录
    hyperfvm_data_version.json      同样上传（App 从这里读版本与地址）
"""
import argparse
import datetime
import hashlib
import json
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
IMAGES = ROOT / "tool/data_pack_src/images"
OUT = ROOT / "tool/_datapack_out"

# 留在安装包里的 XML 矢量图（painterResource 直接引用，不进数据包）。
# 列表仅作说明：打包源目录里本来就只有位图。

DATA_FILES = {
    "card_desc.xml": ROOT / "tool/card_desc.xml",
    "market_item.xml": ROOT / "tool/market_item.xml",
    "odds_boxes.json": ROOT / "tool/_odds_export/odds_boxes.json",
    "odds_draws.json": ROOT / "tool/_odds_export/odds_draws.json",
    "odds_sources.json": ROOT / "tool/_odds_export/odds_sources.json",
}

ZIP_URL_TEMPLATE = "https://gitee.com/phantom-careful/hyper-fvm-updater/releases/download/{tag}/hyperfvm_data.zip"


def collect_images():
    if not IMAGES.is_dir():
        raise SystemExit(f"缺少图片源目录：{IMAGES}")
    images = []
    stems = {}
    for path in sorted(IMAGES.iterdir()):
        if not path.is_file():
            continue
        if path.suffix.lower() == ".xml":
            raise SystemExit(f"图片源目录不应包含 XML：{path.name}（矢量图留在安装包里）")
        stem = path.stem
        if stem in stems:
            raise SystemExit(f"图片 id 冲突：{path.name} 与 {stems[stem]} 同名不同扩展")
        stems[stem] = path.name
        images.append(path)
    return images


def sha256_of(path):
    digest = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", type=int, default=1, help="数据包版本号（整数递增，同时作为 Release 标签）")
    parser.add_argument("--url", default=None, help="zip 下载地址；缺省按 Release 标签拼")
    args = parser.parse_args()
    zip_url = args.url or ZIP_URL_TEMPLATE.format(tag=args.version)

    images = collect_images()
    for name, path in DATA_FILES.items():
        if not path.is_file():
            raise SystemExit(f"缺少数据文件：{path}")

    OUT.mkdir(parents=True, exist_ok=True)
    zip_path = OUT / "hyperfvm_data.zip"

    manifest = {
        "version": args.version,
        "builtAt": datetime.date.today().isoformat(),
        "fileCount": len(images) + len(DATA_FILES) + 1,
    }

    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        zf.writestr("manifest.json", json.dumps(manifest, ensure_ascii=False, indent=1) + "\n")
        for name, path in DATA_FILES.items():
            zf.write(path, f"data/{name}")
        for path in images:
            zf.write(path, f"images/{path.name}")

    size = zip_path.stat().st_size
    version = {
        "version": args.version,
        "size": size,
        "sha256": sha256_of(zip_path),
        "url": zip_url,
    }
    version_path = OUT / "hyperfvm_data_version.json"
    version_path.write_text(json.dumps(version, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")

    print(f"图片 {len(images)} 张，数据文件 {len(DATA_FILES)} 个")
    print(f"zip: {zip_path}  ({size / 1024 / 1024:.1f} MB)")
    print(f"version: {version_path}")
    print(f"sha256: {version['sha256']}")


if __name__ == "__main__":
    sys.exit(main())
