package com.discord.patcher.engine;

import android.os.Handler;
import android.os.Looper;

public abstract class BaseProgressTask<T> {

  public interface TaskCallback<T> {
    void onProgress(int percent, String message);
    void onSuccess(T result);
    void onError(Throwable t);
  }

  protected final Handler mainHandler = new Handler(Looper.getMainLooper());

  protected final void postProgress(TaskCallback<T> listener, int percent, String message) {
    if (listener != null) {
      mainHandler.post(() -> listener.onProgress(percent, message));
    }
  }

  protected final void postError(TaskCallback<T> listener, Throwable t) {
    if (listener != null) {
      mainHandler.post(() -> listener.onError(t));
    }
  }

  protected final void postSuccess(TaskCallback<T> listener, T result) {
    if (listener != null) {
      mainHandler.post(() -> listener.onSuccess(result));
    }
  }
}
