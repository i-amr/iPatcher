# 🍏 iPatcher

A automated asset patcher for the Discord Android app. This tool unpacks a target Discord `.apks` or `.apkm` file, replaces the default emoji assets with iOS alternatives, and scripts the rebuild, alignment, and signing process for immediate installation.

## ⚙️ How It Works
1. **Target Selection:** Accepts an uninstalled `.apks` / `.apkm` file or pulls the currently installed Discord package from the device.
2. **Asset Substitution:** Automatically locates and overwrites all target image files residing under `/assets/emoji-{unicode}.png`.
3. **Repackage & Sign:** Recompiles the package structure and applies an automated cryptographic signature.
4. **Deployment:** Detects signature parity with the existing app on the target device:
   * **Matching Signatures:** Executes an immediate in-place upgrade.
   * **Mismatched Signatures:** Prompts the user to completely uninstall the conflicting old version before installing the newly compiled build.

## 🚀 Usage

### Prerequisites
* Target Discord APK or APKM package
* Android development environment or device environment capable of sideloading signed packages

### Quick Steps
1. Select your input Discord package.
2. Run the patcher to overwrite the `/assets/` directory.
3. Review the signature status check prompt.
4. Finalize the installation.

## 🤝 Contributing
Feel free to open an issue or submit a pull request if you run into parsing bugs with updated APKM structures or specific asset indexing limits.

## 📜 License
This project is licensed under the MIT License - see the LICENSE file for details.

*Disclaimer: This utility is an independent packaging tool intended for asset personalization. It is not affiliated with or endorsed by Discord Inc. or Apple Inc.*
