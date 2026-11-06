package com.discord.patcher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

public class InstallResultReceiver extends BroadcastReceiver {
  @Override
  @SuppressWarnings("deprecation")
  public void onReceive(Context ctx, Intent i) {
    int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
    if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
      Intent confirm = i.getParcelableExtra(Intent.EXTRA_INTENT);
      confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ctx.startActivity(confirm);
    } else if (status != PackageInstaller.STATUS_SUCCESS) {
      Log.e("Patcher", "Install failed: " + i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
    }
  }
}
