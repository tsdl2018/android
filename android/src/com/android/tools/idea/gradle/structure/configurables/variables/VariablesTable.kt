/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.gradle.structure.configurables.variables

import com.android.tools.idea.gradle.dsl.api.ext.GradlePropertyModel
import com.android.tools.idea.gradle.dsl.api.ext.GradlePropertyModel.*
import com.android.tools.idea.gradle.structure.configurables.PsContext
import com.android.tools.idea.gradle.structure.model.PsModule
import com.google.common.base.Joiner
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.ui.treeStructure.treetable.TreeTableModel
import com.intellij.util.ui.AbstractTableCellEditor
import java.awt.Component
import java.awt.event.ActionListener
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.table.TableCellEditor
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

private const val NAME = 0
private const val UNRESOLVED_VALUE = 1
private const val RESOLVED_VALUE = 2

/**
 * Main table for the Variables view in the Project Structure Dialog
 */
class VariablesTable(private val project: Project, private val context: PsContext) : TreeTable(VariablesTableModel(DefaultMutableTreeNode())) {
  val variableNames = ArrayList<String>()

  init {
    fillTable()
  }

  private fun fillTable() {
    context.project.forEachModule { module ->
      val gradleModel = module.parsedModel ?: return@forEachModule
      val extModel = gradleModel.ext()
      val properties = extModel.properties
      if (!properties.isEmpty()) {
        variableNames.addAll(properties.map { property -> property.name })
        val moduleRoot = ModuleNode(module)
        (tableModel.root as DefaultMutableTreeNode).add(moduleRoot)
        for (property in properties) {
          moduleRoot.add(VariableNode(property))
        }
      }
    }
    tree.expandRow(0)
    tree.isRootVisible = false
  }

  override fun getCellEditor(row: Int, column: Int): TableCellEditor {
    if (column == UNRESOLVED_VALUE) {
      return VariableCellEditor()
    }
    return super.getCellEditor(row, column)
  }

  inner class VariableCellEditor : AbstractTableCellEditor() {
    private val textBox = VariableAwareTextBox(project)

    init {
      textBox.setVariants(variableNames)
      textBox.addTextListener(ActionListener { stopCellEditing() })
      textBox.addFocusListener(object : FocusAdapter() {
        override fun focusLost(e: FocusEvent?) {
          stopCellEditing()
          super.focusLost(e)
        }
      })
    }

    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component? {
      if (value !is String) {
        return null
      }
      textBox.text = value
      return textBox
    }

    override fun getCellEditorValue(): Any = textBox.text
  }

  class VariablesTableModel(root: TreeNode) : DefaultTreeModel(root), TreeTableModel {
    override fun getColumnCount(): Int = 3

    override fun getColumnName(column: Int): String {
      return when (column) {
        NAME -> "Name"
        UNRESOLVED_VALUE -> "Value"
        RESOLVED_VALUE -> "Resolved value"
        else -> ""
      }
    }

    override fun getColumnClass(column: Int): Class<*> {
      if (column == NAME) {
        return TreeTableModel::class.java
      }
      return String::class.java
    }

    override fun getValueAt(node: Any?, column: Int): Any? {
      if (node !is VariableNode) {
        if (column == NAME) {
          return node.toString()
        }
        return null
      }

      when (column) {
        NAME -> return node.toString()
        UNRESOLVED_VALUE -> {
          val type = node.property.valueType
          return when (type) {
            GradlePropertyModel.ValueType.MAP -> {
              val unresolvedMapValue = node.property.getRawValue(MAP_TYPE) ?: return null
              "[" + Joiner.on(", ").join(unresolvedMapValue.keys) + "]"
            }
            GradlePropertyModel.ValueType.LIST -> {
              val unresolvedListValue = node.property.getRawValue(LIST_TYPE) ?: return null
              "[" + Joiner.on(", ").join(unresolvedListValue) + "]"
            }
            GradlePropertyModel.ValueType.STRING -> {
              val unresolvedValue = node.property.getRawValue(STRING_TYPE) ?: return null
              StringUtil.wrapWithDoubleQuote(unresolvedValue)
            }
            else -> node.property.getRawValue(STRING_TYPE)
          }
        }
        RESOLVED_VALUE -> {
          val resolvedValue = node.property.getValue(STRING_TYPE) ?: return null
          if (node.property.valueType != ValueType.STRING) {
            return resolvedValue
          }
          return StringUtil.wrapWithDoubleQuote(resolvedValue)
        }
        else -> return null
      }
    }

    override fun isCellEditable(node: Any?, column: Int): Boolean {
      if (column == RESOLVED_VALUE || node !is VariableNode) {
        return false
      }
      if (column == NAME) {
        return true
      }
      val type = node.property.valueType
      return type != ValueType.MAP && type != ValueType.LIST
    }

    override fun setValueAt(aValue: Any?, node: Any?, column: Int) {
      if (aValue == null || node !is VariableNode) {
        return
      }
      if (column == UNRESOLVED_VALUE) {
        node.property.setValue(aValue)
        node.setModuleModified(true)
        nodeChanged(node)
      }
    }

    override fun setTree(tree: JTree?) {}
  }

  class VariableNode(val property: GradlePropertyModel) : DefaultMutableTreeNode(property.name) {
    fun setModuleModified(isModified: Boolean) {
      var parent: TreeNode = this.parent
      while (parent !is ModuleNode) {
        parent = parent.parent
      }
      parent.module.isModified = isModified
    }
  }

  class ModuleNode(val module: PsModule) : DefaultMutableTreeNode(module.name)
}