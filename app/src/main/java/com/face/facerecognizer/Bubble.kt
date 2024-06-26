package com.face.facerecognizer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun FloatingDraggableItem(
    initialOffset: (FloatingDraggableItemState) -> IntOffset,
    content: @Composable BoxScope.(FloatingDraggableItemState) -> Unit,
) {
    val state = remember { mutableStateOf(FloatingDraggableItemState()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { state.updateContainerSize(size = it.size) },
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(200.dp)
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
                .offset { state.value.offset }
                .onGloballyPositioned {
                    state.updateContentSizeSize(
                        size = it.size,
                        initialOffset = initialOffset,
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val calculatedX = state.value.offset.x + dragAmount.x.roundToInt()
                        val calculatedY = state.value.offset.y + dragAmount.y.roundToInt()

                        val offset = IntOffset(
                            calculatedX.coerceIn(0, state.value.dragAreaSize.width),
                            calculatedY.coerceIn(0, state.value.dragAreaSize.height),
                        )
                        state.updateOffset(offset = offset)
                    }
                }
                .clip(shape = RoundedCornerShape(16.dp))
        ) {
            content(state.value)
        }

        DisposableEffect(true) {
            onDispose {
                val offset = initialOffset(state.value)
                state.updateOffset(offset = offset)
            }
        }
    }
}

data class FloatingDraggableItemState(
    val contentSize: IntSize = IntSize(width = 0, height = 0),
    val containerSize: IntSize = IntSize(width = 0, height = 0),
    val offset: IntOffset = IntOffset(x = 0, y = 0),
) {

    val dragAreaSize: IntSize
        get() = IntSize(
            width = containerSize.width - contentSize.width,
            height = containerSize.height - contentSize.height,
        )
}

private fun MutableState<FloatingDraggableItemState>.updateContainerSize(size: IntSize) {
    value = value.copy(containerSize = size)
}

private fun MutableState<FloatingDraggableItemState>.updateContentSizeSize(
    size: IntSize,
    initialOffset: (FloatingDraggableItemState) -> IntOffset,
) {
    val wasNotRenderedBefore = size.isNotEmpty() && value.contentSize.isEmpty()
    val offset = if (wasNotRenderedBefore) {
        val stateWithUpdatedSize = value.copy(contentSize = size)
        initialOffset(stateWithUpdatedSize)
    } else {
        value.offset
    }

    value = value.copy(contentSize = size, offset = offset)
}

private fun IntSize.isEmpty(): Boolean = this == IntSize(width = 0, height = 0)

private fun IntSize.isNotEmpty(): Boolean = !isEmpty()

private fun MutableState<FloatingDraggableItemState>.updateOffset(offset: IntOffset) {
    value = value.copy(offset = offset)
}