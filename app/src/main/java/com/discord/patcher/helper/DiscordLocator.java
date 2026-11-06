package com.discord.patcher.helper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.discord.patcher.App;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DiscordLocator {
  public static final String PKG = "com.discord";

  public static boolean hasInstalledDiscord() {
    try {
      ApplicationInfo ai = App.getPackage().getApplicationInfo(PKG, 0);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  public static List<File> copySplits(Context ctx, File destDir) throws Exception {
    ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(PKG, 0);

    List<String> paths = new ArrayList<>();
    paths.add(ai.sourceDir); // base.apk
    if (ai.splitSourceDirs != null) { // config.*.apk, split.*.apk
      paths.addAll(Arrays.asList(ai.splitSourceDirs));
    }

    long needed = 0;
    for (String p : paths) needed += new File(p).length();

    final long space = destDir.getUsableSpace();
    if (space < needed * 3) {
      throw new IOException("Not enough free storage (need ~" + (needed * 3 >> 20) + " MB)");
    }

    if (!destDir.exists() && !destDir.mkdirs()) throw new IOException("mkdirs failed");
    List<File> out = new ArrayList<>();
    for (String p : paths) {
      File src = new File(p);
      File dst = new File(destDir, src.getName());
      copy(src, dst);
      out.add(dst);
    }
    return out;
  }

  static void copy(File src, File dst) throws IOException {
    byte[] buf = new byte[64 * 1024];
    try (InputStream in = new FileInputStream(src);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(dst), 64 * 1024)) {
      int n;
      while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }
  }
}
