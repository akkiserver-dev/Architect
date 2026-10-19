# 運営用アーキテクト編集ガイド

このフォルダは初回起動時に `plugins/Otogi/architects/` へコピーされます。以後はサーバー側のファイルを編集します。JAR を入れ替えなくても JSON の数値・表示・分類を変更し、軽い Lua フックを組み合わせた新能力を追加できます。

同梱能力のIDとJSON名から `script_` を外しました（例: `nova.json` / `otogi:nova`）。既存の `script_nova.json` などは上書きせず、そのまま新IDで読み込みます。旧IDの保存済みビルド・コマンドも引き継ぎ、ビルドは次回保存時に新IDになります。旧ファイルがある場合、新名の初期ファイルは追加しません。手動で名前を整理する場合は旧ファイルを複製せずリネームしてください。同じ能力の新旧ファイルを両方置くと重複エラーになります。独自の `script_` IDや他の名前空間は変更しません。

複雑なヒット判定、複数対象の走査、移動、描画は Java が担当します。Lua は短い条件分岐と効果の組合せに向いています。大きなループや毎 tick の物理演算は Java の実装を追加してください。

## まず試す

1. 試合を終了する。`/otogi reload` は試合中には実行できません。
2. `nova.json` の `parameters.damage` を `4` から `5` に変更し、説明文も更新する。
3. `/otogi reload` を実行する。成功後は次の試合から新しい値になります。
4. 新能力を追加する場合は JSON をコピーし、**id を別の値**に変更する。Lua も別ファイルにすると個別に編集できます。

全 JSON と参照 Lua の検証後に一括登録します。構文エラー・重複 ID・不明フィールド・不正な型・トップレベルの暴走などがあれば能力登録は一切置換されず、エラーのファイル名が返ります。JSON の削除はその能力の登録解除になります。ビルドに保存されている削除済み ID は選択時に無視されます。

Lua のフック内で実行時エラーや予算超過が起きた場合、そのプレイヤーの該当能力だけを試合終了まで無効化し、属性補正と一時状態を即座に削除します。他プレイヤーの同じ能力は継続します。ログと所有者への通知は、そのプレイヤー・能力について試合中に一度だけ出ます。死亡・再装備では復帰しません。試合終了後に修正して再読み込みしてください。エラー前に適用されたダメージ等の効果は巻き戻せません。実戦投入前に試験試合で各フックを確認してください。

## 同梱の19能力

名前は日本の神話・昔話・妖怪のイメージに着想を得た創作です。元の伝承の能力を再現したものではありません。

| ID | 表示名 | 分類 | 実装 | 操作と特徴 |
|---|---|---|---|---|
| nova | 狐火・輪灯 | 術 | Java pulse | 右クリック、周囲5mに4ダメージ・押し出し、CT9秒 |
| gale | 天狗の羽団扇 | 術 | Java dash | 右クリック、短いダッシュ、CT5秒 |
| mend | 鶴の恩返し | 術 | Java mend | 右クリック、5HP回復、CT12秒 |
| needle | 鬼の縫い針 | 具 | Java lance | 左クリック、8mの細い直線・先頭の相手に3ダメージ |
| siphon | 鬼の血脈 | 相 | Lua | 直接ダメージ命中で1HP回復、内部CT2秒 |
| feather | 天女の羽衣 | 式 | Lua | 速度+12%・被ダメージ+10% |
| riposte | 八咫鏡・返照 | 相 | Lua | 被弾後4秒以内の次の直接攻撃を1.25倍 |
| narukami | 雷神・鳴神 | 術 | Java thunder | 固定した射線を1秒予告して放つ。12m・7HP、CT12秒 |
| yukionna | 雪女・白息 | 術 | Java frost_cone | 6mの扇形に3HPと3秒の減速I、CT9秒 |
| shuten | 酒呑童子・鬼宴 | 術 | Java oni_frenzy | 4秒間、直接具ダメージ1.3倍・被ダメージ1.25倍、CT13秒 |
| orochi | 八岐大蛇・蛇骨 | 具 | Lua | 突きで牙を3つまで蓄え、右クリックで周囲へ放出 |
| inaba | 因幡・白兎 | 具 | Lua | 扇形攻撃の命中後3秒以内に右クリックで跳躍 |
| tamatebako | 浦島・玉手箱 | 相 | Lua | 交戦から6秒離れると2HP回復、内部CT8秒 |
| kudan | 件・凶兆 | 相 | Lua | 体力30%以下で速度I・耐性I、内部CT15秒 |
| issun | 一寸法師・寸鉄 | 式 | Lua | 最大HP減少と、間を空けた一撃の強化 |
| shura | 修羅・背水 | 式 | Lua | 瀕死時に二次以外の与ダメージ増加、常時被ダメージ増加 |
| hasshaku | 八尺様・呼び声 | 術 | Java dread_sphere | 視線の先に球形の領域を予告。1.2秒後に4HPと短い術封印 |
| kuchisake | 口裂け・二口 | 具 | Lua | 扇形攻撃の命中後2秒以内に、右クリックで細い射線の追撃 |
| okuriinu | 送り犬・護り牙 | 相 | Lua | 具で連続被弾すると短い速度・耐性強化 |

HP は Minecraft の体力値（2HP = ハート1個）。距離の m はブロック数です。

## JSON 仕様

必須: `id`, `name`, `description`（文字列配列）, `kind`, `material`。

- `id`: `otogi:` を付けない英小文字・数字・`_/-`、1〜64文字。既存 Java 能力と同じ ID は使えません。
- `kind`: `skill`（術）, `passive`（相）, `park`（式）, `tool`（具）。式は同時に1つ。
- `material`: Bukkit のアイテム名（例 `FEATHER`）。表示用アイコン。
- `cooldown`: tick 単位、1〜72000、既定100。術と Lua 具の右クリックに適用。
- `attackInterval`: tick 単位、2〜1200、既定16。具の左クリック間隔。
- `tickInterval`: 20〜1200、既定20。`on_tick` の最短間隔。アリーナは20tick刻みなので20の倍数を推奨。他の値は次の20tick更新まで繰り上がります。
- `backend`: `java` または `lua`（既定）。
- `handler`: Java 用。`pulse`, `lance`, `dash`, `mend` は術/具。`thunder`, `frost_cone`, `oni_frenzy`, `dread_sphere` は術専用です。
- `script`: Lua 用。このフォルダ内の相対 `.lua` パス。例 `scripts/siphon.lua`。
- `parameters`: 最大64個の数値。絶対値72000以下。Lua では `ctx.params.damage` のように参照。効果 API には下記の追加上限があります。
- `incompatible`: 式で併用を禁止する ID 配列。例 `["otogi:tank"]`。

Java のパラメータ: `pulse`: range/damage/knockback、`lance`: range/width/damage/knockback、`dash`: speed/lift、`mend`: heal。rangeは直線32・球12、damage/heal40、width2、knockback/speed2、lift -0.5〜1の範囲に収まります。未指定の値は各テンプレートの既定値です。

追加Javaテンプレートは、未知のパラメーターや範囲外の値を読み込み時に拒否します。

| handler | パラメーターと許容範囲 |
|---|---|
| thunder | range 0.1〜32、width 0〜2、delayTicks 整数4〜100、damage 0.1〜40、knockback 0〜2 |
| frost_cone | range 0.1〜16、halfAngle 1〜75、damage 0.1〜40、slowTicks 整数1〜200、slowAmplifier 整数0〜2 |
| oni_frenzy | durationTicks 整数1〜200、toolMultiplier 1〜2、incomingMultiplier 1〜2 |
| dread_sphere | range 0.5〜24、radius 0.5〜6、delayTicks 整数8〜100、damage 0.1〜20、silenceTicks 整数0〜100 |

鳴神は発動時の射線を固定し、予告後に同じ射線を攻撃します。死亡・退出・装備解除・試合終了で予約が消えます。雪女の減速はダメージが通って相手が生存した場合だけ適用します。鬼宴の期限・倍率は所有者ごとに管理し、重ね掛けしません。

呼び声は発動時の視線の先へ球形領域を固定し、壁があれば手前に置きます。中心から遮蔽物のない対象だけに命中し、既存の長い術封印は短縮しません。死亡・退出・装備解除・ワールド移動・試合終了で予約を取り消します。

## Lua の書き方

スクリプトはフック名から関数へのテーブルを `return` します。`ctx` の関数は **ドット** で呼びます（`ctx.heal(1)`）。コロンでは呼びません。

```lua
return {
  on_use = function(ctx)
    if ctx.health >= ctx.max_health then return false end
    local healed = ctx.heal(ctx.params.heal)
    if healed then ctx.effect("heal") end
    return healed
  end
}
```

術の `on_use` と具の `on_attack` は必須です。発動成功時は必ず `true` を返します。それ以外は CT を消費しません。ただし戻り値は適用済み効果を取り消しません。

利用できるフック:

| フック | タイミング |
|---|---|
| on_equip / on_unequip | 装備/解除、死亡・再装備時も呼ばれる |
| on_use | 術の右クリック、または Lua 具の右クリック |
| on_attack | 具の左クリック |
| on_tick | tickInterval ごとの定期更新 |
| on_damage_dealt / on_damage_taken | ダメージ調整段階。scale_damage用 |
| on_hit / on_hurt | 防御判定を通過した命中/被弾。回復や状態変更用 |
| on_kill | 撃破時 |
| on_skill | 所有者の術の発動成功後 |

`on_hit/on_hurt` は防御を通過した時点のフックで、別プラグインが後からイベントをキャンセルする場合まで保証しません。

各呼び出しで Lua 環境は新しくなります。グローバル変数や `local` に次回までの状態を保存しないでください。`ctx.state` / `ctx.set_state` を使えば能力ごと・プレイヤーごとに分離され、解除・死亡・退出時に破棄されます。

```lua
return {
  on_hit = function(ctx)
    if ctx.damage_kind == "SECONDARY" then return end
    if ctx.tick < ctx.state("ready", 0) then return end
    ctx.set_state("ready", ctx.tick + 60)
    ctx.buff("speed", 40, 0)
  end
}
```

## ctx API

読み取り値: `params`, `tick`, `health`, `max_health`, `damage`, `damage_kind`。damage_kind は `TOOL`, `AREA_TOOL`, `SKILL`, `AREA_SKILL`, `SECONDARY`, `ENVIRONMENT`。

| 呼び出し | 内容 |
|---|---|
| state(key, default) | 状態を読む。未指定 default は0 |
| set_state(key, value) | 数値/真偽/256文字以内の文字列を保存。nilで削除。最大64キー |
| heal(amount) | 0〜40HP回復。実際に回復したらtrue |
| dash(speed, lift) | 水平速度0〜2、上向き速度-0.5〜1の物理移動 |
| cast(shape, range, width, damage, kind, knockback) | Java の判定で最大16人を攻撃、命中数を返す |
| effect(preset) | arcane/heal/wind/strike の定型演出 |
| buff(name, duration, amplifier) | speed/slowness/resistance/jump/glowing。1〜1200tick、増幅0〜4 |
| attribute(name, amount) | on_equip専用。speed/health/gravity に-0.5〜1の割合補正、解除時自動削除 |
| scale_damage(multiplier) | ダメージ調整フック専用。0〜3倍、最終値上限100 |
| reduce_cooldowns(ticks) | 所有する術のCTを0〜200tick短縮 |
| notice(text) | 160文字以下の短い通知 |

`cast` は `on_use/on_attack/on_tick` だけから使用できます。ダメージ反応からの再帰攻撃は許可しません。

- shape: `first`（直線の先頭のみ）, `line`（直線の全員）, `cone`（扇形）, `sphere`（自身中心）。
- range: 0.1〜32、球は最大12。width: 直線半径0〜2、扇形半角0〜90、球では0。
- damage: 0〜40。kind: TOOL/AREA_TOOL/SKILL/AREA_SKILL/SECONDARY。knockback: 0〜2。
- 対象は同一試合の相手だけ。遮蔽物・無敵・試合終了を Java で確認し、通常のダメージイベントを通します。

## 実行制限

- 1ファイル32KiB、定義128件まで。フォルダ外参照は禁止。
- 1フック（トップレベル評価を含む）10000 Lua命令・呼出深さ48。ゲームAPI32回、castは8回分を消費。
- Java オブジェクト・reflection・IO・OS・network・debug・package・require・coroutine・動的loadを渡しません。
- 利用可: assert/error/ipairs/pairs/next/tonumber/tostring/type/select と math.abs/ceil/floor/min/max/sqrt/sin/cos/pi。
- string/table ライブラリ、metatable、pcall、文字列連結 `..` はありません。文字列の大量確保や実行予算の握り潰しを避けるためです。
- 大きな演算・常駐ループ・外部データ取得には対応しません。信頼する運営者が短いゲーム処理を編集するための機能です。

実装根拠: [LuaJ公式sandbox例](https://github.com/luaj/luaj/blob/master/examples/jse/SampleSandboxed.java) と [LuaJ 3.0.1 DebugLib](https://github.com/luaj/luaj/blob/v3.0.1/src/core/org/luaj/vm2/lib/DebugLib.java)。Luajava/OS/IOの標準環境は使用せず、命令監視だけをJava側に保持します。
