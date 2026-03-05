import re
import os

def fix_expressive():
    path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
    with open(path, 'r') as f:
        content = f.read()

    # تحويل كافة كتل الرسم لاستخدام Compose Path مباشرة حيث أن النسخة الحالية من Material 3 توفر extension لذلك
    # سنقوم بتبسيط الكود وإزالة أي تعقيدات سابقة

    # 1. إزالة الاستيرادات الزائدة
    content = content.replace('import androidx.compose.ui.graphics.asAndroidPath\n', '')

    # 2. إصلاح كتل الرسم في الـ 4 أماكن المعروفة
    # سنستخدم نمطاً عاماً لاستبدال الكتل التي قمت بتخريبها في المحاولات السابقة
    def clean_block(match):
        progress = match.group(1)
        return f"""onDrawBehind {{
                    val path = Path()
                    morph.toPath({progress}, path)"""

    # هذا الريجيكس سيستهدف الكتل التي تبدأ بـ onDrawBehind وتنتهي قبل matrix.reset()
    content = re.sub(r'onDrawBehind \{\s+(?:val path = Path\(\)\n\s+)?val aPath = android\.graphics\.Path\(\); morph\.toPath\(([^,]+), aPath\); val path = aPath\.asComposePath\(\)',
                     clean_block, content)

    # إصلاح الحالات الأخرى التي قد تكون نتجت عن المحاولات السابقة
    content = re.sub(r'onDrawBehind \{\s+val aPath = android\.graphics\.Path\(\)\s+morph\.toPath\(([^,]+), aPath\)\s+val path = aPath\.asComposePath\(\)',
                     clean_block, content)

    with open(path, 'w') as f:
        f.write(content)

def fix_chat():
    path = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
    with open(path, 'r') as f:
        lines = f.readlines()

    new_lines = []
    # إزالة كافة الـ OptIn المكررة
    for line in lines:
        if '@OptIn' in line or '@file:OptIn' in line:
            continue
        new_lines.append(line)

    # إضافة OptIn واحد فقط على مستوى الملف
    new_lines.insert(0, '@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n')

    with open(path, 'w') as f:
        f.writelines(new_lines)

def fix_image_utils():
    path = 'app/src/main/java/com/kai/ghostmesh/core/util/ImageUtils.kt'
    with open(path, 'r') as f:
        content = f.read()
    if 'base64ToBitmap' not in content:
        last_brace = content.rfind('}')
        content = content[:last_brace] + """
    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
"""
        with open(path, 'w') as f:
            f.write(content)

fix_expressive()
fix_chat()
fix_image_utils()
