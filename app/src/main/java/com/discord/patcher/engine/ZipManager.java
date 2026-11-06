package com.discord.patcher.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipManager extends BaseProgressTask<File> {

  private final ExecutorService ioExecutor = Executors.newFixedThreadPool(2);

  public void start(List<File> files, File zipFile, TaskCallback<File> l) {
    ioExecutor.execute(
        () -> {
          try {
            if (files == null || files.isEmpty()) {
              throw new IOException("Files list cannot be null or empty.");
            }

            long totalBytes = 0;
            for (File file : files) {
              if (file != null && file.exists() && file.isFile()) {
                totalBytes += file.length();
              }
            }

            if (totalBytes == 0) {
              throw new IOException("No valid files found to zip or total size is 0.");
            }

            if (zipFile.isDirectory()) {
              zipFile.delete();
            }

            compress(files, totalBytes, zipFile, l);
            postProgress(l, 100, "Success: zip file has been created");
            postSuccess(l, zipFile);
          } catch (Throwable t) {
            postError(l, t);
          }
        });
  }

  public void compress(List<File> files, long totalBytes, File zipFile, TaskCallback<File> l)
      throws IOException {

    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {

      byte[] buffer = new byte[4096]; // 4Kb chunk
      long bytesProcessed = 0;
      int lastPercent = -1;

      for (File file : files) {
        if (file == null || !file.exists() || file.isDirectory()) {
          continue;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
          ZipEntry zipEntry = new ZipEntry(file.getName());
          zos.putNextEntry(zipEntry);

          int length;
          while ((length = fis.read(buffer)) >= 0) {
            zos.write(buffer, 0, length);
            bytesProcessed += length;

            int currentPercent = (int) ((bytesProcessed * 100) / totalBytes);
            if (currentPercent > 99) currentPercent = 99;

            if (currentPercent != lastPercent) {
              lastPercent = currentPercent;
              postProgress(l, currentPercent, "Compressing " + file.getName());
            }
          }
          zos.closeEntry();
        }
      }
    }
  }

  // made with ai
  public void decompress(File zipFile, File outputDir, TaskCallback<File> l) {
    try {
      if (zipFile == null || !zipFile.exists()) {
        throw new IOException("Source ZIP file does not exist.");
      }

      // 1. Calculate total uncompressed bytes across all entries for percentage tracking
      long totalBytes = 0;
      try (ZipFile zf = new ZipFile(zipFile)) {
        Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          if (!entry.isDirectory()) {
            totalBytes += entry.getSize();
          }
        }
      }

      if (totalBytes == 0) {
        throw new IOException("ZIP file is empty or uncompressed sizing could not be read.");
      }

      // Ensure target tracking folders exist
      if (!outputDir.exists()) {
        outputDir.mkdirs();
      }

      // 2. Perform the actual extraction tracking byte-by-byte
      try (FileInputStream fis = new FileInputStream(zipFile);
          ZipInputStream zis = new ZipInputStream(fis)) {

        byte[] buffer = new byte[4096];
        long bytesProcessed = 0;
        int lastPercent = -1;
        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {
          if (entry.isDirectory()) {
            zis.closeEntry();
            continue;
          }

          // Handle security path-traversal vulnerability prevention (Zip Slip)
          File outFile = new File(outputDir, entry.getName());
          String canonicalDestinationPath = outputDir.getCanonicalPath();
          String canonicalResultPath = outFile.getCanonicalPath();
          if (!canonicalResultPath.startsWith(canonicalDestinationPath + File.separator)) {
            throw new SecurityException(
                "Blocked malicious zip entry path escape: " + entry.getName());
          }

          // Ensure subdirectories for nested entry names exist inside cache/work/in
          File parentDir = outFile.getParentFile();
          if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
          }

          // Stream entry data into destination tracking file location
          try (FileOutputStream fos = new FileOutputStream(outFile)) {
            int length;
            while ((length = zis.read(buffer)) >= 0) {
              fos.write(buffer, 0, length);
              bytesProcessed += length;

              int currentPercent = (int) ((bytesProcessed * 100) / totalBytes);
              if (currentPercent > 99) currentPercent = 99;

              if (currentPercent != lastPercent) {
                lastPercent = currentPercent;
                postProgress(l, currentPercent, "Extracting " + entry.getName());
              }
            }
          }
          zis.closeEntry();
        }
      }
      postSuccess(l, null);
      postProgress(l, 100, "Success: zip files has been extracted");
    } catch (Throwable t) {
      postError(l, t);
    }
  }

  public static void writeBytes(
      ZipOutputStream zos, CountingOutputStream cos, ZipEntry dst, int method, byte[] data)
      throws IOException {
    if (method == ZipEntry.STORED) {
      CRC32 crc = new CRC32();
      crc.update(data);
      dst.setMethod(ZipEntry.STORED);
      dst.setSize(data.length);
      dst.setCompressedSize(data.length);
      dst.setCrc(crc.getValue());
      applyAlignment(zos, cos, dst);
    } else {
      dst.setMethod(ZipEntry.DEFLATED);
    }
    zos.putNextEntry(dst);
    zos.write(data);
    zos.closeEntry();
  }

  public static void copyEntry(
      ZipFile zf,
      ZipEntry src,
      ZipEntry dst,
      ZipOutputStream zos,
      CountingOutputStream cos,
      byte[] buf)
      throws IOException {
    if (src.getMethod() == ZipEntry.STORED) {
      dst.setMethod(ZipEntry.STORED);
      dst.setSize(src.getSize());
      dst.setCompressedSize(src.getSize());
      dst.setCrc(src.getCrc()); // required by ZipOutputStream for STORED
      applyAlignment(zos, cos, dst);
    } else {
      dst.setMethod(ZipEntry.DEFLATED);
    }
    zos.putNextEntry(dst);
    try (InputStream in = zf.getInputStream(src)) {
      int n;
      while ((n = in.read(buf)) > 0) zos.write(buf, 0, n);
    }
    zos.closeEntry();
  }

  /**
   * zipalign equivalent. STORED entries must start their DATA at a 4-byte boundary (4096 for .so
   * files). Required for resources.arsc when targetSdk >= 30. We pad by adding a dummy "extra
   * field" to the local header.
   */
  public static void applyAlignment(ZipOutputStream zos, CountingOutputStream cos, ZipEntry e)
      throws IOException {
    zos.closeEntry(); // flush previous entry so cos.count is exact
    int align = e.getName().endsWith(".so") ? 4096 : 4;
    int nameLen = e.getName().getBytes(StandardCharsets.UTF_8).length;
    long dataStart = cos.count + 30 + nameLen; // local header = 30 bytes + name + extra
    int pad = (int) ((align - (dataStart % align)) % align);
    if (pad == 0) return;
    if (pad < 4) pad += align; // an extra field needs >= 4 bytes
    byte[] extra = new byte[pad]; // zero-filled
    extra[0] = (byte) 0x35;
    extra[1] = (byte) 0xD9; // id 0xD935 (what zipalign uses), little-endian
    extra[2] = (byte) ((pad - 4) & 0xFF);
    extra[3] = (byte) (((pad - 4) >> 8) & 0xFF);
    e.setExtra(extra);
  }

  /** Tracks exactly how many bytes have been written, needed for alignment math. */
  public static final class CountingOutputStream extends FilterOutputStream {
    long count = 0;

    CountingOutputStream(OutputStream o) {
      super(o);
    }

    @Override
    public void write(int b) throws IOException {
      out.write(b);
      count++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      out.write(b, off, len);
      count += len;
    }
  }
}
