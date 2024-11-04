package com.kaleksandra.alphanimation

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kaleksandra.coretheme.AppTheme
import com.kaleksandra.coreui.compose.painter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Scaffold {
                    Main()
                }
            }
        }
    }

    @Composable
    fun Main() {
        val frames = remember { mutableStateListOf<List<DrawingItems>>() }
        val savedActions = remember { mutableStateListOf<DrawingItems>() }
        val removedNextActions = remember { mutableStateListOf<DrawingItems>() }
        var last by remember { mutableStateOf<List<DrawingItems>>(emptyList()) }
        val lastFrame = last.map { updateLastColor(it) }
        val color = remember { mutableStateOf(Color.Black) }
        var isAnimationStart by remember { mutableStateOf(false) }
        var animationSpeed by remember { mutableStateOf(3f) }
        var isFrameShows by remember { mutableStateOf(false) }
        var drawingLayoutSize by remember { mutableStateOf(IntSize.Zero) }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var pencilSize by remember { mutableStateOf(5f) }
        var bottomAction by remember { mutableStateOf(BottomAction.INIT) }
        var paintType by remember { mutableStateOf(PaintType.BRUSH) }
        LaunchedEffect(isAnimationStart) {
            if (isAnimationStart) {
                while (true) {
                    frames.forEach {
                        savedActions.clear()
                        savedActions.addAll(it)
                        delay(200 / animationSpeed.toLong())
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PaintActionBar(
                isAnimationStart = isAnimationStart,
                onLastClick = {
                    savedActions.removeLastOrNull()?.let { removedNextActions.add(it) }
                },
                isLastActive = savedActions.isNotEmpty(),
                onNextClick = {
                    removedNextActions.removeLastOrNull()?.let { savedActions.add(it) }
                },
                isNextActive = removedNextActions.isNotEmpty(),
                onDeleteClick = {
                    savedActions.clear()
                    removedNextActions.clear()
                    frames.removeLastOrNull()?.let { savedActions.addAll(it) }
                    last = frames.lastOrNull() ?: emptyList()
                },
                onCreateFrameClick = {
                    val list = savedActions.toList()
                    frames.add(list)
                    last = list
                    savedActions.clear()
                    coroutineScope.launch {
                        listState.animateScrollToItem(frames.size - 1)
                    }
                },
                onCopyFrame = {
                    val list = savedActions.toList()
                    frames.add(list)
                    last = list
                    savedActions.clear()
                    savedActions.addAll(list)
                },
                onLayersClick = {
                    isFrameShows = !isFrameShows
                },
                onPauseClick = {
                    savedActions.clear()
                    frames.removeLastOrNull()?.let { savedActions.addAll(it) }
                    last = frames.lastOrNull() ?: emptyList()
                    isAnimationStart = false
                },
                onPlayClick = {
                    if (!isAnimationStart) {
                        frames.add(savedActions.toList())
                    }
                    isAnimationStart = true
                },
                onDeleteAll = {
                    frames.clear()
                    savedActions.clear()
                    removedNextActions.clear()
                    last = emptyList()
                }
            )
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                PaintCanvas(
                    frameCount = frames.size,
                    isAnimationStart = isAnimationStart,
                    saved = savedActions,
                    lastFrame = lastFrame,
                    color = color.value,
                    saveDrawingItems = { savedActions.add(it) },
                    onDraw = { },
                    setLayoutSize = { drawingLayoutSize = it },
                    isFrameShows = isFrameShows,
                    listState = listState,
                    frames = frames,
                    drawingLayoutSize = drawingLayoutSize,
                    pencilSize = pencilSize,
                    paintType = paintType,
                )
                BottomTabBar(
                    speed = animationSpeed,
                    onSpeedChanged = { animationSpeed = it },
                    isAnimationStart = isAnimationStart,
                    onBrushClick = {
                        bottomAction =
                            if (bottomAction != BottomAction.GENERATOR) BottomAction.GENERATOR else BottomAction.INIT
                    },
                    onEraseClick = {
                        color.value = Color.Transparent
                        bottomAction =
                            if (bottomAction != BottomAction.ERASE) BottomAction.ERASE else BottomAction.INIT
                    },
                    onInstrumentsClick = {
                        bottomAction =
                            if (bottomAction != BottomAction.FORM) BottomAction.FORM else BottomAction.INIT
                    },
                    onColorClick = { color.value = it },
                    onPaletteClick = {
                        bottomAction =
                            if (bottomAction != BottomAction.COLOR) BottomAction.COLOR else BottomAction.INIT
                    },
                    color = color.value,
                    paintType = paintType,
                    onPencilClick = {
                        bottomAction =
                            if (bottomAction != BottomAction.BRUSH) BottomAction.BRUSH else BottomAction.INIT
                    },
                    bottomAction = bottomAction,
                    pencilSize = pencilSize,
                    onPencilSize = { pencilSize = it },
                    onPaintTypeChange = { paintType = it },
                    onGenerateClick = {
                        repeat(it) {
                            val generatedList = generateNForms(
                                10,
                                drawingLayoutSize.width.toFloat(),
                                drawingLayoutSize.height.toFloat()
                            )
                            frames.add(generatedList)
                            last = generatedList
                            savedActions.clear()
                            coroutineScope.launch {
                                listState.animateScrollToItem(frames.size - 1)
                            }
                        }
                    }
                )
            }
        }
    }

    private fun updateLastColor(it: DrawingItems): DrawingItems {
        return when (it) {
            is DrawingItems.Points -> it.copy(it.points.map { point ->
                point.copy(
                    color = point.color.copy(
                        alpha = 0.5f
                    )
                )
            })

            is DrawingItems.DrawingPath -> it.copy(
                color = if (it.color != Color.Transparent) it.color.copy(
                    alpha = 0.5f
                ) else it.color
            )

            is DrawingItems.Circle -> it.copy(
                color = if (it.color != Color.Transparent) it.color.copy(
                    alpha = 0.5f
                ) else it.color
            )

            is DrawingItems.Square -> it.copy(
                color = if (it.color != Color.Transparent) it.color.copy(
                    alpha = 0.5f
                ) else it.color
            )

            is DrawingItems.Triangle -> it.copy(
                color = if (it.color != Color.Transparent) it.color.copy(
                    alpha = 0.5f
                ) else it.color
            )

            is DrawingItems.Arrow -> it.copy(
                color = if (it.color != Color.Transparent) it.color.copy(
                    alpha = 0.5f
                ) else it.color
            )
        }
    }

    @Composable
    private fun BoxScope.Frames(
        isFrameShows: Boolean,
        isAnimationStart: Boolean,
        listState: LazyListState,
        frames: List<List<DrawingItems>>,
        drawingLayoutSize: IntSize,
    ) {
        if (isFrameShows && !isAnimationStart) {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 32.dp, vertical = 6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(6.dp, 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                state = listState,
            ) {
                items(frames) {
                    Box(
                        modifier = Modifier
                            .size(30.dp, 40.dp)
                    ) {
                        Image(
                            painter(R.drawable.background_canvas),
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = "Canvas",
                            contentScale = ContentScale.FillBounds
                        )
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { }
                        ) {
                            onDrawItems(
                                it.map {
                                    when (it) {
                                        is DrawingItems.Arrow -> {
                                            DrawingItems.Arrow(
                                                point1 = Offset(
                                                    it.point1.x * (30.dp.toPx() / drawingLayoutSize.width),
                                                    it.point1.y * (40.dp.toPx() / drawingLayoutSize.height)
                                                ),
                                                point2 = Offset(
                                                    it.point2.x * (30.dp.toPx() / drawingLayoutSize.width),
                                                    it.point2.y * (40.dp.toPx() / drawingLayoutSize.height)
                                                ),
                                                point3 = Offset(
                                                    it.point3.x * (30.dp.toPx() / drawingLayoutSize.width),
                                                    it.point3.y * (40.dp.toPx() / drawingLayoutSize.height)
                                                ),
                                                point4 = Offset(
                                                    it.point4.x * (30.dp.toPx() / drawingLayoutSize.width),
                                                    it.point4.y * (40.dp.toPx() / drawingLayoutSize.height)
                                                ),
                                                color = it.color,
                                                size = it.size * (30.dp.toPx() / drawingLayoutSize.width
                                                        )
                                            )
                                        }

                                        is DrawingItems.Circle -> {
                                            DrawingItems.Circle(
                                                radius = it.radius * (30.dp.toPx() / drawingLayoutSize.width),
                                                center = Offset(
                                                    it.center.x * (30.dp.toPx() / drawingLayoutSize.width),
                                                    it.center.y * (40.dp.toPx() / drawingLayoutSize.height)
                                                ),
                                                color = it.color,
                                                size = it.size * (30.dp.toPx() / drawingLayoutSize.width)
                                            )
                                        }

                                        is DrawingItems.DrawingPath -> {
                                            DrawingItems.DrawingPath(
                                                scalePath(
                                                    it.path,
                                                    30.dp.toPx() / drawingLayoutSize.width,
                                                    40.dp.toPx() / drawingLayoutSize.height,
                                                ),
                                                it.color,
                                                it.size * (30.dp.toPx() / drawingLayoutSize.width)
                                            )
                                        }

                                        is DrawingItems.Points -> it
                                        is DrawingItems.Square -> {
                                            DrawingItems.Square(
                                                topLeft = Offset(
                                                    it.topLeft.x * (30.dp.toPx() / drawingLayoutSize.width),
                                                    it.topLeft.y * (40.dp.toPx() / drawingLayoutSize.height)
                                                ),
                                                size = it.size * (30.dp.toPx() / drawingLayoutSize.width),
                                                color = it.color,
                                                borderSize = it.borderSize * (30.dp.toPx() / drawingLayoutSize.width)
                                            )
                                        }

                                        is DrawingItems.Triangle -> {
                                            DrawingItems.Triangle(
                                                point1 = Offset(
                                                    it.point1.x * (30.dp.toPx() / drawingLayoutSize.width),
                                                    it.point1.y * (40.dp.toPx() / drawingLayoutSize.height)
                                                ),
                                                point2 = Offset(
                                                    it.point2.x * (30.dp.toPx() / drawingLayoutSize.width),
                                                    it.point2.y * (40.dp.toPx() / drawingLayoutSize.height)
                                                ),
                                                point3 = Offset(
                                                    it.point3.x * (30.dp.toPx() / drawingLayoutSize.width),
                                                    it.point3.y * (40.dp.toPx() / drawingLayoutSize.height)
                                                ),
                                                color = it.color,
                                                size = it.size * (30.dp.toPx() / drawingLayoutSize.width)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    //TODO: Перенести во ViewModel
    private fun generateNForms(count: Int, width: Float, height: Float): List<DrawingItems> {
        val borderSize = Random.nextInt(5, 20).toFloat()
        val formSize = Random.nextInt(20, 100).toFloat()
        val result = mutableListOf<DrawingItems>()
        val list = listOf("SQUARE", "CIRCLE", "TRIANGLE")
        repeat(count) {
            val offset = Offset(Random.nextFloat() * (width), Random.nextFloat() * (height))
            val color = getRandomColor()
            val type = list.random()
            val frame = when (type) {
                "SQUARE" -> {
                    DrawingItems.Square(
                        topLeft = offset - Offset(
                            formSize / 2,
                            formSize / 2
                        ),
                        size = formSize,
                        color = color,
                        borderSize = borderSize
                    )
                }

                "CIRCLE" -> {
                    DrawingItems.Circle(
                        center = offset,
                        radius = formSize,
                        color = color,
                        size = borderSize
                    )
                }

                "TRIANGLE" -> {
                    DrawingItems.Triangle(
                        point1 = offset + Offset(0f, -formSize),
                        point2 = offset + Offset(formSize, formSize),
                        point3 = offset + Offset(-formSize, formSize),
                        color = color,
                        size = borderSize
                    )
                }

                else -> null
            }
            frame?.let {
                result.add(it)
            }
        }
        return result
    }

    private fun getRandomColor(): Color {
        val red = Random.nextInt(256)
        val green = Random.nextInt(256)
        val blue = Random.nextInt(256)
        val alpha = 255
        return Color(red, green, blue, alpha)
    }

    private fun scalePath(path: Path, scaleX: Float, scaleY: Float): Path {
        val scaledPath = android.graphics.Path()
        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY)
        path.asAndroidPath().transform(matrix, scaledPath)
        return scaledPath.asComposePath()
    }

    private fun scalePoint(
        point: Point,
        originalWidth: Int,
        originalHeight: Int,
        newWidth: Int,
        newHeight: Int,
    ): Point {
        val scaledX = point.offset.x * newWidth / originalWidth
        val scaledY = point.offset.y * newHeight / originalHeight
        return point.copy(
            offset = Offset(scaledX, scaledY),
            size = (newWidth / point.offset.x) * point.size
        )
    }

    enum class BottomAction {
        INIT, BRUSH, ERASE, FORM, COLOR, GENERATOR
    }

    enum class PaintType {
        BRUSH, ERASE, SQUARE, CIRCLE, TRIANGLE, ARROW
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BoxScope.BottomTabBar(
        speed: Float = 3f,
        paintType: PaintType,
        onSpeedChanged: (Float) -> Unit,
        isAnimationStart: Boolean,
        onBrushClick: () -> Unit,
        onEraseClick: () -> Unit,
        onInstrumentsClick: () -> Unit,
        onColorClick: (Color) -> Unit,
        onPaletteClick: () -> Unit,
        color: Color,
        onPencilClick: () -> Unit,
        bottomAction: BottomAction,
        pencilSize: Float,
        onPencilSize: (Float) -> Unit,
        onPaintTypeChange: (PaintType) -> Unit,
        onGenerateClick: (Int) -> Unit,
    ) {
        Column(
            modifier = Modifier
                .defaultMinSize(minHeight = 32.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (bottomAction != BottomAction.INIT && !isAnimationStart) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0x29555454)
                        )
                        .padding(16.dp),
                ) {
                    when (bottomAction) {
                        BottomAction.BRUSH, BottomAction.ERASE -> {
                            Slider(
                                value = pencilSize,
                                onValueChange = { onPencilSize(it) },
                                valueRange = 1f..30f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (color == Color.Transparent) Color.White else color,
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent,
                                    disabledActiveTickColor = Color.Transparent,
                                    disabledInactiveTickColor = Color.Transparent,
                                    activeTrackColor = if (color == Color.Transparent) Color.White else color,
                                ),
                                steps = 10,
                            )
                        }

                        BottomAction.GENERATOR -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                var count by remember { mutableStateOf(1) }
                                TextField(
                                    value = count.toString(),
                                    onValueChange = { value ->
                                        if (value.toIntOrNull() != null) {
                                            count = value.toInt()
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.textFieldColors(
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = Color.Transparent,
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Button(
                                    onClick = {
                                        onGenerateClick(count)
                                        count = 1
                                    },
                                ) {
                                    Text(
                                        "Сгенерировать",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        BottomAction.FORM -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = { onPaintTypeChange(PaintType.SQUARE) }
                                ) {
                                    Icon(
                                        painter(R.drawable.ic_square),
                                        "Square form",
                                        modifier = Modifier.size(32.dp),
                                        tint = if (paintType == PaintType.SQUARE) color else MaterialTheme.colorScheme.surface
                                    )
                                }
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = { onPaintTypeChange(PaintType.CIRCLE) }
                                ) {
                                    Icon(
                                        painter(R.drawable.ic_circle),
                                        "Circle form",
                                        modifier = Modifier.size(32.dp),
                                        tint = if (paintType == PaintType.CIRCLE) color else MaterialTheme.colorScheme.surface
                                    )
                                }
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = { onPaintTypeChange(PaintType.TRIANGLE) }
                                ) {
                                    Icon(
                                        painter(R.drawable.ic_triangle),
                                        "Triangle form",
                                        modifier = Modifier.size(32.dp),
                                        tint = if (paintType == PaintType.TRIANGLE) color else MaterialTheme.colorScheme.surface
                                    )
                                }
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = { onPaintTypeChange(PaintType.ARROW) }
                                ) {
                                    Icon(
                                        painter(R.drawable.ic_arrow_up),
                                        "Arrow form",
                                        modifier = Modifier.size(32.dp),
                                        tint = if (paintType == PaintType.ARROW) color else MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                        }

                        BottomAction.COLOR -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                /*IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = onPaletteClick
                                ) {
                                    Icon(
                                        painter(R.drawable.ic_group),
                                        "Choose color",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }*/
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = { onColorClick(Color.White) }
                                ) {
                                    Icon(
                                        painter(R.drawable.ic_color),
                                        "Choose color",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = { onColorClick(Color.Red) }
                                ) {
                                    Icon(
                                        painter(R.drawable.ic_color),
                                        "Choose color",
                                        tint = Color.Red,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = { onColorClick(Color.Black) }
                                ) {
                                    Icon(
                                        painter(R.drawable.ic_color),
                                        "Choose color",
                                        tint = Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = { onColorClick(Color.Blue) }
                                ) {
                                    Icon(
                                        painter(R.drawable.ic_color),
                                        "Choose color",
                                        tint = Color.Blue,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
            AnimatedVisibility(
                !isAnimationStart,
                exit = shrinkVertically() + fadeOut(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { onPencilClick() }
                    ) {
                        Icon(
                            painter(R.drawable.ic_pencil),
                            "Pencil",
                            modifier = Modifier.size(32.dp),
                            tint = if (bottomAction == BottomAction.BRUSH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onBrushClick
                    ) {
                        Icon(
                            painter(R.drawable.ic_brush),
                            "Brush",
                            modifier = Modifier.size(32.dp),
                            tint = if (bottomAction == BottomAction.GENERATOR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onEraseClick
                    ) {
                        Icon(
                            painter(R.drawable.ic_erase),
                            "Erase",
                            modifier = Modifier.size(32.dp),
                            tint = if (bottomAction == BottomAction.ERASE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onInstrumentsClick
                    ) {
                        Icon(
                            painter(R.drawable.ic_instruments),
                            "Instruments",
                            modifier = Modifier.size(32.dp),
                            tint = if (bottomAction == BottomAction.FORM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        modifier = Modifier
                            .size(32.dp)
                            .border(
                                2.dp,
                                color = if (bottomAction == BottomAction.COLOR) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            ),
                        onClick = onPaletteClick,
                    ) {
                        Icon(
                            painter(R.drawable.ic_color),
                            "Choose color",
                            modifier = Modifier.size(32.dp),
                            tint = color
                        )
                    }
                }
            }
            AnimatedVisibility(
                isAnimationStart,
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Slider(
                        value = speed,
                        onValueChange = { onSpeedChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth(),
                        valueRange = 1f..5f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent,
                            disabledActiveTickColor = Color.Transparent,
                        ),
                        steps = 3,
                    )
                }
            }
        }
    }

    @Composable
    fun BoxScope.PaintCanvas(
        setLayoutSize: (IntSize) -> Unit,
        frameCount: Int,
        isAnimationStart: Boolean,
        lastFrame: List<DrawingItems>,
        saved: List<DrawingItems>,
        color: Color,
        saveDrawingItems: (DrawingItems) -> Unit,
        onDraw: () -> Unit,
        isFrameShows: Boolean,
        listState: LazyListState,
        frames: List<List<DrawingItems>>,
        drawingLayoutSize: IntSize,
        pencilSize: Float,
        paintType: PaintType,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned {
                    setLayoutSize(it.size)
                }
                .padding(
                    horizontal = 16.dp,
                    vertical = 60.dp
                )
                .clipToBounds()
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
        ) {
            Image(
                painter(R.drawable.background_canvas),
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Canvas",
                contentScale = ContentScale.FillBounds
            )
            DrawingCanvas(
                isAnimationStart = isAnimationStart,
                saved = saved,
                last = lastFrame,
                color = color,
                saveDrawingItems = saveDrawingItems,
                onDraw = onDraw,
                size = pencilSize,
                paintType = paintType,
            )
            if (!isAnimationStart) {
                Text(
                    text = (frameCount + 1).toString(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 12.dp),
                    color = Color.DarkGray,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Frames(isFrameShows, isAnimationStart, listState, frames, drawingLayoutSize)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun PaintActionBar(
        isAnimationStart: Boolean,
        isLastActive: Boolean,
        isNextActive: Boolean,
        onLastClick: () -> Unit,
        onNextClick: () -> Unit,
        onDeleteClick: () -> Unit,
        onCreateFrameClick: () -> Unit,
        onCopyFrame: () -> Unit,
        onLayersClick: () -> Unit,
        onPauseClick: () -> Unit,
        onPlayClick: () -> Unit,
        onDeleteAll: () -> Unit,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            AnimatedVisibility(
                !isAnimationStart
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        modifier = Modifier.size(24.dp),
                        onClick = onLastClick,
                        enabled = isLastActive,
                    ) {
                        Icon(
                            painter(R.drawable.ic_last),
                            "Last",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(24.dp),
                        enabled = isNextActive,
                    ) {
                        Icon(
                            painter(R.drawable.ic_next),
                            "Next",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(
                !isAnimationStart
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { onDeleteClick() },
                                onLongClick = { onDeleteAll() }
                            )
                            .size(32.dp),
                    ) {
                        Icon(
                            painter(R.drawable.ic_bin),
                            "Bin",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { onCreateFrameClick() },
                                onLongClick = { onCopyFrame() }
                            )
                            .size(32.dp),
                    ) {
                        Icon(
                            painter(R.drawable.ic_file_plus),
                            "Create frame",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onLayersClick
                    ) {
                        Icon(
                            painter(R.drawable.ic_layers),
                            "Layers",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = if (isAnimationStart) Modifier.fillMaxWidth() else Modifier,
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (isAnimationStart) {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = onPauseClick
                        ) {
                            Icon(
                                painter(R.drawable.ic_pause),
                                "Pause",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        IconButton(
                            modifier = Modifier.size(32.dp),
                            onClick = onPlayClick
                        ) {
                            Icon(
                                painter(R.drawable.ic_play),
                                "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun BoxScope.DrawingCanvas(
        size: Float,
        isAnimationStart: Boolean,
        saved: List<DrawingItems>,
        last: List<DrawingItems>? = null,
        saveDrawingItems: (DrawingItems) -> Unit,
        onDraw: () -> Unit,
        color: Color,
        paintType: PaintType,
    ) {
        val points = remember(isAnimationStart) { mutableStateListOf<Point>() }
        var offsetStart by remember { mutableStateOf(Offset(0f, 0f)) }
        val context = LocalContext.current
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isAnimationStart, color, size, paintType) {
                    if (isAnimationStart) return@pointerInput
                    detectTapGestures(
                        onTap = { offset: Offset ->
                            when (paintType) {
                                PaintType.SQUARE -> {
                                    val squareSize = 40.dp.toPx()
                                    saveDrawingItems(
                                        DrawingItems.Square(
                                            topLeft = offset - Offset(
                                                squareSize / 2,
                                                squareSize / 2
                                            ),
                                            size = squareSize,
                                            color = color,
                                            borderSize = size
                                        )
                                    )
                                }

                                PaintType.CIRCLE -> {
                                    val circleSize = 20.dp.toPx()
                                    saveDrawingItems(
                                        DrawingItems.Circle(
                                            center = offset,
                                            radius = circleSize,
                                            color = color,
                                            size = size
                                        )
                                    )
                                }

                                PaintType.TRIANGLE -> {
                                    val triangleSize = 20.dp.toPx()
                                    saveDrawingItems(
                                        DrawingItems.Triangle(
                                            point1 = offset + Offset(0f, -triangleSize),
                                            point2 = offset + Offset(triangleSize, triangleSize),
                                            point3 = offset + Offset(-triangleSize, triangleSize),
                                            color = color,
                                            size = size
                                        )
                                    )
                                }

                                PaintType.ARROW -> {
                                    val lineSize = 32.dp.toPx()
                                    val arrowSize = 8.dp.toPx()
                                    saveDrawingItems(
                                        DrawingItems.Arrow(
                                            point1 = offset + Offset(0f, -lineSize),
                                            point2 = offset + Offset(
                                                -arrowSize - 12.dp.toPx(),
                                                arrowSize - 4.dp.toPx()
                                            ),
                                            point3 = offset + Offset(
                                                arrowSize + 12.dp.toPx(),
                                                arrowSize - 4.dp.toPx()
                                            ),
                                            point4 = offset + Offset(0f, lineSize),
                                            color = color,
                                            size = size
                                        )
                                    )
                                }

                                else -> {
                                    points.add(
                                        Point(
                                            size = size,
                                            offset = offset,
                                            color = color
                                        )
                                    )
                                    //saveDrawingItems(DrawingItems.Points(points.toList()))
                                    points.clear()
                                }
                            }
                        },
                    )
                }
                .pointerInput(isAnimationStart, color, size, paintType) {
                    if (isAnimationStart) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            onDraw()
                            offsetStart = it
                        },
                        onDrag = { change, offset ->
                            change.consume()
                            offsetStart += offset
                            points.add(
                                Point(
                                    size = size,
                                    offset = offsetStart,
                                    color = if (color == Color.Transparent) Color.White.copy(0.3f) else color
                                )
                            )
                        },
                        onDragEnd = {
                            offsetStart = Offset(0f, 0f)
                            Path().also {
                                points.forEachIndexed { index, point ->
                                    if (index == 0) {
                                        it.moveTo(point.offset.x, point.offset.y)
                                    } else {
                                        it.lineTo(point.offset.x, point.offset.y)
                                    }
                                }
                                saveDrawingItems(
                                    DrawingItems.DrawingPath(
                                        path = it,
                                        color = color,
                                        size = size,
                                    )
                                )
                            }
                            points.clear()
                        })
                }
        ) {
            if (!isAnimationStart) {
                if (last != null) {
                    //Отрисовывает последний полупрозрачный кадр
                    onDrawItems(last)
                }
            }
            //Отрисовывает фигуры, которые нарисованы на данный момент на канвасе
            onDrawItems(saved)
            if (!isAnimationStart) {
                //Отрисовывает линии под пальцем при рисовании
                onDrawPoints(points)
            }
        }
    }

    private fun DrawScope.onDrawItems(actions: List<DrawingItems>) {
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            actions.forEach {
                when (it) {
                    is DrawingItems.Arrow -> {
                        drawLine(
                            it.color,
                            it.point1,
                            it.point4,
                            strokeWidth = it.size
                        )
                        drawLine(
                            it.color,
                            it.point1,
                            it.point2,
                            strokeWidth = it.size
                        )
                        drawLine(
                            it.color,
                            it.point1,
                            it.point3,
                            strokeWidth = it.size
                        )
                    }

                    is DrawingItems.Circle -> {
                        drawCircle(
                            color = it.color,
                            center = it.center,
                            radius = it.radius,
                            style = Stroke(it.size)
                        )
                    }

                    is DrawingItems.Points -> {

                    }

                    /*is DrawingItems.Points -> {
                    onDrawPoints(it.points)
                }*/

                    is DrawingItems.Square -> {
                        drawRect(
                            color = it.color,
                            topLeft = it.topLeft,
                            size = Size(it.size, it.size),
                            style = Stroke(it.borderSize)
                        )
                    }

                    is DrawingItems.Triangle -> {
                        drawLine(
                            it.color,
                            it.point1,
                            it.point2,
                            strokeWidth = it.size
                        )
                        drawLine(
                            it.color,
                            it.point2,
                            it.point3,
                            strokeWidth = it.size
                        )
                        drawLine(
                            it.color,
                            it.point3,
                            it.point1,
                            strokeWidth = it.size
                        )
                    }

                    is DrawingItems.DrawingPath -> {
                        onDrawPath(it)
                    }
                }
            }
            restoreToCount(checkPoint)
        }
    }

    private fun DrawScope.onDrawPath(path: DrawingItems.DrawingPath) {
        if (path.color == Color.Transparent) {
            drawPath(
                path.path,
                path.color,
                style = Stroke(
                    path.size,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = BlendMode.Clear,
            )
        } else {
            drawPath(
                path.path,
                path.color,
                style = Stroke(
                    path.size,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }

    private fun DrawScope.onDrawPoints(points: List<Point>) {
        var lastPoint = points.firstOrNull()
        points.forEach { item ->
            lastPoint?.let {
                drawLine(
                    item.color,
                    it.offset,
                    item.offset,
                    strokeWidth = item.size,
                )
            }
            lastPoint = item
        }
    }

    sealed class DrawingItems {
        data class DrawingPath(val path: Path, val color: Color, val size: Float) : DrawingItems()
        data class Points(val points: List<Point>) : DrawingItems()
        data class Circle(
            val center: Offset,
            val radius: Float,
            val color: Color,
            val size: Float,
        ) : DrawingItems()

        data class Square(
            val topLeft: Offset,
            val size: Float,
            val color: Color,
            val borderSize: Float,
        ) : DrawingItems()

        data class Triangle(
            val point1: Offset,
            val point2: Offset,
            val point3: Offset,
            val color: Color,
            val size: Float,
        ) : DrawingItems()

        data class Arrow(
            val point1: Offset,
            val point2: Offset,
            val point3: Offset,
            val point4: Offset,
            val color: Color,
            val size: Float,
        ) : DrawingItems()
    }

    data class Point(val size: Float, val offset: Offset, val color: Color)
}