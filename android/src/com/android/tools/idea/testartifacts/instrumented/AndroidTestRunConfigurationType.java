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

package com.android.tools.idea.testartifacts.instrumented;

import com.android.tools.idea.run.AndroidRunConfigurationType;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.ui.LayeredIcon;
import icons.AndroidIcons;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.android.util.AndroidCommonUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AndroidTestRunConfigurationType implements ConfigurationType {
  private static final NotNullLazyValue<Icon> ANDROID_TEST_ICON = new NotNullLazyValue<Icon>() {
    @NotNull
    @Override
    protected Icon compute() {
      LayeredIcon icon = new LayeredIcon(2);
      icon.setIcon(AndroidIcons.AndroidModule, 0);
      icon.setIcon(AllIcons.Nodes.JunitTestMark, 1);
      return icon;
    }
  };

  private final ConfigurationFactory myFactory = new AndroidRunConfigurationType.AndroidRunConfigurationFactory(this) {
    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new AndroidTestRunConfiguration(project, this);
    }
  };

  public static AndroidTestRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(AndroidTestRunConfigurationType.class);
  }

  @Override
  public String getDisplayName() {
    return AndroidBundle.message("android.test.run.configuration.type.name");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return AndroidBundle.message("android.test.run.configuration.type.description");
  }

  @Override
  public Icon getIcon() {
    return ANDROID_TEST_ICON.getValue();
  }

  @Override
  @NotNull
  public String getId() {
    return AndroidCommonUtils.ANDROID_TEST_RUN_CONFIGURATION_TYPE;
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{myFactory};
  }

  public ConfigurationFactory getFactory() {
    return myFactory;
  }
}
