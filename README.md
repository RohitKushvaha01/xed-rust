# Xed-Editor Extension Template

This repository is a starting point for building extensions for **Xed-Editor (Karbon)**.
It includes a ready-to-use template, build scripts, and a simple folder structure so you can focus on writing your extension instead of setting up the environment.

> [!TIP]
> See the [documentation](https://xed-editor.github.io/Xed-Docs/docs/extensions) page for more details about creating and managing extensions for Xed-Editor.

---

## Getting Started

### 1. Clone the Template

```bash
git clone https://github.com/Xed-Editor/Extension-Template
cd Extension-Template
```

---

### 2. Configure Your Extension

Before building, update the following in `manifest.json`:

* `name` – your extension’s name
* `version` – version of your extension
* `author` – Developer of the extension

>[!WARNING]
If you rename the main class or move it to another package/folder, **you must update the `mainClass` field in `manifest.json`**, or the extension will not load.

---

### 3. Build the Extension

To build and package the extension, run:

```bash
./gradlew :app:createFinalZip
```

This command automatically:
* Cleans previous output directories.
* Compiles both `debug` and `release` APK variants.
* Disables ProGuard (R8) obfuscation for the `debug` build.
* Enables ProGuard (R8) obfuscation and applies the SDK mappings for the `release` build.
* Packages both APKs into the final output ZIP file.

> [!NOTE]
> For a completely clean build from scratch, you can run `./gradlew clean :app:createFinalZip`.

---

### 4. Find the Output

After a successful build, your extension package will be created here:

```
output/YourExtensionName.zip
```

