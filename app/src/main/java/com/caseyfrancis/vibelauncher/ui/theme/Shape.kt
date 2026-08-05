package com.caseyfrancis.vibelauncher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CardCornerShape = RoundedCornerShape(20.dp)
val TileCornerShape = RoundedCornerShape(16.dp)
val BadgeCornerShape = RoundedCornerShape(50)

val LauncherShapes = Shapes(
    small = TileCornerShape,
    medium = CardCornerShape,
    large = CardCornerShape
)
