---
title: أنماط المنارات
layout: page
parent: "استخدام ساوندسكيب"
has_toc: true
lang: arz
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# أنماط المنارات

Soundscape بيخليك تختار من بين كذا صوت منارة مختلف في
**إعدادات → الصوت → أنماط منارات الصوت**. كلهم بيعملوا نفس الشغلانة —
بيشدّوك ناحية الوجهة باستخدام الصوت المكاني ثلاثي الأبعاد — بس صوتهم
مختلف جدًا عن بعض.

كل منارة متبنية من مجموعة صغيرة من العينات المتكررة. Soundscape
بيختار تلقائيًا العينة الصح للزاوية اللي بين الاتجاه اللي انت مواجهه
واتجاه المنارة، فالصوت بيتغير وانت بتلف راسك أو
بتتحرك. عينات "على المحور" بتشتغل لما تكون متجه ناحية المنارة؛ وعينات
"الخلف" بتشتغل لما تكون ورا منك.

## إزاي تستخدم الصفحة دي

لكل منارة تحت هتلاقي:

- **عينة صوتية متكررة** بتشغّل كل واحدة من تنويعات الزاوية بالدور،
  بادئة من صوت على المحور (قدامك) ومنتهية بصوت
  ورا منك، عشان تقدر تسمع إزاي المنارة بتتغير وانت بتلف.
- **رسم بياني لطيف التردد** بيوضح فين بتقع طاقة المنارة في
  المدى المسموع. لو عندك ضعف سمع في ترددات معينة،
  المنارات اللي معظم طاقتها في المناطق اللي بتسمعها كويس هتبقى
  أسهل واحدة تتبعها.

الرسومات معايرة عشان أعلى قمة تقع عند 0&nbsp;dB؛ المنحنيات
مفيدة لمقارنة *شكل* محتوى التردد لكل منارة، مش
علو صوتها المطلق.

**ملاحظة:** العينات الصوتية هنا أحادية ومش بتستخدم التوضيع المكاني ثلاثي الأبعاد.
في التطبيق نفسه بيتم تصيير الصوت بشكل ثنائي الأذن، فهيبان إنه
جاي من اتجاه معين لما تلبس سماعات رأس استريو.

---

{% include beacon-styles.md %}

<script>
  /* Only allow one beacon audio sample to play at a time. Kramdown strips
     newlines inside <script> blocks, so a // comment would swallow the rest
     of the script — use a block comment. */
  document.addEventListener("DOMContentLoaded", function () {
    var players = document.querySelectorAll("audio");
    players.forEach(function (player) {
      player.addEventListener("play", function () {
        players.forEach(function (other) {
          if (other !== player) {
            other.pause();
          }
        });
      });
    });
  });
</script>
