import re
import os

def fix_expressive():
    path = 'app/src/main/java/com/kai/ghostmesh/core/ui/components/ExpressiveComponents.kt'
    with open(path, 'r') as f:
        content = f.read()

    # 1. تنظيف شامل للملف وإعادة بناء كتل الرسم
    # سنستخدم استراتيجية: إنشاء Path محلي وتحويله
    def clean_draw_block(progress):
        return f"""onDrawBehind {{
                    val androidPath = android.graphics.Path()
                    morph.toPath({progress}, androidPath)
                    val path = androidPath.asComposePath()"""

    # استبدال كافة الأنماط المشوهة السابقة
    content = re.sub(r'onDrawBehind \{\s+.*?morph\.toPath\(([^,]+),.*?\)',
                     lambda m: clean_draw_block(m.group(1)), content, flags=re.DOTALL)

    # إزالة أي تعارضات في التسمية
    content = content.replace('val path = Path()', '')

    with open(path, 'w') as f:
        f.write(content)

def fix_main_activity():
    path = 'app/src/main/java/com/kai/ghostmesh/MainActivity.kt'
    if not os.path.exists(path): return
    with open(path, 'r') as f:
        content = f.read()

    # إضافة المعاملات المفقودة لـ ChatScreen
    if 'stagedMedia = ' not in content:
        content = content.replace('onClearReply = { chatViewModel.clearReply() },',
            '''onClearReply = { chatViewModel.clearReply() },
                                    stagedMedia = stagedMedia,
                                    onStageMedia = { uri, type -> chatViewModel.stageMedia(uri, type) },
                                    onUnstageMedia = { chatViewModel.unstageMedia(it) },
                                    recordingDuration = recordingDuration,
                                    cornerRadius = currentCornerRadius,
                                    transportType = currentTransport,''')

    # التأكد من تعريف المتغيرات المطلوبة في MainActivity
    if 'val stagedMedia by chatViewModel.stagedMedia.collectAsState()' not in content:
        content = content.replace('val chatMessages by chatViewModel.messages.collectAsState()',
            '''val chatMessages by chatViewModel.messages.collectAsState()
                                val stagedMedia by chatViewModel.stagedMedia.collectAsState()
                                val recordingDuration by chatViewModel.recordingDuration.collectAsState()
                                val currentCornerRadius by settingsViewModel.cornerRadius.collectAsState()
                                val currentTransport = "" // Placeholder for now''')

    with open(path, 'w') as f:
        f.write(content)

def fix_chat_screen():
    path = 'app/src/main/java/com/kai/ghostmesh/features/chat/ChatScreen.kt'
    with open(path, 'r') as f:
        lines = f.readlines()

    new_lines = []
    has_file_optin = False
    for line in lines:
        if '@file:OptIn' in line:
            if not has_file_optin:
                new_lines.append('@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n')
                has_file_optin = True
            continue
        if '@OptIn' in line: continue
        new_lines.append(line)

    if not has_file_optin:
        new_lines.insert(0, '@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)\n')

    with open(path, 'w') as f:
        f.writelines(new_lines)

fix_expressive()
fix_main_activity()
fix_chat_screen()
