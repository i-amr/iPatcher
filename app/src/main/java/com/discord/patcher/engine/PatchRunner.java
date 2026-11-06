package com.discord.patcher.engine;

import android.content.Context;
import com.discord.patcher.helper.ApkSigner;
import com.discord.patcher.helper.DiscordLocator;
import com.discord.patcher.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class PatchRunner extends BaseProgressTask<List<File>> {

  private final Context ctx;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private Future<?> future;

  public PatchRunner(Context c) {
    this.ctx = c.getApplicationContext();
  }

  public void cancel() {
    if (future != null) future.cancel(true);
  }

  public void start(TaskCallback<List<File>> l, boolean fromApks) {
    future =
        executor.submit(
            () -> {
              try {
                File inDir = FileUtils.getInputDir();
                File outDir = FileUtils.getOutputDir();

                List<File> inputs = null;
                if (fromApks) { // files
                  inputs = FileUtils.getApks(true);
                }

                if (inputs == null) { // installed
                  postProgress(l, 1, "Copying Discord APKs…");
                  inputs = DiscordLocator.copySplits(ctx, inDir);
                }

                if (inputs.size() == 0) {
                  postError(l, new RuntimeException("Error APKs input cannot be empty or null"));
                  return;
                }

                ApkPatcher patcher = new ApkPatcher();
                List<File> signed = new ArrayList<>();
                int totalReplaced = 0;
                int n = inputs.size();

                for (int i = 0; i < n; i++) {
                  File in = inputs.get(i);
                  File patched = new File(outDir, "aligned_" + in.getName());
                  File finalApk = new File(outDir, in.getName());
                  final int idx = i;

                  ApkPatcher.Progress sub =
                      (frac, msg) -> {
                        int pct = 5 + (int) (((idx + frac * 0.8f) / n) * 90);
                        postProgress(l, pct, "Patching " + in.getName());
                      };

                  totalReplaced += patcher.patch(in, patched, sub);

                  int signPct = 5 + (int) (((i + 0.85f) / n) * 90);
                  postProgress(l, signPct, "Signing " + in.getName());
                  ApkSigner.sign(ctx, patched, finalApk);

                  patched.delete();
                  in.delete();
                  signed.add(finalApk);
                }

                if (totalReplaced == 0) {
                  throw new IOException("0 files replaced, something went wrong.");
                }

                postProgress(l, 100, "Success: " + totalReplaced + " emoji replaced");
                postSuccess(l, signed);

              } catch (Throwable t) {
                postError(l, t);
              }
            });
  }
}
