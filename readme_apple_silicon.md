# Creasy on Apple Silicon (M1/M2/M3)

This guide covers running Creasy on macOS **Apple Silicon** (arm64/aarch64). The most common failure on M‑series Macs is loading **x86_64** JavaFX native libraries ("incompatible architecture"), which results in errors like:

- `libprism_es2.dylib ... incompatible architecture (have 'x86_64', need 'arm64')`
- `Error initializing QuantumRenderer: no suitable pipeline found`
- `No toolkit found`

## Requirements

- **JDK 16+** (arm64 build). Example: Homebrew OpenJDK on Apple Silicon.
- **JavaFX SDK** for **macOS arm64** (not x86_64).

## One-time cleanup (if you already tried to run Creasy)

JavaFX caches its native libraries. If you previously ran with the wrong architecture, clear the cache:

```bash
rm -rf ~/.openjfx/cache/16
```

(Replace `16` with the JavaFX version you installed, if different.)

## Run the app with JavaFX (arm64)

1. Download the **macOS arm64** JavaFX SDK (e.g., JavaFX 16+).
2. Set `JAVA_FX_HOME` to the extracted SDK path.
3. Run the jar with the JavaFX modules on the module path.

```bash
export JAVA_FX_HOME=javafx-sdk-25.0.2
java \
  --module-path "$JAVA_FX_HOME/lib" \
  --add-modules javafx.controls,javafx.fxml \
  -jar target/Creasy-0.1.0.jar
```

If the app uses Swing interop, add `javafx.swing`:

```bash
--add-modules javafx.controls,javafx.fxml,javafx.swing
```

## Optional: silence native-access warning

Recent JDKs show a warning about restricted native access. You can silence it with:

```bash
java \
  --enable-native-access=ALL-UNNAMED \
  --module-path "$JAVA_FX_HOME/lib" \
  --add-modules javafx.controls,javafx.fxml \
  -jar target/Creasy-0.1.0.jar
```

This warning is not the cause of the crash; it is just a notice.

## If you still see architecture errors

- Ensure **both** Java and JavaFX are **arm64** builds.
- Remove `~/.openjfx/cache/<version>` and rerun.
- Avoid mixing x86_64 Java (Rosetta) with arm64 JavaFX, or vice‑versa.

## Notes for developers

When building the jar, JavaFX is **not reliably bundled** on all systems. On Apple Silicon, the safest approach is to install the arm64 JavaFX SDK locally and run with `--module-path` as shown above.
