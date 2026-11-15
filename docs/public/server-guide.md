# 御伽の運営ガイド

Paperサーバーへ御伽を導入し、自分たちのマップで試合を開くための手順です。一般プレイヤー向けの案内は[遊び方ガイド](guide.md)を参照してください。

## 導入に必要なもの

- このリポジトリが対象とする **Java 25 / Paper 26.2** のサーバー。
- 戦場として使う、ロード済みワールドと建築済みの地形。
- 管理用の権限 `otogi.admin`。初期設定ではOPに付与されます。
- 通常モードは2人以上、デュオは4人以上の偶数人数。参加人数分のスポーン地点。

Otogiは同時に1試合を進行します。戦場のワールド生成や地形の設置・消去は行いません。初回起動時はマップ未登録です。

## プラグインを入れる

リポジトリのルートでビルドします。Windowsの場合は次のコマンドを使います。

```powershell
.\gradlew.bat build
```

生成された `build/libs/Otogi-0.1.0-all.jar` を、停止中のサーバーの `plugins` フォルダーに配置して起動します。`-all` の付いたJARには必要なライブラリが含まれます。通常のJARだけを配置しないでください。

JARを更新するときも、サーバーを完全に停止してから入れ替え、再起動します。Bukkitの `/reload` は使いません。設定だけの変更には、試合とお試しを終えてから `/otogi reload` を使います。

## マップを1つ登録する

ここではIDを `garden`、表示名を「幽世の庭」とします。`otogi.admin` を持つプレイヤーが、使用するロード済みワールドに入って実行してください。

1. **マップを作成**：`/otogi map create garden 幽世の庭`
2. **対角の二点で範囲を設定**：一方で `/otogi map pos1 garden`、もう一方で `/otogi map pos2 garden`。高さを含む直方体です。床の下と、ジャンプ・高台・観戦地点より上まで含めます。
3. **スポーンを登録**：安全な足場に立ち、`/otogi map spawn garden add`。場所を変えて参加人数分を登録します。現在の向きも保存されます。
4. **観戦地点を登録**：範囲内の安全な場所で `/otogi map spectator garden`。
5. **復帰地点を登録**：範囲内の安全な場所で `/otogi map recovery garden`。
6. **拠点制圧用の中心を登録**：中心に立って `/otogi map point garden 5`。半径5ブロックです。拠点制圧を使わなければ省略できます。
7. **検証して選択**：`/otogi map validate garden` の案内に従って不足を直し、`/otogi map select garden`。

足場、頭上の空間、危険ブロックも検査されます。参加人数はスポーン数までです。デュオを4人で遊ぶなら、最低4点のスポーンを登録してください。

### 登録内容を直す

| コマンド | 用途 |
| --- | --- |
| `/otogi map list` | 登録済みマップを一覧表示 |
| `/otogi map info garden` | マップの情報を確認 |
| `/otogi map spawn garden list` | スポーンの番号と位置を確認 |
| `/otogi map spawn garden remove 2` | 2番のスポーンを削除 |
| `/otogi map radius garden 8` | 拠点の中心を動かさず半径を8へ変更 |
| `/otogi map validate garden` | 設定と安全性を確認 |

ゲーム内の設定操作は `plugins/Otogi/maps/garden.json` に保存されます。JSONを直接編集した場合は、試合終了後に `/otogi map reload` または `/otogi reload` で反映します。試合中はマップ設定を変更できません。

## 試合を始める

参加者全員がロビーで「試合に参加待機」を押すか、`/otogi game join` を実行します。運営はロビーの「マップ・モード」で選び、「待機者で試合開始」を押します。

コマンドで始める場合は、モードIDとマップIDを指定します。

```text
/otogi game start ffa garden
```

デュオを遊ぶ場合は4・6・8人などの偶数人数で待機し、次のように開始します。

```text
/otogi game start duo garden
```

モードIDは `ffa`、`last_stand`、`timed`、`bounty`、`king_of_hill`、`duo`。勝利条件は[遊び方ガイド](guide.md)にまとめています。

`/otogi game status` で状況を確認し、`/otogi game stop` で中止できます。中止した試合は通算戦績へ集計しません。通常終了は途中退出者を含めて集計し、退出者は勝利扱いになりません。

## ルールと演出を調整する

起動後の設定ファイルは `plugins/Otogi` にあります。編集前にバックアップを取り、試合とお試しをすべて終えてから `/otogi reload` で反映します。検証に失敗した設定は反映せず、従来の設定を維持します。

| ファイル | 主な内容 | 時間の単位 |
| --- | --- | --- |
| `config.yml` | 基礎体力、ビルド枠、乱戦・デュオのルール、標準能力 | 能力は秒。復活待ち・加護はtick |
| `combat.yml` | 具の威力・判定・攻撃間隔、防御、音と粒子 | tick |
| `modes.json` | 残機戦の残機・時間、拠点制圧の目標点・時間 | 秒 |
| `architects/*.json` | 追加能力の表示・数値・種類 | tick |
| `architects/scripts/*.lua` | 追加能力の短い動作処理 | 各APIの仕様に従う |
| `lang/ja_jp.json` | メニュー、通知、能力名、説明 | — |

20tick＝1秒、2HP＝1ハートです。`config.yml` でも `ffa.respawn-delay-ticks` と `ffa.spawn-protection-ticks` はtickなので注意してください。

### 基本ルールの例

次は `config.yml` の該当部分です。ファイル全体を置き換えず、既存項目を編集します。

```yaml
base-health: 40.0
build:
  size: 5
ffa:
  match-seconds: 600
  kill-limit: 10
  spawn-protection-ticks: 60
  respawn-delay-ticks: 100
duo:
  match-seconds: 600
  kill-limit: 20
```

基礎体力は1〜1024HP、ビルド枠は1〜9です。復活待ちと加護の設定は、デュオを含む復帰のあるモードで共通に使います。能力による追加体力は基礎体力とは別に適用します。

残機戦・拠点制圧の初期設定は次の通りです。

```json
{
  "lastStand": {"lives": 3, "matchSeconds": 600},
  "kingOfHill": {"targetPoints": 60, "matchSeconds": 600}
}
```

時間制乱戦の3分と、賞金争奪の目標20点は現在の実装で固定されています。賞金争奪の制限時間は `ffa.match-seconds` に従います。

### 音・境界の表示

`combat.yml` の `feedback` には、`sound`、`particles`、`volume`、`boundary`、`warp-particles`、`warp-sound`、`kill-sound`、`kill-volume`、`kill-pitch` があります。粒子を切っても当たり判定は変わりません。CTの表示方式は各プレイヤーがロビーから変更できます。

### 文言・能力の編集

`lang/ja_jp.json` の `{0}`、`{1}` は差し込み用なので残します。単一引用符を表示する場合は `''` と書きます。能力の表示名や説明を変えたときは、公開ガイドと能力図鑑も更新してください。

JSON・Luaの詳細は、リポジトリ内の開発資料 `docs/script-architects.md` を参照してください。

## 持ち物・戦績・ワールド設定

試合終了・退出・中止やプラグイン停止時には、参加前の持ち物、体力、効果、経験値、ゲームモード、位置、スコアボードなどを復元します。ビルドは `builds.yml`、通算戦績は `player-stats.json` に保存します。お試しの成績は記録しません。

ロケーターバーの無効化は**ワールド全体**に適用され、同じワールドの試合外プレイヤーにも影響します。最後の試合・お試し利用者が抜けると、元の設定へ戻します。運営時には専用の戦場ワールドを用意すると管理しやすくなります。

## 公開前に確認する

- 実際の接続先・参加方法・参加可能時間を、サーバーの告知へ記載する。
- JavaとPaperの対象バージョン、JARの起動ログを確認する。
- マップ検証を通し、実際の人数で開始・復活・終了・退出を試す。
- 持ち物や位置が戻ること、通常終了だけが戦績に入ることを確認する。
- デュオは4人以上で、相方表示・味方への攻撃・チーム得点を確認する。
- 自分たちの調整値と、公開ガイドの説明を照合する。

この資料は現行ソースと同梱設定に基づく手順です。公開サーバーでの動作確認は、使用する環境・マップ・人数で行ってください。
