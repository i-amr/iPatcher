package com.discord.patcher.helper;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import com.discord.patcher.receiver.InstallResultReceiver;
import com.discord.patcher.App;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public final class ApkInstaller {

  public static void uninstall(Activity a) {
    a.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + DiscordLocator.PKG)));
  }

  public static void install(List<File> apks) throws IOException {
    PackageInstaller pi = App.getPackage().getPackageInstaller();
    PackageInstaller.SessionParams params =
        new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
    params.setAppPackageName(DiscordLocator.PKG);

    int id = pi.createSession(params);
    try (PackageInstaller.Session s = pi.openSession(id)) {
      byte[] buf = new byte[64 * 1024];
      if (apks != null)
        for (File f : apks) {
          try (InputStream in = new FileInputStream(f);
              OutputStream out = s.openWrite(f.getName(), 0, f.length())) {
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            s.fsync(out);
          }
        }
      Context ctx = App.getContext();
      Intent cb = new Intent(ctx, InstallResultReceiver.class);
      PendingIntent pending =
          PendingIntent.getBroadcast(
              ctx,
              id,
              cb,
              PendingIntent.FLAG_UPDATE_CURRENT
                  | PendingIntent.FLAG_MUTABLE); // MUTABLE is required
      s.commit(pending.getIntentSender());
    }
  }
}
