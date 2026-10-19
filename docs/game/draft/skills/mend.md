# 鶴の恩返し (mend)

状態: draft → この仕様を起点にデータ駆動試験として実装。

## 操作と効果

着想: 『鶴の恩返し』の報恩を、傷を繕う術へ置き換えた創作。安全な退き際を作る回復枠。

右クリックで自身を5HP回復。全快なら不発でCT消費なし。CT240tick。

## 狙いと制約

攻撃中断時の立て直し。回復は最大HPまで。
値は試験用の初期値。実戦でバランス確認が必要。ヒット判定・移動・演出はJavaが実行し、Luaは短い条件分岐と効果の組合せだけを扱う。

## 運営の編集先

- 定義: `plugins/Otogi/architects/mend.json` (同梱元 `src/main/resources/architects/mend.json`)
- 実行: java / mend
- JSONのname/description/material/cooldown/attackInterval/parametersで表示と数値を変更する。
- Luaの場合は `plugins/Otogi/architects/scripts/` 内の同名スクリプトを編集する。
- `/otogi reload` は全定義の検証が成功した場合のみ登録を置換する。試合中の再読み込みは拒否され、終了後に再読み込みすると次試合から新定義になる。

## 検証観点

他プレイヤーと状態が混ざらないこと、死亡/退出で状態が消えること、不正JSON/Luaで現行定義が失われないこと、数値変更が次試合に反映されること。
