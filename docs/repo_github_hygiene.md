# Repo hygiene — chuẩn bị đẩy AURON lên GitHub

**Ngày quét:** 11/09/2026
**Phạm vi:** toàn bộ `D:\HOCTIN\GYM TRACKER` (không tính `.git`)
**Mục tiêu:** chỉ đẩy source code + README + docs, giảm dung lượng tối đa

**Trạng thái:** đã thực thi. Commit `ed5d609` — cây làm việc sạch, 0 file rác còn tracked.
Chưa `push` (repo chưa có remote) — xem mục 8.

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

## 3. Những gì được GIỮ (137,1 MB / 1.845 file)

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

`git push --all` hoặc `--mirror` sẽ đẩy **~291 MB lịch sử**, phần lớn là rác mà `.gitignore`
đã chặn ở working tree. Tuy nhiên `git push origin master` thì **không** — xem mục dưới.

### Điểm mấu chốt: KHÔNG cần xoá ref Codex để GitHub sạch

`git push origin master` **chỉ gửi object reachable từ `master`**. Ref không được push sẽ ở
lại máy local. Nghĩa là có thể có repo GitHub sạch mà **không mất một byte dữ liệu nào**:

| | Đẩy riêng `master` | Đẩy `--all` / `--mirror` |
|---|---|---|
| GitHub nhận | ~151 MB (cây sạch + 3 commit) | ~291 MB (kèm checkpoint Codex) |
| Ref Codex | giữ nguyên ở local | bị đẩy lên |
| 15 branch `claude/*` | không lên GitHub | lên GitHub |
| `.git` local | vẫn 379 MB | vẫn 379 MB |

**Khuyến nghị: đẩy riêng `master`.** Chỉ dùng `--all` / `--mirror` nếu thật sự muốn 15 branch
`claude/*` xuất hiện trên GitHub.

Lưu ý: 15 branch `claude/*` đều trỏ **cùng một commit** `def77819` với `master`, nên chúng
chiếm **0 byte** dữ liệu riêng. Xoá chúng chỉ để gọn danh sách branch, không giảm dung lượng.

### Ref Codex chứa gì (đo ngày 11/09/2026)

Checkpoint được tạo **07/07/2026 18:35 UTC** (65 ngày trước), là ảnh chụp toàn workspace:

| | Số object | Dung lượng |
|---|---|---|
| Tổng reachable từ ref | 5.793 | 289,8 MB |
| **Duy nhất so với `master`** | **5.682** | **277,6 MB** |
| Dùng chung với `master` | 111 | 12,2 MB |

Thành phần 5.682 đường dẫn duy nhất: `external_catalog_sources/` 3.078 · `app/src/` 1.828 ·
file gốc 199 · `qa-screenshots/` 89 · `docs/` 57 · ~25 thư mục `_backup_*`.

**Kết luận:** xoá ref này = mất khả năng rollback về checkpoint 07/07, gồm cả một bản
`app/src` cũ. **Không ảnh hưởng việc dùng Codex về sau** — Codex tạo checkpoint mới cho mỗi
turn, và `.codex/` chỉ chứa `config.toml` + `hooks.json`, không chứa dữ liệu phiên.

### Nếu vẫn muốn `.git` local nhỏ lại — archive trước khi xoá

```bash
# 0) Lưu checkpoint ra ngoài repo (khôi phục được bằng git clone <file>.bundle)
git bundle create D:/codex-checkpoint-backup.bundle <ref-codex-day-du>

# 1) Xoá ref Codex
git update-ref -d <ref-codex-day-du>

# 2) Detach worktree TRƯỚC khi xoá branch (xem cảnh báo bên dưới)
for w in .claude/worktrees/*/; do git -C "$w" checkout --detach; done
for b in $(git for-each-ref --format='%(refname:short)' refs/heads/claude); do
  git branch -D "$b"
done

# 3) Nén object store (đưa .git từ ~380 MB xuống ~20 MB)
git reflog expire --expire=now --all
git gc --prune=now
```

### CẢNH BÁO: 3 worktree đang có thay đổi CHƯA commit

`git worktree remove --force` xoá **cả thư mục**, sẽ mất phần chưa commit:

| Worktree | Số thay đổi | Nội dung |
|---|---|---|
| `peaceful-raman-3788b1` | **20** | 7 file nguồn đã sửa (`Formatters.kt`, `GymTrackerApp.kt`, `HomeScreen.kt`, `HomeViewModel.kt`, `OnboardingScreen.kt`, `hero_default.xml`, `strings.xml`) + package mới `ui/detail/` + 4 PNG hero |
| `pedantic-cohen-265975` | 1 | `.claude/settings.local.json` |
| `pedantic-nash-5141da` | 1 | `_verify_tmp.py` |

Vì vậy: dùng `checkout --detach` (giữ nguyên thư mục + thay đổi) thay vì
`worktree remove --force`. **Không xoá** `peaceful-raman-3788b1` trước khi xác nhận package
`ui/detail/` đã được hợp nhất vào cây chính.

### Phương án thay thế — tạo lịch sử mới hoàn toàn

Repo hiện có **3 commit**, nên giá trị lịch sử thấp. Có thể bỏ hẳn lịch sử cũ:

```bash
git checkout --orphan clean-main
git add -A
git commit -m "AURON: initial public snapshot (source only)"
git branch -D master
git branch -m master
```

Cách này cho cây sạch tuyệt đối và `.git` chỉ còn ~140 MB. Nhược điểm: mất 3 commit cũ.
**Không cần thiết** nếu bạn chỉ đẩy riêng `master` như khuyến nghị ở trên.

---

## 5. Tuỳ chọn giảm thêm (chưa áp dụng)

Các mục dưới đây **đang được giữ** vì bạn yêu cầu giữ `docs` và toàn bộ code. Có thể loại
thêm nếu muốn repo nhẹ hơn nữa:

| Dung lượng | Đường dẫn | Nhận xét |
|---|---|---|
| 30,9 MB | `docs/screenshots/` | 38 ảnh PNG bằng chứng QA. Bỏ được nếu chấp nhận mất ảnh minh hoạ trong docs |
| 11,8 MB | `AndroidLiquidGlass-kmp/` | Thư viện tham chiếu (Kyant0), **không** có trong `settings.gradle.kts` |
| 7,4 MB | `PrismalAGSL-master/` | Repo tham chiếu AGSL (styropyr0), không build |
| 454 KB | `react-muscle-highlighter-main/` | Nguồn SVG path đã port vào `BodyMapPaths.kt` |

Nếu bỏ cả 4 mục: còn khoảng **87 MB**.

### 3 repo tham chiếu có ảnh hưởng gì tới công việc liquid glass?

Đã kiểm chứng bằng build file: `app/build.gradle.kts` chỉ khai báo dependency qua toạ độ
Maven (`libs.androidx.*`, `libs.lottie.compose`), **không có `project(...)`** nào trỏ tới
3 thư mục này. `settings.gradle.kts` cũng chỉ `include(":app")`.

→ **Cả 3 đều KHÔNG được biên dịch vào app.** Xoá chúng không thể làm hỏng build.

Giá trị tham khảo khi đang làm liquid glass:

- `AndroidLiquidGlass-kmp` — đây chính là bản tham chiếu kiến trúc mà lens của dự án dựa
  theo (Kyant dùng `graphicsContext.createGraphicsLayer()` + `drawLayer()`). Hữu ích khi
  còn tinh chỉnh hiệu ứng.
- `PrismalAGSL-master` — tham chiếu AGSL.
- `react-muscle-highlighter-main` — SVG path **đã port xong** vào
  `ui/muscleheat/BodyMapPaths.kt`; repo gốc chỉ còn giá trị ghi công (MIT).

**Khuyến nghị: giữ trên đĩa, ignore khỏi git.** Chúng là repo của bên thứ ba, clone lại được
từ upstream bất cứ lúc nào; đẩy vào repo của bạn vừa trùng lặp upstream vừa thêm 18,6 MB.
Vì vẫn nằm trên đĩa nên công việc liquid glass không bị ảnh hưởng gì. Nếu muốn giữ chúng
trên GitHub cho tiện thì để nguyên — đây là lựa chọn hợp lệ, chỉ tốn 18,6 MB.

---

## 6. Khuyến nghị bổ sung

- **`.gitattributes`** — Git đang cảnh báo `LF will be replaced by CRLF`. Thêm
  `* text=auto eol=lf` và `*.bat text eol=crlf` để tránh nhiễu diff giữa Windows và GitHub.
- **Giới hạn GitHub** — file lớn nhất trong payload là 3,1 MB
  (`AndroidLiquidGlass-kmp/.../homescreen.png`), an toàn so với ngưỡng chặn 100 MB/file.
- **`external_catalog_sources/`** — nếu cần lưu vết nguồn gốc của catalog, nên đẩy lên
  object storage/release asset thay vì commit vào repo.

---

## 7. Về việc tách thành nhiều file `.gitignore`

Git **có** hỗ trợ `.gitignore` lồng nhau: file đặt trong thư mục con chỉ áp dụng cho cây con
đó. Tuy nhiên với repo này, tách file **không giảm được dung lượng**, vì hai lý do:

1. **Không có dự án Node.js nào của bạn.** Lệnh
   `find . -maxdepth 4 -name package.json` chỉ tìm thấy **một** kết quả:
   `react-muscle-highlighter-main/.../package.json` — thuộc repo tham chiếu bên thứ ba.
   `node_modules/` chỉ tồn tại trong `Verdent/` (app đã cài) và trong các `_backup_*`.
   Vậy nên "file .gitignore cho Node.js đặt cạnh package.json" không có đối tượng áp dụng.

2. **~95% rác nằm ở thư mục GỐC** — `_backup_*/`, `Verdent/`, `.claude/`, `.gradle-user/`,
   `graphify-out/`, `external_catalog_sources/`. Một file `.gitignore` lồng nhau **không thể**
   ignore thứ nằm ngoài cây con của nó, nên file gốc vẫn phải giữ đầy đủ các rule này.

Kết luận: tách file là hợp lệ và vẫn idiomatic, nhưng ở repo này lợi ích bằng 0 về dung lượng —
chỉ là tổ chức lại. Chỉ nên tách khi bạn thực sự thêm một toolchain JS/Node mới (ví dụ web
dashboard trong `web/`).

Nếu vẫn muốn cấu trúc module, cách chia hợp lý cho repo này:

| File | Phạm vi | Nội dung |
|---|---|---|
| `/.gitignore` (gốc) | Toàn repo | OS/editor, Node/Python generic, backup & asset staging, QA artifact, tooling/agent, app cài kèm, signing |
| `/app/.gitignore` | Module Android | `build/`, `*.apk`, `*.aab`, `*.dex`, `*.class`, `*.kotlin_module`, `externalNativeBuild/` |

**Bắt buộc giữ** trong file gốc: ngoại lệ `!gradle/wrapper/gradle-wrapper.jar`.

---

## 8. Lệnh đẩy lên GitHub

Repo **chưa có remote** (`git remote -v` trống). Sau khi commit `ed5d609`:

```bash
# 1) Tạo repo trống trên GitHub trước (KHÔNG tick "Add README/.gitignore")

# 2) Gắn remote (thay <user> và <repo>)
git remote add origin https://github.com/<user>/<repo>.git

# 3) Đẩy DUY NHẤT nhánh master -> GitHub sạch, ref Codex ở lại local
git push -u origin master
```

Kiểm tra sau khi đẩy: `git count-objects -vH` (local vẫn 379 MB — bình thường) và xem dung
lượng repo trên GitHub (~151 MB).

**Cảnh báo:** không dùng `git push --all` / `--mirror` / `git push origin --tags` nếu không
muốn đẩy checkpoint Codex và 15 branch `claude/*` lên GitHub.
