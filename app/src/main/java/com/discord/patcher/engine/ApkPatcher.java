package com.discord.patcher.engine;

import com.discord.patcher.utils.FileUtils;
import com.discord.patcher.utils.SignatureCheck;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class ApkPatcher {

  public interface Progress {
    void onProgress(float fraction, String msg);
  }

  /** @return number of entries replaced in this APK */
  public int patch(File in, File out, Progress progress) throws IOException {
    int replaced = 0, seen = 0;
    long lastReport = 0;

    try (ZipFile zf = new ZipFile(in);
        ZipManager.CountingOutputStream cos =
            new ZipManager.CountingOutputStream(
                new BufferedOutputStream(new FileOutputStream(out), 128 * 1024));
        ZipOutputStream zos = new ZipOutputStream(cos)) {

      int total = zf.size();
      byte[] buf = new byte[64 * 1024];
      Enumeration<? extends ZipEntry> en = zf.entries();

      while (en.hasMoreElements()) {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("cancelled");

        ZipEntry src = en.nextElement();
        String name = src.getName();
        seen++;

        if (src.isDirectory() || SignatureCheck.isOldSignature(name)) continue;

        String assetPath = null;
        if (name.startsWith("assets/emoji-")) assetPath = name.replace("assets/", "emojis/");

        ZipEntry dst = new ZipEntry(name);
        dst.setTime(src.getTime());

        if (assetPath != null) {
          byte[] data = FileUtils.readAsset(assetPath);
          ZipManager.writeBytes(zos, cos, dst, src.getMethod(), data);
          replaced++;
        } else {
          ZipManager.copyEntry(zf, src, dst, zos, cos, buf);
        }

        long now = System.currentTimeMillis();
        if (now - lastReport > 100) {
          lastReport = now;
          progress.onProgress(seen / (float) total, name);
        }
      }
      zos.closeEntry();
    }
    return replaced;
  }
}
