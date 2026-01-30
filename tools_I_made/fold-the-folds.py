from pathlib import Path
import subprocess
from concurrent.futures import ThreadPoolExecutor, as_completed
import os

ORIPA_JAR = Path(__file__).resolve().parent.parent / "lib" / "oripa-1.78-all.jar"

def fold_the_folds(fold_path: Path) -> None:
    folded_fold_path = fold_path.with_suffix(".folded.fold")
    subprocess.run(
        ["java", "-jar", str(ORIPA_JAR), "--fold", str(folded_fold_path), str(fold_path)],
        check=True,
    )

def main(max_workers: int | None = None) -> None:
    folder = Path(__file__).resolve().parent
    fold_paths = list(folder.rglob("*.fold"))

    if max_workers is None:
        cpu_count = os.cpu_count() or 1
        max_workers = max(1, cpu_count - 1)

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = [executor.submit(fold_the_folds, p) for p in fold_paths]
        for f in as_completed(futures):
            f.result()

if __name__ == "__main__":
    main()
