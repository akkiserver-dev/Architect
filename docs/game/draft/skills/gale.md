# 天狗の羽団扇 (gale)

状態: draft → この仕様を起点にデータ駆動試験として実装。

## 操作と効果

着想: 天狗の羽団扇で風を起こし、身を運ぶイメージ。攻めにも離脱にも使える短い踏み込み。

右クリックで視線方向へ速度1.25、上向き0.18のダッシュ。速度上限2。CT100tick。

## 狙いと制約

位置取り用。テレポートではなく物理移動、壁を貫通しない。
値は試験用の初期値。実戦でバランス確認が必要。ヒット判定・移動・演出はJavaが実行し、Luaは短い条件分岐と効果の組合せだけを扱う。

## 運営の編集先

- 定義: `plugins/Otogi/architects/gale.json` (同梱元 `src/main/resources/architects/gale.json`)
- 実行: java / dash
- JSONのname/description/material/cooldown/attackInterval/parametersで表示と数値を変更する。
- Luaの場合は `plugins/Otogi/architects/scripts/` 内の同名スクリプトを編集する。
- `/otogi reload` は全定義の検証が成功した場合のみ登録を置換する。試合中の再読み込みは拒否され、終了後に再読み込みすると次試合から新定義になる。

## 検証観点

他プレイヤーと状態が混ざらないこと、死亡/退出で状態が消えること、不正JSON/Luaで現行定義が失われないこと、数値変更が次試合に反映されること。
