# 鬼の血脈 (siphon)

状態: draft → この仕様を起点にデータ駆動試験として実装。

## 操作と効果

着想: 鬼のしぶとさ。攻撃を通して活力を取り戻す相で、戦い続けるビルドを支える。

命中確定時、二次ダメージ以外から1HP回復。内部CT40tick。

## 狙いと制約

小さな継戦力。反射や追加ダメージで無限連鎖しない。
値は試験用の初期値。実戦でバランス確認が必要。ヒット判定・移動・演出はJavaが実行し、Luaは短い条件分岐と効果の組合せだけを扱う。

## 運営の編集先

- 定義: `plugins/Otogi/architects/siphon.json` (同梱元 `src/main/resources/architects/siphon.json`)
- 実行: lua / siphon.lua
- JSONのname/description/material/cooldown/attackInterval/parametersで表示と数値を変更する。
- Luaの場合は `plugins/Otogi/architects/scripts/` 内の同名スクリプトを編集する。
- `/otogi reload` は全定義の検証が成功した場合のみ登録を置換する。試合中の再読み込みは拒否され、終了後に再読み込みすると次試合から新定義になる。

## 検証観点

他プレイヤーと状態が混ざらないこと、死亡/退出で状態が消えること、不正JSON/Luaで現行定義が失われないこと、数値変更が次試合に反映されること。
