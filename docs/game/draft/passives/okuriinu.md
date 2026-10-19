# 送り犬・護り牙 (okuriinu)

状態: 実装済み（draftを先に作成、対人調整前）。送り犬の伝承に着想を得た創作の相。

確定した `TOOL` / `AREA_TOOL` の被弾が、直近100tickに3回そろうと、自身に耐性Iと速度Iを各40tick付ける。発動後は記録を消し、内部CT200tick。CT中の被弾は記録しない。窓の最初の被弾から100tickに達した記録は期限切れとして外し、残った被弾は保持する。`SECONDARY`、術、環境ダメージは数えない。自動移動はしない。

編集先: `plugins/Otogi/architects/okuriinu.json` と `scripts/okuriinu.lua`。`required_hits` は1〜3の範囲で変更できる。窓・内部CT・各効果時間と段階もJSONから変更できる。試合終了後の `/otogi reload` で反映。

検証: 被弾種別の除外、100tickの期限境界、期限切れ後に残る記録、3回目で発動、CT中の無蓄積、CT期限ちょうどでの再蓄積、死亡後の初期化、所有者間の分離。
