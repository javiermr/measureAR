/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package  com.develop.common.helpers;

import android.app.Activity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.snackbar.BaseTransientBottomBar;

/**
 * Helper to manage the sample snackbar. Hides the Android boilerplate code, and exposes simpler
 * methods.
 */
public final class SandbarHelper {
  private static final int BACKGROUND_COLOR = 0xbf323232;
  private Snackbar messageSnackbar;
  private enum DismissBehavior { HIDE, FINISH };

    /**
   * Shows a snackbar with a given error message. When dismissed, will finish the activity. Useful
   * for notifying errors, where no further interaction with the activity is possible.
   */
  public void showError(Activity activity, String errorMessage) {
    show(activity, errorMessage);
  }

    private void show(
            final Activity activity, final String message) {
    activity.runOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            messageSnackbar =
                Snackbar.make(
                    activity.findViewById(android.R.id.content),
                    message,
                    Snackbar.LENGTH_INDEFINITE);
            messageSnackbar.getView().setBackgroundColor(BACKGROUND_COLOR);
            if (DismissBehavior.FINISH != DismissBehavior.HIDE) {
              messageSnackbar.setAction(
                  "Dismiss",
                      v -> messageSnackbar.dismiss());
              if (DismissBehavior.FINISH == DismissBehavior.FINISH) {
                messageSnackbar.addCallback(
                    new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                      @Override
                      public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        activity.finish();
                      }
                    });
              }
            }
            messageSnackbar.show();
          }
        });
  }
}
