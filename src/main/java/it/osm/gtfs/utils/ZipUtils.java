package it.osm.gtfs.utils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/***
 * This class contains methods to work with zip files
 */
public class ZipUtils {

    public static void unzipToDirectory(File file, String extractDirectory) {
        try {
            unzipToDirectory(new FileInputStream(file), extractDirectory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static void unzipToDirectory(InputStream inputStream,
                                          String extractPath) {

        File extractDir = new File(extractPath);

        if (extractDir.isFile()) {
            System.err.println("The supplied directory path is a file and not a directory.");
            return;
        }
        extractDir.mkdirs();

        try {
            ZipInputStream zipinputStream = new ZipInputStream(inputStream);
            ZipEntry entry;

            while ((entry = zipinputStream.getNextEntry()) != null) {
                String filePath = extractDir.getPath() + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it

                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                    byte[] bytesIn = new byte[4096];
                    int read = 0;

                    while ((read = zipinputStream.read(bytesIn)) != -1) {
                        bos.write(bytesIn, 0, read);
                    }

                    bos.close();

                } else {
                    // if the entry is a directory, make the directory
                    File dir = new File(filePath);
                    dir.mkdir();
                }

                zipinputStream.closeEntry();
            }

            zipinputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

