---
title: 信标样式
layout: page
parent: "使用 Soundscape"
has_toc: true
lang: zh-CN
permalink: /users/help-beacon-styles.html
machine-translated: true
---

# 信标样式

Soundscape 允许您在 **设置 → 音频 → 音频信标样式** 中选择几种不同的信标声音。它们的作用都相同——利用 3D 空间音频将您引向目的地——但它们听起来彼此差异很大。

每个信标都由一小组循环播放的样本构成。Soundscape 会根据您所朝向的方向与信标方向之间的角度自动选择合适的样本，因此当您转动头部或移动时，声音也会随之变化。当您正对着信标时，会播放“对准轴线”的样本；当信标在您身后时，则会播放“背后”的样本。

## 如何使用此页面

对于下面的每个信标，您都会看到：

- **一段循环播放的音频样本**，它会依次播放各个角度变体，从对准轴线（正前方）的声音开始，到背后的声音结束，这样您就可以听到信标在您转身时是如何变化的。
- **一张频谱图**，显示信标的能量在可听范围内的分布位置。如果您在某些特定频率上有听力损失，那么大部分能量分布在您听得清楚的区域的信标将最容易跟随。

这些图表经过归一化处理，使最响的峰值位于 0&nbsp;dB；这些曲线适用于比较每个信标频率内容的*形状*，而非其绝对响度。

**注意：** 这里的音频样本是单声道的，未使用 3D 空间化处理。在应用本身中，声音是经过双耳渲染的，因此当您佩戴立体声耳机时，它会听起来像是来自某个特定方向。

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
