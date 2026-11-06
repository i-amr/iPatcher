package com.discord.patcher.helper;

import android.content.Context;
import com.android.apksig.KeyConfig;
import com.discord.patcher.security.Signature;
import java.io.File;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

public final class ApkSigner {

  public static void sign(Context ctx, File in, File out) throws Exception {
    final PrivateKey privateKey = Signature.getKey();
    final X509Certificate cert = Signature.getCertificate();

    final KeyConfig keyConfig = new KeyConfig.Jca(privateKey);
    final com.android.apksig.ApkSigner.SignerConfig cfg = new com.android.apksig.ApkSigner.SignerConfig.Builder("key", keyConfig, Collections.singletonList(cert)).build();

    new com.android.apksig.ApkSigner.Builder(Collections.singletonList(cfg))
        .setInputApk(in)
        .setOutputApk(out)
        .setMinSdkVersion(24) // Discord's minSdk; v1 not needed at 24+
        .setV1SigningEnabled(false)
        .setV2SigningEnabled(true)
        .setV3SigningEnabled(true)
        .build()
        .sign();
  }
}
