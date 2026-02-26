# ChateX - الدليل التقني الشامل (Technical Manifesto)

هذا الملف هو "الخزنة المركزية" والمعجم التقني لمشروع ChateX. تم تصميمه ليكون المرجع الأول لضمان استمرارية الجودة التقنية والجمالية (Expressive UI).

---
 
## 1. الهندسة المعمارية للنظام (System Architecture)

### الـ Mesh Networking (Nearby Connections API)
نعتمد على استراتيجية Strategy.P2P_CLUSTER لأنها الأنسب لتطبيقات المحادثة الجماعية القريبة.
- **Discovery & Advertising:** يعمل الموبايل كـ "Node" (عقدة) تبث وتستقبل في آن واحد.
- **Payloads:** يتم إرسال الرسائل كـ Payload.BYTES. المخطط القادم يدعم Payload.FILE للصور.
- **Multi-hop Relay:** بناء جدول توجيه (Routing Table) يدوي لتمرير الرسائل عبر الأجهزة الوسيطة.

### الصلاحيات (Permissions) - API 34+
تعتبر هذه الأهم لضمان عمل التطبيق على أندرويد 12 و 13 و 14 و 15:
- BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT: للبحث والربط.
- NEARBY_WIFI_DEVICES: (Android 13+) ضرورية لعمل WiFi Direct بدون Location.
- ACCESS_FINE_LOCATION: ضرورية للإصدارات الأقدم من أندرويد 12.

---

## 2. موسوعة Material 3 Expressive (M3E)

الـ Expressive UI ليس مجرد "ثيم"، بل هو نظام فيزيائي متكامل.

### أ- نظام الحركة (Motion Physics)
ننتقل من منحنيات الـ Bezier إلى الـ Spring Physics. القيم المثالية لـ M3E هي:
- **Expressive Motion:**
  - Stiffness = 800f: استجابة سريعة جداً.
  - DampingRatio = 0.7f: يسمح بـ Overshoot (ارتداد) بسيط يعطي طابعاً حيوياً.
- **Standard Motion:**
  - Stiffness = 1500f.
  - DampingRatio = 1.0f: حركة حادة ومباشرة بدون ارتداد.

### ب- تحول الأشكال (Shape Morphing)
نستخدم مكتبة androidx.graphics:graphics-shapes:1.0.1 للقيام بالتحولات العضوية.
- **RoundedPolygon:** مضلع رياضي يتم تعريفه بـ numVertices.
- **MorphingIcon:** تم تنفيذه باستخدام Canvas و Morph.toPath. يقوم بتحويل شكل "نجمة" إلى "دائرة" بشكل مستمر.
- **WavyProgressIndicator:** مؤشر مخصص يرسم موجة جيبية (Sine Wave) متحركة باستخدام Canvas.

**نمط تنفيذ الـ Morphing (المحدث):**
```kotlin
val morph = remember { Morph(startPolygon, endPolygon) }
// الرسم باستخدام Canvas
Canvas(modifier = modifier) {
    morph.toPath(progress, path.asAndroidPath())
    drawPath(path, color)
}
```

### ج- المكونات التعبيرية (Expressive Components)
1. **SplitButton:**
   - زر مزدوج مع Morphing للزوايا الداخلية (shapeByInteraction).
2. **FloatingToolbar:**
   - "بار" عائم يتحول من FAB إلى Toolbar كامل عند الـ Scroll.
3. **WavyProgressIndicator:**
   - تم التنفيذ: مؤشر تقدم "موجي" يتفاعل مع سرعة التحميل (أو بشكل مستمر في الـ MVP).
4. **LoadingIndicator (Expressive):**
   - لا يدور فقط، بل يتحول (Morph) بين أشكال هندسية (Circle -> Star -> Square).

---

## 4. خارطة الطريق (Roadmap)

### المرحلة 1: الـ Visual Identity (منجزة)
- [x] ترقية المشروع لـ 1.5.0-alpha14 و Kotlin 2.3.10.
- [x] تغيير اسم التطبيق لـ ChateX.
- [x] تنفيذ أيقونة "الشبح المتحول" (Ghost Morphing Icon).
- [x] بناء الـ ExpressiveTheme بألوان GhostWhite و EctoplasmGreen.
- [x] إضافة WavyProgressIndicator.

### المرحلة 2: الـ Mesh Core
- [x] تحسين الـ MeshManager ليدعم الـ Auto-Reconnection بشكل أذكى.
- [x] إضافة الـ Node Discovery UI (رادار يظهر الأجهزة كفقاعات متحولة).
- [x] تنفيذ الـ Multi-hop Relay و الـ Packet Logic.

### المرحلة 3: الـ Experience
- [x] محادثات مشفرة (E2EE) باستخدام AES-256.
- [x] إرسال صور وفويس نوتس مشفرة.
- [x] ميزة التدمير الذاتي (Burn After Reading).

---
*تم تحرير هذا الدستور بواسطة Kai-Agent (الشريك التقني لـ Jo).*
*الإصدار: 1.3 | التاريخ: 26 فبراير 2026*
