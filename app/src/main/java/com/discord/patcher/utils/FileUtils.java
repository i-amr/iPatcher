package com.discord.patcher.utils;
import android.content.res.AssetManager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import com.discord.patcher.App;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
  public static final int ACTION_CREATE = 1;
  public static final int ACTION_PICK = 2;

  public static final String MIME_APK = "application/vnd.android.package-archive";
  public static final String MIME_BIN = "application/octet-stream";

  public static final String EXTRA_PATH = "android.intent.extra.PATH";

  public static void createApk(Activity activity, String outputFileName) {
    Intent intent =
        new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(MIME_BIN)
            .putExtra(Intent.EXTRA_TITLE, outputFileName);

    activity.startActivityForResult(intent, ACTION_CREATE);
  }

  public static void writeUri(File content, Uri uri) throws IOException {
    if (content == null) return;

    try (OutputStream outputStream = App.getResolver().openOutputStream(uri)) {
      if (outputStream == null)
        throw new IOException("Could not open destination URI output stream");
      Files.copy(content.toPath(), outputStream);
    }
  }

  public static void pickApk(Activity activity) {
    Intent intent =
        new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(MIME_BIN);

    activity.startActivityForResult(intent, ACTION_PICK);
  }

  public static void readUri(Uri uri, File destFile) throws IOException {
    try (InputStream inputStream = App.getResolver().openInputStream(uri)) {
      if (inputStream == null) throw new IOException("InputStream is null");

      StandardCopyOption option = StandardCopyOption.REPLACE_EXISTING;
      Files.copy(inputStream, destFile.toPath(), option);
    }
  }

  public static List<File> getApks(boolean in) {
    File[] files =
        (in ? getInputDir() : getOutputDir()).listFiles((dir, name) -> name.endsWith(".apk"));

    if (files == null || files.length == 0) return null;

    List<File> apks = new ArrayList<>();
    for (File f : files) apks.add(f);

    if (apks.isEmpty()) return null;
    return apks;
  }

  public static byte[] readAsset(String path) throws IOException {
    try (InputStream in = App.getAsset().open(path, AssetManager.ACCESS_STREAMING);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(16 * 1024)) {
      byte[] b = new byte[16 * 1024];
      int n;
      while ((n = in.read(b)) > 0) bos.write(b, 0, n);
      return bos.toByteArray();
    }
  }

  public static void deleteRecursive(File root) {
    File[] files = root.listFiles();
    if (files != null) {
      for (File file : files) deleteRecursive(file);
    }
    root.delete();
  }

  public static File getWorkingDir() {
    File dir = new File(App.getCache(), "work");
    createDir(dir);
    return dir;
  }

  public static File getInputDir() {
    File dir = new File(getWorkingDir(), "in");
    createDir(dir);
    return dir;
  }

  public static File getOutputDir() {
    File dir = new File(getWorkingDir(), "out");
    createDir(dir);
    return dir;
  }

  public static boolean createDir(File f) {
    if (f.isFile()) {
      f.delete();
      return f.mkdirs();
    }
    return f.isDirectory() || f.mkdirs();
  }

  public static void cleanWorkingDir() {
    deleteRecursive(getWorkingDir());
  }
}
