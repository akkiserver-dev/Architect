# 件・凶兆 (kudan)

状態: 実装済み（draftを先に作成、対人調整前）。凶事を告げる件の伝承に着想を得た、危機を知らせる相。

## 操作と効果

現在HPが最大HPの30%以下になると、20tick（1秒）ごとの判定で自身に速度Iを60tick（3秒）、耐性Iを20tick（1秒）付与し、短い通知を出す。発動後の内部CTは300tick（15秒）。低体力のままCTが終われば再発動できる。自動移動はしない。

## 狙いと制約

致死ダメージを防ぐ不死や、被弾で透明になる隠密とは異なり、残りHPが少ない間に手動で退く隙を作る。死亡・解除・再装備時には内部CTを初期化する。付与済みの短時間ポーション効果は自然に切れる。

## 運営の編集先

- 定義: `plugins/Otogi/architects/kudan.json`（同梱元 `src/main/resources/architects/kudan.json`）
- 実行: `plugins/Otogi/architects/scripts/kudan.lua`
- `parameters.threshold` は最大HP比、`speed_ticks`・`resistance_ticks` は各効果時間、`speed_amplifier`・`resistance_amplifier` は効果段階（0がI）、`interval` は内部CT。`tickInterval` は判定間隔。説明文も数値と合わせて変更する。

## 検証観点

閾値を超える間は発動しないこと、30%ちょうどで発動すること、CT中は再通知・再付与しないこと。移動の強制がなく、効果は自分だけであること。
