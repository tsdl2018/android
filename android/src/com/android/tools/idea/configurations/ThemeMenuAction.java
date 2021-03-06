/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.tools.idea.configurations;

import com.android.tools.adtui.actions.DropDownAction;
import com.android.tools.idea.res.ResourceHelper;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import icons.StudioIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThemeMenuAction extends DropDownAction {
  private final ConfigurationHolder myRenderContext;

  public ThemeMenuAction(@NotNull ConfigurationHolder renderContext) {
    super("", "Theme in Editor", StudioIcons.LayoutEditor.Toolbar.THEME_BUTTON);
    myRenderContext = renderContext;
  }

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    updatePresentation(e.getPresentation());
  }

  @Override
  public boolean displayTextInToolbar() {
    return true;
  }

  private void updatePresentation(Presentation presentation) {
    Configuration configuration = myRenderContext.getConfiguration();
    boolean visible = configuration != null;
    if (visible) {
      String brief = getThemeLabel(configuration.getTheme(), true);

      // The tests only have access to the template presentation and not the actual presentation of the
      // ActionButtonWithText that is create for this action
      // This is a little hack since the text displayed is taken from the a Presentation that might no be the same as template one.
      // The order is also important. If the text is set on the template presentation after the current presentation,
      // the button disappear (Intellij Actions magic)
      getTemplatePresentation().setText(brief, false);
      presentation.setText(brief, false);
    }
    if (visible != presentation.isVisible()) {
      presentation.setVisible(visible);
    }
  }

  /**
   * Returns a suitable label to use to display the given theme
   *
   * @param theme the theme to produce a label for
   * @param brief if true, generate a brief label (suitable for a toolbar
   *              button), otherwise a fuller name (suitable for a menu item)
   * @return the label
   */
  @NotNull
  public static String getThemeLabel(@Nullable String theme, boolean brief) {
    if (theme == null) {
      return "";
    }
    theme = ResourceHelper.styleToTheme(theme);

    if (brief) {
      int index = theme.lastIndexOf('.');
      if (index < theme.length() - 1) {
        return theme.substring(index + 1);
      }
    }
    return theme;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Configuration configuration = myRenderContext.getConfiguration();
    if (configuration != null) {
      ThemeSelectionDialog dialog = new ThemeSelectionDialog(configuration);
      if (dialog.showAndGet()) {
        String theme = dialog.getTheme();
        if (theme != null) {
          configuration.setTheme(theme);
        }
      }
    }
  }
}
