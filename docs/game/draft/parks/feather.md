# 天女の羽衣 (feather)

状態: draft → この仕様を起点にデータ駆動試験として実装。

## 操作と効果

着想: 羽衣伝説の軽やかさ。足運びと引き換えに打たれ弱くなる式。

装備中は移動速度+12%。受けるダメージ1.10倍。

## 狙いと制約

機動力と耐久力の交換。解除/死亡時に速度補正を削除。
値は試験用の初期値。実戦でバランス確認が必要。ヒット判定・移動・演出はJavaが実行し、Luaは短い条件分岐と効果の組合せだけを扱う。

## 運営の編集先

- 定義: `plugins/Otogi/architects/feather.json` (同梱元 `src/main/resources/architects/feather.json`)
- 実行: lua / feather.lua
- JSONのname/description/material/cooldown/attackInterval/parametersで表示と数値を変更する。
- Luaの場合は `plugins/Otogi/architects/scripts/` 内の同名スクリプトを編集する。
- `/otogi reload` は全定義の検証が成功した場合のみ登録を置換する。試合中の再読み込みは拒否され、終了後に再読み込みすると次試合から新定義になる。

## 検証観点

他プレイヤーと状態が混ざらないこと、死亡/退出で状態が消えること、不正JSON/Luaで現行定義が失われないこと、数値変更が次試合に反映されること。
