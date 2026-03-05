import re
import os

def fix_file(path, pattern, replacement):
    if not os.path.exists(path): return
    with open(path, 'r') as f:
        content = f.read()
    new_content = re.sub(pattern, replacement, content, flags=re.MULTILINE)
    with open(path, 'w') as f:
        f.write(new_content)

# 1. إصلاح ExpressiveComponents.kt بشكل نهائي
# سنقوم باستبدال كتل الـ drawWithCache بالكامل لضمان نظافة الكود
expressive_path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'

# تنظيف شامل للملف من أي محاولات سابقة فاشلة
with open(expressive_path, 'r') as f:
    lines = f.readlines()

new_lines = []
skip_until = None
for line in lines:
    if skip_until and skip_until not in line: continue
    if skip_until and skip_until in line:
        skip_until = None
        continue

    if 'onDrawBehind {' in line:
        # اكتشاف نوع الـ progress المستخدم في هذه الكتلة
        # سنفترض الترتيب الطبيعي للكتل في الملف
        pass
    new_lines.append(line)

# بدلاً من التلاعب بالأسطر، سنقوم باستبدال الدوال بالكامل لضمان الجودة
with open(expressive_path, 'r') as f:
    full_content = f.read()

# إصلاح CoercedExpressiveCard
full_content = re.sub(r'fun CoercedExpressiveCard\(.*?\)\s*\{.*?Column\(',
    r'''fun CoercedExpressiveCard(
    userRadius: Float,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val morphProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = GhostMotion.TactileSpring,
        label = "card_morph"
    )

    val shapeStart = remember(userRadius) { RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding(userRadius.coerceIn(0f, 100f) / 100f)) }
    val shapeEnd = remember(userRadius) { RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding((userRadius * 1.2f).coerceIn(0f, 100f) / 100f)) }
    val morph = remember(userRadius) { Morph(shapeStart, shapeEnd) }

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        interactionSource = interactionSource,
        color = Color.Transparent,
        modifier = modifier
            .physicalTilt()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(16.dp)
                } else Modifier
            )
            .drawWithCache {
                val matrix = Matrix()
                onDrawBehind {
                    val aPath = android.graphics.Path()
                    morph.toPath(morphProgress, aPath)
                    val path = aPath.asComposePath()

                    matrix.reset()
                    matrix.scale(size.width / 2f, size.height / 2f)
                    matrix.translate(1f, 1f)
                    path.transform(matrix)

                    drawPath(path, color = containerColor)
                    drawPath(path, color = Color.White.copy(alpha = 0.2f), style = Stroke(0.5.dp.toPx()))
                }
            }
    ) {
        Column(''', full_content, flags=re.DOTALL)

# إصلاح MorphingDiscoveryButton و MD3ELoadingIndicator بنفس المنطق
# سيتم استخدام aPath لتفادي التعارض

def fix_morph_calls(content):
    # استبدال أي كتلة رسم تحتوي على morph.toPath لتستخدم aPath
    blocks = [
        ('morphProgress', 'morphProgress'),
        ('shapeProgress', 'shapeProgress'),
        ('localEasedProgress', 'animatedProgress'),
        ('localEasedProgress', 'localEasedProgress')
    ]
    for old_p, new_p in blocks:
        # هذا النمط سيبحث عن كتل الرسم المشوهة ويصلحها
        content = re.sub(r'onDrawBehind\s*\{.*?morph\.toPath\(.*?,.*?\).*?matrix\.reset\(\)',
            f'''onDrawBehind {{
                    val aPath = android.graphics.Path()
                    morph.toPath({new_p}, aPath)
                    val path = aPath.asComposePath()
                    matrix.reset()''', content, flags=re.DOTALL)
    return content

# تطبيق الإصلاح على الملف بالكامل
with open(expressive_path, 'w') as f:
    f.write(full_content)

# 2. إصلاح ChatScreen.kt
chat_path = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
with open(chat_path, 'r') as f:
    c_lines = f.readlines()
clean_chat = [l for l in c_lines if '@OptIn' not in l and '@file:OptIn' not in l]
clean_chat.insert(0, '@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n')
with open(chat_path, 'w') as f:
    f.writelines(clean_chat)
