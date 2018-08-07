package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionHandler
import com.maddyhome.idea.vim.extension.VimNonDisposableExtension
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.CaretData
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys

private const val NEXT_OCCURRENCE = "<Plug>NextOccurrence"
private const val SKIP_OCCURRENCE = "<Plug>SkipOccurrence"
private const val REMOVE_OCCURRENCE = "<Plug>RemoveOccurrence"
private const val ALL_OCCURRENCES = "<Plug>AllOccurrences"

class VimMultipleCursorsExtension : VimNonDisposableExtension() {
  override fun getName() = "multiple-cursors"

  override fun initOnce() {
    putExtensionHandlerMapping(MappingMode.NVO, parseKeys(NEXT_OCCURRENCE), NextOccurrenceHandler(), false)
    putExtensionHandlerMapping(MappingMode.NVO, parseKeys(ALL_OCCURRENCES), AllOccurrencesHandler(), false)
    putExtensionHandlerMapping(MappingMode.V, parseKeys(SKIP_OCCURRENCE), SkipOccurrenceHandler(), false)
    putExtensionHandlerMapping(MappingMode.V, parseKeys(REMOVE_OCCURRENCE), RemoveOccurrenceHandler(), false)

    putKeyMapping(MappingMode.NVO, parseKeys("<A-n>"), parseKeys(NEXT_OCCURRENCE), true)
    putKeyMapping(MappingMode.V, parseKeys("<A-x>"), parseKeys(SKIP_OCCURRENCE), true)
    putKeyMapping(MappingMode.V, parseKeys("<A-p>"), parseKeys(REMOVE_OCCURRENCE), true)
  }

  private class NextOccurrenceHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      val offset = VimPlugin.getMotion().selectNextOccurrence(editor)
      if (offset != -1) {
        MotionGroup.moveCaret(editor, editor.caretModel.primaryCaret, offset, true)
      }
    }
  }

  private class AllOccurrencesHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      TODO("not implemented")
    }
  }

  private class SkipOccurrenceHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      val offset = VimPlugin.getMotion().skipCurrentOccurrence(editor)
      if (offset != -1) {
        MotionGroup.moveCaret(editor, editor.caretModel.primaryCaret, offset, true)
      }
    }

  }

  private class RemoveOccurrenceHandler : VimExtensionHandler {
    override fun execute(editor: Editor, context: DataContext) {
      editor.selectionModel.removeSelection()
      editor.caretModel.removeCaret(editor.caretModel.primaryCaret)
    }
  }
}

fun MotionGroup.selectNextOccurrence(editor: Editor): Int {
  val caretModel = editor.caretModel
  val primaryCaret = caretModel.primaryCaret
  val nextOffset = VimPlugin.getSearch().searchNext(editor, primaryCaret, 1)
  if (nextOffset == -1) return nextOffset

  val state = CommandState.getInstance(editor)
  if (caretModel.caretCount == 1 && state.mode != CommandState.Mode.VISUAL) {
    primaryCaret.moveToOffset(nextOffset)
    state.pushState(CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER, MappingMode.VISUAL)
    return addNewSelection(editor)
  }

  caretModel.addCaret(editor.offsetToVisualPosition(nextOffset), true) ?: return -1
  return addNewSelection(editor)
}

internal fun MotionGroup.addNewSelection(editor: Editor, caret: Caret = editor.caretModel.primaryCaret): Int {
  val range = SearchHelper.findWordUnderCursor(editor, caret) ?: return -1
  val startOffset = range.startOffset
  val endOffset = range.endOffset - 1
  CaretData.setVisualStart(caret, startOffset)
  updateSelection(editor, caret, endOffset)
  editor.scrollingModel.scrollToCaret(ScrollType.CENTER)

  return endOffset
}

fun MotionGroup.skipCurrentOccurrence(editor: Editor): Int {
  val primaryCaret = editor.caretModel.primaryCaret
  val nextOffset = VimPlugin.getSearch().searchNext(editor, primaryCaret, 1)
  if (nextOffset == -1) return nextOffset

  primaryCaret.moveToOffset(nextOffset)
  return addNewSelection(editor, primaryCaret)
}