package com.discord.patcher;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import java.io.File;

public class App extends Application {
  private static Context context;
  private static AssetManager assets;
  private static PackageManager pm;

  @Override
  public void onCreate() {
    super.onCreate();
    context = getApplicationContext();
    assets = getAssets();
    pm = getPackageManager();
  }

  public static Context getContext() {
    return context;
  }

  public static AssetManager getAsset() {
    return assets;
  }

  public static PackageManager getPackage() {
    return pm;
  }

  public static File getCache() {
    return context.getCacheDir();
  }

  public static ContentResolver getResolver() {
    return context.getContentResolver();
  }
}
