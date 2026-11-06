package com.discord.patcher.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import com.discord.patcher.App;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;

public class SignatureCheck {
  // keystore vs installed-app
  public static boolean isSignatureMatched(String packageName) {
    PackageManager pm = App.getPackage();

    try {
      Signature[] installedSignatures = getInstalledAppSignatures(pm, packageName);
      if (installedSignatures == null || installedSignatures.length == 0) {
        return false;
      }

      return Arrays.equals(
          installedSignatures[0].toByteArray(),
          com.discord.patcher.security.Signature.getCertificate().getEncoded());
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  // installed-app vs apk-file
  public static boolean isSignatureMatched(String packageName, File apkFile) {
    PackageManager pm = App.getPackage();

    try {
      Signature[] installedSignatures = getInstalledAppSignatures(pm, packageName);
      if (installedSignatures == null || installedSignatures.length == 0) {
        return false;
      }

      Signature[] patchSignatures = getApkSignatures(pm, apkFile.getAbsolutePath());
      if (patchSignatures == null || patchSignatures.length == 0) {
        return false;
      }

      return Arrays.equals(installedSignatures[0].toByteArray(), patchSignatures[0].toByteArray());
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  @SuppressWarnings("deprecation")
  private static Signature[] getInstalledAppSignatures(PackageManager pm, String packageName) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28+
        PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
        if (pi.signingInfo != null) {
          if (pi.signingInfo.hasMultipleSigners()) {
            return pi.signingInfo.getApkContentsSigners();
          } else {
            return pi.signingInfo.getSigningCertificateHistory();
          }
        }
      } else {
        PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        return pi.signatures;
      }
    } catch (PackageManager.NameNotFoundException e) {
      // app is not installed
      return null;
    }
    return null;
  }

  @SuppressWarnings("deprecation")
  private static Signature[] getApkSignatures(PackageManager pm, String apkPath) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28+
      PackageInfo pi = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNING_CERTIFICATES);
      if (pi != null && pi.signingInfo != null) {
        if (pi.signingInfo.hasMultipleSigners()) {
          return pi.signingInfo.getApkContentsSigners();
        } else {
          return pi.signingInfo.getSigningCertificateHistory();
        }
      }
    } else {
      PackageInfo pi = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNATURES);
      if (pi != null) {
        return pi.signatures;
      }
    }
    return null;
  }

  public static boolean isOldSignature(String n) {
    if (!n.startsWith("META-INF/")) return false;
    String u = n.toUpperCase(Locale.ROOT);
    return u.endsWith(".SF")
        || u.endsWith(".RSA")
        || u.endsWith(".DSA")
        || u.endsWith(".EC")
        || u.endsWith("/MANIFEST.MF");
  }
}
