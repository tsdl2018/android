/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.gradle.structure.configurables.android.dependencies;

import com.android.tools.idea.gradle.structure.configurables.PsContext;
import com.android.tools.idea.gradle.structure.configurables.android.dependencies.details.DependencyDetails;
import com.android.tools.idea.gradle.structure.model.PsProject;
import com.android.tools.idea.gradle.structure.model.android.PsAndroidDependency;
import com.android.tools.idea.gradle.structure.model.android.PsAndroidModule;
import com.android.tools.idea.structure.dialog.Header;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.SideBorder;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.navigation.History;
import com.intellij.ui.navigation.Place;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.intellij.ui.IdeBorderFactory.createEmptyBorder;
import static com.intellij.ui.ScrollPaneFactory.createScrollPane;
import static com.intellij.util.PlatformIcons.LIBRARY_ICON;
import static com.intellij.util.ui.UIUtil.getInactiveTextColor;

public abstract class AbstractDeclaredDependenciesPanel extends JPanel implements Place.Navigator, Disposable {
  @NotNull private final PsContext myContext;
  @NotNull private final PsProject myProject;
  @NotNull private final EmptyDetailsPanel myEmptyDetailsPanel;
  @NotNull private final JScrollPane myDetailsScrollPane;
  @NotNull private final JPanel myContentsPanel;
  @NotNull private final String myEmptyText;

  @NotNull private final Map<Class<?>, DependencyDetails> myDependencyDetails = Maps.newHashMap();

  @Nullable private final PsAndroidModule myModule;

  private List<AbstractPopupAction> myPopupActions;
  private DependencyDetails myCurrentDependencyDetails;
  private History myHistory;

  protected AbstractDeclaredDependenciesPanel(@NotNull String title,
                                              @NotNull PsContext context,
                                              @NotNull PsProject project,
                                              @Nullable PsAndroidModule module) {
    super(new BorderLayout());
    myContext = context;
    myProject = project;
    myModule = module;

    myEmptyText = String.format("Please select a dependency from the '%1$s' view", title);

    myEmptyDetailsPanel = new EmptyDetailsPanel(myEmptyText);
    myDetailsScrollPane = createScrollPane(myEmptyDetailsPanel);
    myDetailsScrollPane.setBorder(createEmptyBorder());

    Header header = new Header(title);
    add(header, BorderLayout.NORTH);

    OnePixelSplitter splitter = new OnePixelSplitter(true, "psd.editable.dependencies.main.horizontal.splitter.proportion", 0.75f);

    myContentsPanel = new JPanel(new BorderLayout());

    splitter.setFirstComponent(myContentsPanel);
    splitter.setSecondComponent(myDetailsScrollPane);

    add(splitter, BorderLayout.CENTER);
  }

  protected final void addDetails(@NotNull DependencyDetails<?> details) {
    myDependencyDetails.put(details.getSupportedModelType(), details);
  }

  protected void updateDetails(@Nullable PsAndroidDependency selected) {
    if (selected != null) {
      myCurrentDependencyDetails = myDependencyDetails.get(selected.getClass());
      if (myCurrentDependencyDetails != null) {
        myDetailsScrollPane.setViewportView(myCurrentDependencyDetails.getPanel());
        //noinspection unchecked
        myCurrentDependencyDetails.display(selected);
        return;
      }
    }
    myCurrentDependencyDetails = null;
    myDetailsScrollPane.setViewportView(myEmptyDetailsPanel);
  }

  @Nullable
  protected DependencyDetails getCurrentDependencyDetails() {
    return myCurrentDependencyDetails;
  }

  @NotNull
  protected final JPanel createActionsPanel() {
    final JPanel actionsPanel = new JPanel(new BorderLayout());

    DefaultActionGroup actions = new DefaultActionGroup();

    AnAction addDependencyAction = new DumbAwareAction("Add Dependency", "", IconUtil.getAddIcon()) {
      @Override
      public void actionPerformed(AnActionEvent e) {
        initPopupActions();
        JBPopup popup = JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<AbstractPopupAction>(null, myPopupActions) {
          @Override
          public Icon getIconFor(AbstractPopupAction action) {
            return action.icon;
          }

          @Override
          public boolean isMnemonicsNavigationEnabled() {
            return true;
          }

          @Override
          public PopupStep onChosen(final AbstractPopupAction action, boolean finalChoice) {
            return doFinalStep(new Runnable() {
              @Override
              public void run() {
                action.execute();
              }
            });
          }

          @Override
          @NotNull
          public String getTextFor(AbstractPopupAction action) {
            return "&" + action.index + "  " + action.text;
          }
        });
        popup.show(new RelativePoint(actionsPanel, new Point(0, actionsPanel.getHeight() - 1)));
      }
    };

    actions.add(addDependencyAction);
    List<AnAction> extraToolbarActions = getExtraToolbarActions();
    if (!extraToolbarActions.isEmpty()) {
      actions.addSeparator();
      actions.addAll(extraToolbarActions);
    }

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("TOP", actions, true);
    JComponent toolbarComponent = toolbar.getComponent();
    toolbarComponent.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
    actionsPanel.add(toolbarComponent, BorderLayout.CENTER);

    return actionsPanel;
  }

  @NotNull
  protected List<AnAction> getExtraToolbarActions() {
    return Collections.emptyList();
  }

  private void initPopupActions() {
    if (myPopupActions == null) {
      List<AbstractPopupAction> actions = Lists.newArrayList();
      actions.add(new AddDependencyAction());
      myPopupActions = actions;
    }
  }

  @NotNull
  protected JPanel getContentsPanel() {
    return myContentsPanel;
  }

  @NotNull
  protected PsContext getContext() {
    return myContext;
  }

  @NotNull
  public String getEmptyText() {
    return myEmptyText;
  }

  @Override
  public void setHistory(History history) {
    myHistory = history;
  }

  @Nullable
  protected History getHistory() {
    return myHistory;
  }

  private class AddDependencyAction extends AbstractPopupAction {
    AddDependencyAction() {
      super("Artifact Dependency", LIBRARY_ICON, 1);
    }

    @Override
    void execute() {
      AddArtifactDependencyDialog dialog;
      if (myModule == null) {
        dialog = new AddArtifactDependencyDialog(myProject);
      }
      else {
        dialog = new AddArtifactDependencyDialog(myModule);
      }
      dialog.showAndGet();
    }
  }

  private static abstract class AbstractPopupAction implements ActionListener {
    @NotNull final String text;
    @NotNull final Icon icon;

    final int index;

    AbstractPopupAction(@NotNull String text, @NotNull Icon icon, int index) {
      this.text = text;
      this.icon = icon;
      this.index = index;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      execute();
    }

    abstract void execute();
  }

  private static class EmptyDetailsPanel extends JPanel {
    EmptyDetailsPanel(@NotNull String text) {
      super(new BorderLayout());
      JBLabel emptyText = new JBLabel(text);
      emptyText.setForeground(getInactiveTextColor());
      emptyText.setHorizontalAlignment(SwingConstants.CENTER);
      add(emptyText, BorderLayout.CENTER);
    }
  }
}
