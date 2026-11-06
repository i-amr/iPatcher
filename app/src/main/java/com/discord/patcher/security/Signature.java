package com.discord.patcher.security;

import com.discord.patcher.App;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;

public class Signature {
  public static final String ALIAS = "あ";
  public static final char[] PASS = "P@$$W0RD".toCharArray();

  private static KeyStore jks;

  public static KeyStore getKeyStore() throws Exception {
    if (jks != null) return jks;

    jks = KeyStore.getInstance("PKCS12");
    try (InputStream is = App.getAsset().open("key.p12")) {
      jks.load(is, PASS);
    }

    return jks;
  }

  public static <T extends Key> T getKey() throws Exception {
    if (jks == null) jks = getKeyStore();

    return (T) jks.getKey(ALIAS, PASS);
  }

  public static <T extends Certificate> T getCertificate() throws Exception {
    if (jks == null) jks = getKeyStore();

    return (T) jks.getCertificate(ALIAS);
  }
}
