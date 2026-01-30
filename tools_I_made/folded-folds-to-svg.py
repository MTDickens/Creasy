from pathlib import Path
import subprocess
from concurrent.futures import ThreadPoolExecutor, as_completed
import os

# 确保路径指向正确的 JAR 文件
ORIPA_JAR = Path(__file__).resolve().parent.parent / "lib" / "oripa-1.78-all.jar"

def folded_folds_to_svg(fold_path: Path) -> None:
    # 剥去 .folded.fold 并加上 .svg
    # 使用 replace 替换后缀，生成 output.svg
    svg_path = fold_path.with_name(fold_path.name.replace(".folded.fold", ".svg"))
    
    subprocess.run(
        ["java", "-jar", str(ORIPA_JAR), "--image", str(svg_path), "-n", "0", str(fold_path)],
        check=True,
    )

def main(max_workers: int | None = None) -> None:
    folder = Path(__file__).resolve().parent
    # 更改匹配模式，只处理 .folded.fold 文件
    fold_paths = list(folder.rglob("*.folded.fold"))

    if max_workers is None:
        cpu_count = os.cpu_count() or 1
        max_workers = max(1, cpu_count - 1)

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = [executor.submit(folded_folds_to_svg, p) for p in fold_paths]
        for f in as_completed(futures):
            f.result()

if __name__ == "__main__":
    main()
