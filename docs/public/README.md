# 御伽・公開用ドキュメント

- [HTML版の入口](index.html)：Minecraft風の字体とインベントリ風の装飾で読む遊び方ガイド。
- [遊び方・Markdown版](guide.md)：告知やWikiへ転載しやすい原稿。
- [能力図鑑](abilities.md)：同梱54種の名前・分類・説明。
- [運営ガイド](server-guide.md)：導入から試合開催、設定変更まで。

## 閲覧・配布

`index.html` をブラウザーで開きます。フォント、CSS、JavaScriptはこのフォルダー内に同梱し、外部通信なしで閲覧できます。配布時は `assets` を含め、フォルダーごと渡してください。

HTML版には文字の読みやすさを切り替えるボタン、能力の検索・分類絞り込み、印刷用のスタイルがあります。印刷はブラウザーの機能から行えます。JavaScriptが無効でも本文と全能力を読めます。Markdown自体の字体は表示先のアプリに従います。

## 原稿の更新

`guide.md` と `server-guide.md` が手書きの原稿です。編集後、リポジトリルートで次を実行します。Python 3の標準ライブラリだけを使います。

```text
python tools/build_public_docs.py
```

HTML3ページと `abilities.md` を生成します。図鑑はJavaの標準能力ID・追加JSON・日本語翻訳から作成するため、生成済みの図鑑を直接編集しないでください。HTML用のデザインは `assets/book.css`、操作は `assets/book.js` にあります。生成処理にネットワーク接続は不要です。

数値は同梱の初期設定です。能力説明はゲーム内の翻訳原文を転記するため、サーバー独自の数値へ自動追従しません。ルールや同梱能力が変わった際は原稿も見直してください。

## GitHub Pages で公開する

1. リポジトリの [Settings → Pages](https://github.com/akkiserver-dev/Architect/settings/pages) を開き、**Build and deployment → Source** を **GitHub Actions** に設定します。
2. ワークフローと公開用ファイルを `master` に反映します。公開用ファイルには、生成済みの HTML・Markdown と `assets` が必要です。
3. [Actions](https://github.com/akkiserver-dev/Architect/actions) で **Publish Otogi guide** の成功を確認します。初回の push 後に Pages を有効にした場合は、同じワークフローの **Run workflow** で `master` を選んで実行します。

公開先は [https://akkiserver-dev.github.io/Architect/](https://akkiserver-dev.github.io/Architect/) です。公開サイトにはガイド、能力図鑑、運営ガイドとその素材だけを配置します。

更新時は上のコマンドで HTML と図鑑を再生成し、生成物と原稿をコミットしてください。`master` に公開用ファイルの変更が入ると再公開されます。ワークフローが公開するのはコミット済みのファイルです。

## フォント

日本語ドット書体 **DotGothic16** を使用しています。フォント本体はGoogle Fontsリポジトリの未改変版、ライセンスはSIL Open Font License 1.1です。Minecraft本体のフォントは含みません。

- 作者：[The DotGothic16 Project Authors](https://github.com/fontworks-fonts/DotGothic16)
- 取得元：[Google Fonts / DotGothic16](https://github.com/google/fonts/tree/main/ofl/dotgothic16)
- 同梱ライセンス：[assets/fonts/OFL.txt](assets/fonts/OFL.txt)

Minecraftの画像やテクスチャは使用していません。HTMLのピクセル風装飾はこのガイド用に作成したものです。
