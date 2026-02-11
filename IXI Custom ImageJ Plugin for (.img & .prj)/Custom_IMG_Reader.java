import ij.*;
import ij.io.*;
import ij.plugin.PlugIn;
import java.io.*;
import java.util.regex.*;

/**
 * IXI Custom IMG Reader
 * Designed to be called by ImageJ's HandleExtraFileTypes hook.
 */
public class Custom_IMG_Reader implements PlugIn {

    public void run(String path) {
        // Check if called from HandleExtraFileTypes to avoid double-opening or empty
        // dialogs
        // In this plugin, that stack-trace code acts as a guard to stop the pluginS
        // from
        // running when ImageJ already invoked it internally.
        // Double Up Prevention
        // ---------------------------------------------------------------------
        StackTraceElement[] stack = Thread.currentThread().getStackTrace(); // Standard java libary : Gets the thread
                                                                            // that is currently running this code : The
                                                                            // result is an ordered list of method calls
                                                                            // that led to this point
        for (StackTraceElement s : stack) { // enhanced for loop (or for-each loop).
            if (s.getClassName().contains("HandleExtraFileTypes")) { // This chunk checks who called your code and
                                                                     // immediately stops execution if it came from a
                                                                     // specific ImageJ class.
                return;
            }
        }
        // -----------------------------------------------------------------------------------------
        // What file to open? -> opens it, and shows the image!
        if (path == null || path.isEmpty()) {
            OpenDialog od = new OpenDialog("Open .img", ""); // Opens ImageJ’s file chooser dialog titled “Open .img”
            if (od.getFileName() == null) // If the user clicks Cancel, stop the plugin cleanly
                return;
            path = od.getDirectory() + od.getFileName(); // Builds the full file path from the selected folder + file
                                                         // name (USED for OPEN FUNCTION)
        }
        ImagePlus imp = open(path); // calls the function
        if (imp != null)
            imp.show(); // shows the image
    }

    // ----------------------------------------------------------------------------------------
    public ImagePlus open(String path) {
        // SET UP----------------------------------------------------
        // Want to create a file object from the file path
        // Exsample path:
        // "C:\Users\kaden\OneDrive\Desktop\Breast3_static_material_1025x1025x641_20kev.img"
        // ect
        // https://imagej.net/ij/developer/api/ij/ij/io/FileInfo.html : INFORMATION on
        // what FILE OBJECT CAN Hold
        File f = new File(path); // (java) (creates a file object like and adress)
        String name = f.getName(); // gets the file name (Breast3_static_material_1025x1025x641_20kev.img)
        long fileSize = f.length(); // total size in bytes
        FileInfo fi = new FileInfo(); // Imagej : Empty instructions sheet (Library : ij.io.FileInfo)
        fi.fileFormat = FileInfo.RAW; // This line is ImageJ-specific and it’s basically you telling ImageJ There is
                                      // no built in info to find.
        fi.intelByteOrder = true; // The file stores numbers in little-endian order (Intel style).
        fi.fileType = FileInfo.GRAY32_FLOAT; // float image.
        // ----------------------------------------------------------
        // Non Hard Coded Image Detection
        // --------------------------------------------------------
        Matcher m = Pattern // java.util.regex.Pattern and Matcher
                .compile("(\\d+)[xX](\\d+)[xX](\\d+)") // (Width, Height, and Depth). () = Captured Group
                .matcher(name);
        // m.group(1) returns the String "1025".
        // m.group(2) returns the String "1025"
        // m.group(3) returns the String "641".
        if (m.find()) { // file name contains dimensions
            // width - height - number of images
            fi.width = Integer.parseInt(m.group(1)); // parseINT changes it into an integer that java can read instead
                                                     // of text
            fi.height = Integer.parseInt(m.group(2));
            fi.nImages = Integer.parseInt(m.group(3));
        } else { // In case file name does not contain dimensions
            IJ.log("|X| Reader: Could not find dimensions in the filename: " + name);
            return null;
        }
        // --------------------------------------------------------------------------------------------
        // Non Hard Coded OFFSET calculator
        // Calculation: width * height * slices * 4 bytes per float
        long expectedDataSize = (long) fi.width * fi.height * fi.nImages * 4;
        if (fileSize >= expectedDataSize) {
            fi.longOffset = fileSize - expectedDataSize;
        } else {
            IJ.log("|X| Reader: File is smaller than expected dimensions allowed.");
            return null;
        }
        // ------------------------------------------------------------------------------------------------
        // File location
        // Library for getParent()
        // https://docs.oracle.com/javase/8/docs/api/java/io/File.html#getParent--
        fi.directory = f.getParent() + File.separator; // parent directy + slash for both (MAC & WINDOWS)
        fi.fileName = f.getName();
        // .prj file? ---------------------
        if (fi.fileName.toLowerCase().endsWith(".prj")) {
            fi.fileName = fi.fileName.substring(0, fi.fileName.length() - 4) + ".img";
        }
        // ----------------------------------------------------------------------------------------------
        // Open Image
        try {
            // Library: import ij.io.FileOpener;
            // Library: import ij.ImagePlus;
            return new FileOpener(fi).openImage(); // FileOpener is ImageJ’s class for loading images // Reads the file
                                                   // from disk
        } catch (Exception e) {
            IJ.log("|X| Reader: The file opener failed");
            return null;
        }
    }
}
