/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.connection.assistant;

import com.android.tools.analytics.UsageTracker;
import com.android.tools.idea.assistant.OpenAssistSidePanelAction;
import com.google.wireless.android.sdk.stats.AndroidStudioEvent;
import com.google.wireless.android.sdk.stats.ConnectionAssistantEvent;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class OpenConnectionAssistantSidePanelAction extends OpenAssistSidePanelAction {
  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setVisible(ConnectionAssistantBundleCreator.isAssistantEnabled());
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    super.actionPerformed(event);

    UsageTracker.getInstance()
      .log(AndroidStudioEvent.newBuilder().setKind(AndroidStudioEvent.EventKind.CONNECTION_ASSISTANT_EVENT)
             .setConnectionAssistantEvent(ConnectionAssistantEvent.newBuilder()
                                            .setType(ConnectionAssistantEvent.ConnectionAssistantEventType.OPEN)));
  }
}
