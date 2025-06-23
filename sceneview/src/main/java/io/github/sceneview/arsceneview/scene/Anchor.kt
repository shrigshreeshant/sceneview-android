package io.github.sceneview.arsceneview.scene

import com.google.ar.core.Anchor

fun Anchor.destroy() {
    detach()
}