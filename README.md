# ByteObf v2.0.1

**A modern, robust, and easy-to-use Java bytecode obfuscator with a graphical user interface.**

This project, **ByteObf**, is an independent fork of the original [Bozar obfuscator by vimasig](https://github.com/vimasig/Bozar). It has been modernized to run on the latest Java versions (21+) and its core engine has been hardened to reliably handle complex, real-world applications and "fat JARs" without crashing.

The goal of ByteObf is to provide a stable, open-source tool for protecting Java applications through effective obfuscation techniques, making reverse-engineering more difficult.

## Key Features

ByteObf offers a suite of obfuscation and hardening techniques that can be configured through its GUI or via the command line.

#### Obfuscation Transformers
*   **Rename:** Systematically renames classes, methods, and fields to short or invisible names, removing semantic context.
*   **Control Flow Obfuscation:** Injects opaque predicates and bogus jump instructions to break down linear method logic and confuse decompilers.
*   **Constant Obfuscation:** Hides string literals and numbers by encoding them and rebuilding them at runtime.
*   **Line Number Removal:** Strips debugging line number information from class files to make stack traces less informative.
*   **Local Variable Removal:** Removes the names of local variables within methods.
*   **Source File Removal:** Strips the original source file name from the class file metadata.
*   **Shuffle Members:** Randomizes the order of methods and fields within classes to disrupt analysis.
*   **Remove Inner Class Info:** Detaches inner classes from their outer class metadata.
*   **Decompiler Crashing:** Injects specially crafted, invalid bytecode that is accepted by the JVM but known to crash or hang many popular Java decompilers.
*   **Watermarking:** Adds custom text or data to the output JAR's ZIP comments or class constant pools.

#### Core Engine Enhancements
*   **Robust JAR Processing:** Reliably processes complex "fat JARs" with missing dependencies or duplicate entries by using a safe-fallback mechanism (`COMPUTE_MAXS`).
*   **Modern Toolchain:** Built and tested on modern JDKs (23+) and dependencies, ensuring forward compatibility.
*   **Non-Modular:** The project has been converted from a modular to a standard classpath application for maximum compatibility.

## Getting Started

1.  Download the latest `ByteObf-2.0.1-all.jar` from the [**Releases**](https://github.com/rayacode/ByteObf/releases) page.
2.  Run the executable JAR from your terminal:
    ```bash
    java -jar ByteObf-2.0.1-all.jar
    ```
3.  Use the GUI to select your input JAR, output location, and desired obfuscation options.

## Building from Source

To build the project yourself, you will need **JDK 21 or newer**.

```bash
# 1. Clone your new repository
git clone https://github.com/rayacode/ByteObf.git
cd ByteObf

# 2. Run the Gradle wrapper to build the executable JAR
# On Windows
gradlew.bat shadowJar

# On Linux/macOS
./gradlew shadowJar
The final, runnable JAR will be located at build/libs/ByteObf-2.0.1-all.jar.

Command Line Arguments
ByteObf can be run headlessly from the command line, which is useful for integration into automated build scripts.

Command	Description
-input	Target file path to obfuscate.
-output	Output path for the obfuscated JAR.
-config	Path to a configuration file byteobfconfig.json
-noupdate	Disables the startup update check.
-console	Runs the application without a GUI and starts the obfuscation task immediately.



License

    ByteObf is licensed under the terms of the GNU General Public License v3.0.

    Acknowledgements and Original License
    This project, ByteObf, is a derivative work of the Bozar project, which was originally created by vimasig and licensed under the MIT license. The original copyright notice for the Bozar project is preserved below as required by its license.

 

MIT License

Copyright (c) 2021 vimasig

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.