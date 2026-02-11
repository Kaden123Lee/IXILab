# IXI Custom ImageJ Plugin Report

## 1. Program Overview & Logic

This plugin suite allows ImageJ to open raw 3D image files (`.img`) and their project files (`.prj`) that follow a specific naming convention (e.g., `Name_1025x1025x641.img`). It bypasses ImageJ's default behavior for `.img` files, which assumes they are Analyze 7.5 format and require a matching `.hdr` header file.

### **Core Components**

#### **A. Custom_IMG_Reader.java**
This is the core logic that actually reads the image.
1.  **File Identification**: It takes a file path string as input.
2.  **Dimension Parsing**: It uses **Regular Expressions (Regex)** to extract image dimensions directly from the filename.
    *   *Pattern:* `(\\d+)[xX](\\d+)[xX](\\d+)` looks for numbers separated by 'x' (case-insensitive).
    *   *Example:* A file named `Breast_1025x1025x641.img` is parsed as Width: 1025, Height: 1025, Depth: 641.
3.  **Offset Calculation**: Instead of a fixed header size, it calculates the offset dynamically:
    *   `Expected Size = Width * Height * Slices * 4 bytes (Float32)`
    *   `Header Offset = Total File Size - Expected Size`
4.  **Loading**: It populates an ImageJ `FileInfo` object with these parameters (Intel Byte Order, Gray32 Float) and uses `FileOpener` to create the image.

#### **B. HandleExtraFileTypes.java**
This is ImageJ's "dispatcher" that intercepts file opening requests.
1.  ** interception**: It has been modified to check for `.img` and `.prj` files **before** the standard ImageJ readers.
2.  **Delegation**: If it finds such a file, it attempts to load your `Custom_IMG_Reader` plugin.
3.  **Reflection**: It uses Java Reflection to call the `open()` method of your reader preventing compile-time dependency loops and allowing valid failure handling (falling back to other readers if your reader returns null).

---

## 2. Changes to HandleExtraFileTypes.java

To integrate the custom reader, the following specific logic was injected into the `tryOpen` method.

**Location:** Inserted before the standard "Analyze format" check (approx lines 118-134).

**The Logic:**
```java
if (name.endsWith(".img") || name.endsWith(".prj")) {
    try {
        // Attempt to run the custom plugin by name
        Object reader = IJ.runPlugIn("Custom_IMG_Reader", "");
        
        // If the plugin was found ...
        if (reader != null) {
            // ... use Reflection to find and call the 'open' method
            java.lang.reflect.Method m = reader.getClass().getMethod("open", String.class);
            Object res = m.invoke(reader, path);
            
            // If it returns a valid ImagePlus, we are done!
            if (res instanceof ImagePlus)
                return res;
        }
    } catch (Exception e) {
        // If anything fails (class not found, method not found), fails silently
        // and lets ImageJ continue to its default readers.
    }
}
```

**Key Adjustments Made:**
*   **Priority Placement**: Placed *above* the NIfTI/Analyze check. This ensures your reader gets the "first refusal" on `.img` files.
*   **Removal of Menu Check**: We removed the `if (Menus.getCommands().get("Custom IMG Reader") != null)` check. This ensures the hook fires even if the plugin isn't explicitly listed in the users' Plugins menu file (common during development).
*   **PRJ Handling**: Added ` || name.endsWith(".prj")` to allows users to drag-and-drop the project file and still load the associated image.

---


## 3. How to Set Up the Program

### **Step 1: File Placement**
Navigate to your ImageJ plugins folder. You should see a folder structure like `ImageJ/plugins/Input-Output`.
Ensure both files are in this folder (or a subfolder you created):
1.  `Custom_IMG_Reader.java`
2.  `HandleExtraFileTypes.java`

**Critial Note:** `HandleExtraFileTypes.java` is a special file name. It *must* replace the existing file of the same name in `ImageJ/plugins/Input-Output` to take effect as the global file handler.

### **Step 2: Compilation**
You must compile the files in a specific order due to dependencies.

1.  Open **ImageJ**.
2.  Drag and drop **`Custom_IMG_Reader.java`** into the ImageJ toolbar to open it.
3.  Go to menu **Plugins > Compile and Run...**
    *   *Result:* This creates `Custom_IMG_Reader.class`.
4.  Drag and drop **`HandleExtraFileTypes.java`** into the ImageJ toolbar.
5.  Go to menu **Plugins > Compile and Run...**
    *   *Result:* This creates `HandleExtraFileTypes.class` and updates the system hook.
