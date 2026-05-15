package com.opclient.icon

import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    val outDir = File(args[0])
    outDir.mkdirs()

    val size = 512
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    // Background circle — DarkColors.background (#111C10)
    g.color = Color(0x11, 0x1C, 0x10)
    g.fillOval(26, 26, 460, 460)

    val identity = g.transform

    // Left page — rotated -4° around its center (172, 255)
    // DarkColors.textPrimary (#E4EDE2)
    g.color = Color(0xE4, 0xED, 0xE2)
    g.rotate(Math.toRadians(-4.0), 172.0, 255.0)
    g.fill(RoundRectangle2D.Float(80f, 130f, 185f, 250f, 12f, 12f))

    // Text lines on left page — DarkColors.textSecondary (#7A9C76)
    g.color = Color(0x7A, 0x9C, 0x76)
    listOf(Triple(105, 195, 140), Triple(105, 225, 120), Triple(105, 255, 130))
        .forEach { (x, y, w) -> g.fillRoundRect(x, y, w, 10, 4, 4) }
    g.transform = identity

    // Right page — rotated +4° around its center (340, 255)
    g.color = Color(0xE4, 0xED, 0xE2)
    g.rotate(Math.toRadians(4.0), 340.0, 255.0)
    g.fill(RoundRectangle2D.Float(247f, 130f, 185f, 250f, 12f, 12f))

    // Text lines on right page
    g.color = Color(0x7A, 0x9C, 0x76)
    listOf(Triple(267, 195, 140), Triple(267, 225, 120), Triple(267, 255, 100))
        .forEach { (x, y, w) -> g.fillRoundRect(x, y, w, 10, 4, 4) }
    g.transform = identity

    // Spine — DarkColors.accent (#6AB874)
    g.color = Color(0x6A, 0xB8, 0x74)
    g.fillRoundRect(245, 120, 22, 270, 6, 6)

    g.dispose()
    val out = File(outDir, "op_client.png")
    ImageIO.write(img, "PNG", out)
    println("Icon written to ${out.absolutePath}")
}
