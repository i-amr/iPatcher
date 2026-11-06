package com.discord.patcher.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.discord.patcher.utils.FileUtils;
import com.discord.patcher.R;
import com.discord.patcher.engine.BaseProgressTask;
import com.discord.patcher.engine.PatchRunner;
import com.discord.patcher.engine.ZipManager;
import com.discord.patcher.helper.ApkInstaller;
import com.discord.patcher.helper.DiscordLocator;
import com.discord.patcher.utils.SignatureCheck;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {
  private PatchRunner runner;
  private ZipManager zipManager;
  private ProgressBar bar;
  private TextView status;
  private ImageButton btnClean;
  private Button btnPatch, btnImport, btnInstall, btnExport;
  private List<File> result;

  private File exportedCacheFile;

  private boolean fromApks, isRunning;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    bar = findViewById(R.id.bar);
    status = findViewById(R.id.status);
    btnClean = findViewById(R.id.btnClean);
    btnPatch = findViewById(R.id.btnPatch);
    btnImport = findViewById(R.id.btnImport);
    btnInstall = findViewById(R.id.btnInstall);
    btnExport = findViewById(R.id.btnExport);

    result = FileUtils.getApks(false);

    runner = new PatchRunner(this);
    zipManager = new ZipManager();

    btnClean.setOnClickListener(
        v -> {
          if (isRunning) {
            status.setText("Please wait for the current process to be completed.");
            return;
          }
          new Thread(
                  () -> {
                    isRunning = true;
                    FileUtils.cleanWorkingDir();
                    runOnUiThread(
                        () -> {
                          isRunning = false;
                          status.setText("Done. Cache has been cleaned up.");
                        });
                  })
              .start();
        });

    btnPatch.setOnClickListener(
        v -> {
          if (isRunning) {
            status.setText("Please wait for the current process to be completed.");
            return;
          }

          if (!fromApks && !DiscordLocator.hasInstalledDiscord()) {
            status.setText("Error: Discord was not found, Try Import.");
            return;
          }

          isRunning = true;
          updateButtons(btnPatch, false);
          bar.setVisibility(View.VISIBLE);

          runner.start(
              new BaseProgressTask.TaskCallback<List<File>>() {
                @Override
                public void onProgress(int p, String m) {
                  bar.setProgress(p);
                  status.setText(m);
                }

                @Override
                public void onSuccess(List<File> apks) {
                  result = apks;
                  bar.setVisibility(View.INVISIBLE);

                  if (DiscordLocator.hasInstalledDiscord()
                      && !SignatureCheck.isSignatureMatched(DiscordLocator.PKG)) {
                    status.setText("Done. Discord must be uninstalled first.");
                    ApkInstaller.uninstall(MainActivity.this);
                  } else {
                    status.setText("Done. Discord is ready to be installed.");
                    btnInstall.performClick();
                  }

                  updateButtons(btnPatch, true);
                }

                @Override
                public void onError(Throwable t) {
                  Log.e("Patcher", "failed", t);
                  status.setText("Error: " + t.getMessage());
                  bar.setVisibility(View.INVISIBLE);

                  updateButtons(btnPatch, true);
                  setButtonEnabled(btnInstall, false);
                  setButtonEnabled(btnExport, false);
                }
              },
              fromApks);

          isRunning = false;
        });

    btnImport.setOnClickListener(
        v -> {
          if (isRunning) {
            status.setText("Please wait for the current process to be completed.");
            return;
          }

          isRunning = true;
          updateButtons(btnImport, false);
          FileUtils.pickApk(this);
          isRunning = false;
        });

    btnInstall.setOnClickListener(
        v -> {
          if (isRunning) {
            status.setText("Please wait for the current process to be completed.");
            return;
          }

          if (FileUtils.getApks(false) == null) {
            status.setText("Patch the app first");
            return;
          }

          isRunning = true;
          updateButtons(btnInstall, false);

          new Thread(
                  () -> {
                    final StringBuilder sb = new StringBuilder();
                    try {
                      ApkInstaller.install(result);
                    } catch (IOException e) {
                      sb.append("Install error: ").append(e.getMessage());
                    }

                    runOnUiThread(
                        () -> {
                          if (sb.length() > 0) status.setText(sb.toString());
                          isRunning = false;
                          updateButtons(btnInstall, true);
                        });
                  })
              .start();
        });

    File zipFile = new File(FileUtils.getWorkingDir(), "dst.apks"); // data/cache/work/dst.apks

    btnExport.setOnClickListener(
        v -> {
          if (isRunning) {
            status.setText("Please wait for the current process to be completed.");
            return;
          }

          if (zipFile.exists()) {
            exportedCacheFile = zipFile;
            FileUtils.createApk(MainActivity.this, "Discord.apks");
            return;
          } else if (FileUtils.getApks(false) == null) {
            status.setText("Patch the app first");
            return;
          }

          isRunning = true;
          updateButtons(btnExport, false);
          bar.setVisibility(View.VISIBLE);

          zipManager.start(
              result,
              zipFile,
              new BaseProgressTask.TaskCallback<File>() {
                @Override
                public void onProgress(int p, String m) {
                  bar.setProgress(p);
                  status.setText(m);
                }

                @Override
                public void onSuccess(File content) {
                  bar.setVisibility(View.INVISIBLE);

                  exportedCacheFile = content;
                  FileUtils.createApk(MainActivity.this, "Discord.apks");

                  status.setText("Select where you want to save Discord.apks...");
                  updateButtons(btnExport, true);
                }

                @Override
                public void onError(Throwable t) {
                  bar.setVisibility(View.INVISIBLE);
                  status.setText("Export error: " + t.getMessage());
                  updateButtons(btnExport, true);
                }
              });

          isRunning = false;
        });
  }

  private void updateButtons(Button button, boolean enabled) {
    switch (button.getId()) {
      case R.id.btnPatch:
        btnPatch.setText(enabled ? "Patch" : "Patching...");
        break;
      case R.id.btnImport:
        btnImport.setText(enabled ? "Import" : "Importing...");
        break;
      case R.id.btnInstall:
        btnInstall.setText(enabled ? "Install" : "Installing...");
        break;
      case R.id.btnExport:
        btnExport.setText(enabled ? "Export" : "Exporting...");
        break;
    }
    setButtonEnabled(button, enabled);
    btnPatch.setClickable(enabled);
    btnImport.setClickable(enabled);
    btnInstall.setClickable(enabled);
    btnExport.setClickable(enabled);
  }

  public static void setButtonEnabled(Button button, boolean enabled) {
    button.setEnabled(enabled);
    button.setAlpha(enabled ? 1 : .5f);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == RESULT_OK && data != null && data.getData() != null) {
      if (requestCode == FileUtils.ACTION_CREATE) {

        if (exportedCacheFile != null && exportedCacheFile.exists()) {
          Uri targetUri = data.getData();

          new Thread(
                  () -> {
                    try {
                      FileUtils.writeUri(exportedCacheFile, targetUri);
                      runOnUiThread(() -> status.setText("Discord.apks successfully exported!"));

                      exportedCacheFile.delete();
                    } catch (IOException e) {
                      runOnUiThread(() -> status.setText("Write failed: " + e.getMessage()));
                    }
                  })
              .start();
        } else {
          status.setText("Export failed: Temporary cache file missing.");
        }
      } else if (requestCode == FileUtils.ACTION_PICK) {
        Uri selectedApkUri = data.getData();

        status.setText("Importing installer bundle...");
        File src = new File(FileUtils.getWorkingDir(), "src.apks"); // data/cache/work/src.apks

        setButtonEnabled(btnImport, false);
        bar.setVisibility(View.VISIBLE);

        new Thread(
                () -> {
                  try {
                    FileUtils.readUri(selectedApkUri, src);

                    zipManager.decompress(
                        src,
                        FileUtils.getInputDir(),
                        new BaseProgressTask.TaskCallback<File>() {
                          @Override
                          public void onProgress(int p, String m) {
                            bar.setProgress(p);
                            status.setText(m);
                          }

                          @Override
                          public void onSuccess(File content) {
                            bar.setVisibility(View.INVISIBLE);
                            updateButtons(btnImport, true);
                            fromApks = true;
                            btnPatch.performClick();
                            fromApks = false;
                          }

                          @Override
                          public void onError(Throwable t) {
                            bar.setVisibility(View.INVISIBLE);
                            status.setText("Import error: " + t.getMessage());
                            updateButtons(btnImport, true);
                          }
                        });
                  } catch (IOException e) {
                    runOnUiThread(() -> status.setText("Failed to read file: " + e.getMessage()));
                  }

                  runOnUiThread(
                      () -> {
                        status.setText("Import complete! Ready to patch.");
                      });
                })
            .start();
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    runner.cancel();
  }
}
