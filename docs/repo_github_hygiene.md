# Repo hygiene — chuẩn bị đẩy AURON lên GitHub

**Ngày quét:** 11/09/2026
**Phạm vi:** toàn bộ `D:\HOCTIN\GYM TRACKER` (không tính `.git`)
**Mục tiêu:** chỉ đẩy source code + README + docs, giảm dung lượng tối đa

---

## 1. Kết quả đo lường

| Chỉ số | Trước | Sau khi áp `.gitignore` |
|---|---|---|
| Dung lượng working tree | **6.3 GB** | **137,1 MB** |
| Số file | **87.017** | **1.845** |
| Giảm | — | **−97,9 %** (cả dung lượng lẫn số file) |

Dung lượng `.git` hiện tại: **379,34 MB** (10.872 loose object, 0 pack) — xem mục 4,
`.gitignore` **không** làm giảm được phần này.

---

## 2. Các nhóm "rác" đã loại (theo dung lượng)

| Dung lượng | File | Đường dẫn | Bản chất |
|---|---|---|---|
| 2,7 GB | ~35.000 | `_backup_*/`, `_asset_*/` | Bản sao lịch sử theo từng task, chứa cả `.jpg`, `.apk`, `build/` |
| 749,9 MB | 752 | `Verdent/` | Electron app đã cài (`Verdent.exe`, `app.asar`, `node_modules`) |
| 477,3 MB | 5.802 | `.claude/` | Git worktree + cache của agent |
| 463,2 MB | 9.049 | `.gradle-user/` | Gradle user home (cache `.jar`, `.aar`) |
| 163,6 MB | 1.780 | `graphify-out/` | Output knowledge graph, tái tạo bằng `graphify update .` |
| 96,9 MB | 2.664 | `external_catalog_sources/` | Nguyên liệu thô tải về để build catalog |
| 102,5 MB | 1.425 | `build/`, `app/build/`, `.gradle/` | Output build (`.dex`, `.class`, `.flat`, `.ap_`, APK) |
| 8,5 MB | 6 | `trash/` | Quarantine tạm |
| ~12 MB | ~35 | `qa-screenshots/`, `qa_*.png`, `qa-*.db` | Artifact QA |
| 2,0 MB | 97 | `qa_*.xml` (dump UI hierarchy ở gốc) | Artifact QA — xem cảnh báo bên dưới |
| 5,2 MB | 4 | `_asset_sources_hero_new/` | Ảnh staging gốc của hero |
| 592 KB | 7 | `ảnh mẫu dùng cho onboarding/` | Ảnh mẫu |
| 480 KB | 2 | `Confetti Partyyy!!.json`, `Trophy.json` | **Trùng md5 100 %** với `app/src/main/res/raw/auron_*.json` |
| 94 KB | 1 | `_backup_task8_advsheet_20260901_202417.kt` | File backup rời ở gốc |

---

## 3. Những gì được GIỮ (139,1 MB / 1.941 file)

| Dung lượng | File | Đường dẫn |
|---|---|---|
| 82,0 MB | 1.365 | `app/` — trong đó `src/main/assets/exercise_images` 66 MB (1.154 ảnh bài tập), `src/main/res` 12,8 MB, `src/main/java` 1,6 MB, `app/schemas` 360 KB |
| 36,2 MB | 131 | `docs/` — trong đó `docs/screenshots` 30,9 MB |
| 10,7 MB | 152 | `AndroidLiquidGlass-kmp/` — thư viện tham chiếu (không nằm trong `settings.gradle.kts`) |
| 7,4 MB | 149 | `PrismalAGSL-master/` — repo tham chiếu AGSL |
| 259,7 KB | 12 | File ở gốc: `README.md`, `AGENTS.md`, `CLAUDE.md`, `.graphifyignore`, `gradlew*`, `*.gradle.kts`, `AuronLiquidGlass.kt`, `LiquidGlassPhysicsTest.kt` |
| 454 KB | 19 | `react-muscle-highlighter-main/` — nguồn SVG path (đã ghi công trong `BodyMapPaths.kt`) |
| 126 KB | 13 | `tools/recovery-evaluation/` — script Python phân tích |
| 44,7 KB | 3 | `gradle/` — wrapper + `libs.versions.toml` |

### Nguyên tắc an toàn đã áp dụng

**Không ignore ảnh/JSON/XML toàn cục.** `app/src/main/assets` chứa 1.158 ảnh bài tập
(`jpg`/`webp`) và `app/src/main/res/raw` chứa 4 file Lottie JSON — đây là tài nguyên
runtime bắt buộc. Ignore theo đuôi file sẽ làm hỏng app. Vì vậy ảnh chỉ bị ignore ở
**thư mục gốc** (pattern có tiền tố `/`).

`*.jar` bị ignore nhưng có ngoại lệ bắt buộc:

```
!gradle/wrapper/gradle-wrapper.jar
```

Thiếu file này thì `./gradlew` không chạy được.

Đã kiểm chứng: 20 đường dẫn nguồn quan trọng (Kotlin, assets, schemas, docs, wrapper jar,
`libs.versions.toml`) đều **được giữ**, và 16 đường dẫn rác đều **bị ignore**.

### Cảnh báo: đừng chỉ dùng một kiểu dấu phân cách

Lần quét đầu chỉ dùng pattern `qa-*.xml` (gạch nối) nên **lọt 97 file** dump UI hierarchy
đặt tên bằng gạch dưới (`qa_muscle3c1_current.xml`, `qa_v311d_launch.xml`, …). Vì vậy
`.gitignore` dùng pattern bao trùm, chỉ match ở thư mục gốc:

```
/qa-*
/qa_*
/ui-*
/ui_*
```

Khi thêm rule mới, nên kiểm chứng lại bằng:

```bash
git ls-files -o --exclude-standard | grep -v "/"
```

Lệnh này liệt kê mọi file ở gốc còn sót lại; nếu thấy file QA nào thì rule chưa phủ hết.

---

## 4. Vấn đề nghiêm trọng: `.git` nặng 379 MB

`.gitignore` chỉ chặn file **mới**. Nó không xoá được thứ đã nằm trong lịch sử.

### Số liệu

| Nguồn | Dung lượng | Ghi chú |
|---|---|---|
| Object reachable từ `HEAD` | 14,0 MB / 186 file | Toàn vẹn — 186/186 blob giải được |
| Object reachable từ **tất cả ref** | **291,6 MB** / 4.218 blob | Phần sẽ được đẩy lên GitHub |
| Object rời rạc (loose) trên đĩa | 379,34 MB / 10.872 object | Bao gồm ~5.000 object không còn ref nào trỏ tới |

### Thủ phạm

1. **`refs/codex/turn-diffs/checkpoints/50af89.../b6de30.../1783449301640/4fcb3a61-...`**
   — một tree ref duy nhất trỏ tới **5.793 object**. Đây là checkpoint tự động của Codex,
   chụp lại toàn bộ workspace (kể cả `build/`, `_backup_*`). Đây là nguồn phình chính.
2. **15 branch rác `refs/heads/claude/*`** (mỗi branch 214 object, trỏ cùng một commit).
3. **5 git worktree** trong `.claude/worktrees/`.

### Hệ quả

Nếu `git push` ngay bây giờ, GitHub sẽ nhận **~291 MB lịch sử**, trong đó phần lớn là rác
mà `.gitignore` đã chặn ở working tree. Repo vẫn phình dù cây làm việc đã sạch.

### Cách xử lý (chạy sau khi đã xác nhận)

```bash
# 1) Bỏ theo dõi 131 file đã bị ignore nhưng còn trong index (KHÔNG xoá file trên đĩa)
git rm -r --cached _backup_20260603_153958 _backup_20260603_205629 \
    _backup_20260606_100541 _asset_sources_hero_new _asset_backup_hero_old \
    qa-screenshots

# 2) Xoá ref rác của agent (nguồn phình 291 MB)
git update-ref -d refs/codex/turn-diffs/checkpoints/50af8993d5992b36fd59675e67777b0258d033ac06a6eb736ba486874ba105ad/b6de300b985d2cf91e90b18dd26bd.../1783449301640/4fcb3a61-4237-4360-b56e-dad15672c33e
for b in $(git for-each-ref --format='%(refname:short)' refs/heads/claude); do
  git branch -D "$b"
done

# 3) Gỡ worktree của agent rồi dọn object
git worktree list --porcelain | awk '/^worktree/ {print $2}' | grep '\.claude/worktrees' \
  | while read w; do git worktree remove --force "$w"; done
git worktree prune

# 4) Nén lại object store (đưa .git từ ~380 MB xuống ~20 MB)
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

Sau bước 4, kiểm tra lại: `git count-objects -vH` và `git rev-list --objects --all | wc -l`.

### Phương án thay thế — tạo lịch sử mới hoàn toàn

Repo hiện chỉ có **2 commit** (`b731372` initial + `def7781` hygiene), nên giá trị lịch sử
gần như bằng không. Có thể bỏ hẳn lịch sử cũ:

```bash
git checkout --orphan clean-main
git add -A
git commit -m "AURON: initial public snapshot (source only)"
git branch -D master
git branch -m master
```

Cách này cho kết quả sạch tuyệt đối và không cần `gc` nặng.

---

## 5. Tuỳ chọn giảm thêm (chưa áp dụng)

Các mục dưới đây **đang được giữ** vì bạn yêu cầu giữ `docs` và toàn bộ code. Có thể loại
thêm nếu muốn repo nhẹ hơn nữa:

| Dung lượng | Đường dẫn | Nhận xét |
|---|---|---|
| 30,9 MB | `docs/screenshots/` | 38 ảnh PNG bằng chứng QA. Bỏ được nếu chấp nhận mất ảnh minh hoạ trong docs |
| 11,8 MB | `AndroidLiquidGlass-kmp/` | Thư viện tham chiếu, **không** có trong `settings.gradle.kts` (build chỉ `include(":app")`) |
| 7,4 MB | `PrismalAGSL-master/` | Repo tham chiếu AGSL, không build |
| 454 KB | `react-muscle-highlighter-main/` | Nguồn SVG path đã port vào `BodyMapPaths.kt` |

Nếu bỏ cả 4 mục: còn khoảng **87 MB**.

---

## 6. Khuyến nghị bổ sung

- **`.gitattributes`** — Git đang cảnh báo `LF will be replaced by CRLF`. Thêm
  `* text=auto eol=lf` và `*.bat text eol=crlf` để tránh nhiễu diff giữa Windows và GitHub.
- **Giới hạn GitHub** — file lớn nhất trong payload là 3,1 MB
  (`AndroidLiquidGlass-kmp/.../homescreen.png`), an toàn so với ngưỡng chặn 100 MB/file.
- **`external_catalog_sources/`** — nếu cần lưu vết nguồn gốc của catalog, nên đẩy lên
  object storage/release asset thay vì commit vào repo.
