# Hướng dẫn vận hành — AURON trên GitHub

Tài liệu này mô tả repo đã được thiết lập ra sao, cách dùng hằng ngày, cách agent Claude trên
Notion làm việc với nó, và những hạn chế cần biết. Cập nhật: 11/09/2026.

---

## 1. Thiết lập hiện tại

| Hạng mục | Giá trị |
|---|---|
| Remote | `https://github.com/nguyenthehuyhoang157tc-prog/GYM-TRACKER.git` |
| Nhánh | `master` (cũng là nhánh mặc định trên GitHub) |
| Danh tính commit | `nguyenthehuyhoang157tc-prog <248923513+nguyenthehuyhoang157tc-prog@users.noreply.github.com>` |
| Số file theo dõi | 1.849 (~146 MB) |
| CI | `.github/workflows/android-ci.yml` — compile + unit test mỗi push/PR |
| Visibility | Private |

`master` là nhánh mặc định cả ở local lẫn GitHub. Không dùng `main`.

---

## 2. Danh tính commit — vì sao là địa chỉ noreply

Địa chỉ **noreply** ở trên **không phải email thật** — nó do GitHub cấp riêng cho tài khoản
(`<user-id>+<username>@users.noreply.github.com`). Commit vẫn được gắn vào profile của bạn
(avatar, contribution graph) nhưng email thật không bị lộ.

**Không thể dùng `nguyenthehuyhoang157tc@gmail.com`.** Tài khoản đang bật tuỳ chọn *"Block
command line pushes that expose my email"*, nên GitHub từ chối thẳng:

```
remote: error: GH007: Your push would publish a private email address.
```

Nếu sau này bạn muốn dùng email thật, có hai cách: tắt tuỳ chọn đó tại
`github.com/settings/emails` (email sẽ nằm vĩnh viễn trong metadata commit, công khai), hoặc
giữ nguyên địa chỉ noreply như hiện tại.

Cấu hình đã đặt ở **cả hai mức** (global cho mọi repo, và repo-level cho repo này):

```bash
git config --global user.email   # -> 248923513+nguyenthehuyhoang157tc-prog@users.noreply.github.com
git config user.email            # -> giống trên
```

---

## 3. Quy trình làm việc hằng ngày

```bash
cd "D:/HOCTIN/GYM TRACKER"

# 1) Sửa code...

# 2) BẮT BUỘC: sau mỗi lần sửa Composable, compile ngay
export JAVA_HOME='D:/jdk17/jdk-17.0.19+10'
export PATH="/d/jdk17/jdk-17.0.19+10/bin:$PATH"
./gradlew :app:compileDebugKotlin

# 3) Chạy test (khuyến nghị trước khi push)
./gradlew :app:testDebugUnitTest

# 4) Xem đúng những gì sẽ được commit
git status --short

# 5) Commit + push
git add -A
git commit -m "mô tả ngắn gọn thay đổi"
git push origin master
```

**Đừng dùng `git push --all` hay `--mirror`.** Chúng sẽ đẩy lên cả checkpoint Codex
(277 MB dữ liệu riêng) và 15 nhánh `claude/*` còn nằm ở local. Chỉ đẩy `master`.

### Nếu push bị từ chối

| Thông báo | Nguyên nhân | Cách xử lý |
|---|---|---|
| `GH007 ... private email address` | Email commit đang là email thật | Kiểm tra `git config user.email`; phải là địa chỉ noreply |
| `stale info` (khi force-push) | Ref tracking ở local không đọc được | Dùng dạng tường minh: `git push --force-with-lease=refs/heads/master:<sha-cu> origin master` |
| `Permission denied` cho `./gradlew` | Mất bit thực thi | `git update-index --chmod=+x gradlew` |

---

## 4. Agent Claude trên Notion làm việc thế nào

Agent đọc repo qua GitHub API. Những gì nó có và không có:

**Có:** toàn bộ source Kotlin/XML, `app/src/main/assets` (1.154 ảnh bài tập + catalog JSON),
`app/schemas` (10 file schema Room), toàn bộ build config, 132 file `docs/`, và 3 thư mục
tham chiếu (`AndroidLiquidGlass-kmp`, `PrismalAGSL-master`, `react-muscle-highlighter-main`).

**Đọc trước tiên:** `CLAUDE.md` — brief dành riêng cho agent, gồm các quy tắc bắt buộc và trỏ
tới `docs/engineering_notes.md` (bài học AGSL, bug dialog-insets, kế hoạch set-type).

**Không có:**
- `local.properties` — agent tự cấp `ANDROID_HOME`.
- Keystore ký app — **không thể tạo APK release đã ký**.
- `graphify-out/graph.json` — có, nhưng là **ảnh chụp 06/09**, không phải index sống.
- Bộ nhớ phiên làm việc local của bạn (`.workbuddy-ai/memory/`).

### Hạn chế lớn nhất: agent không tự compile được

Nếu tích hợp Notion chỉ có quyền GitHub API (không có sandbox có JDK + Android SDK), agent
**sửa code mà không biên dịch**, tức là không biết mình vừa làm hỏng build hay chưa. CI chính
là lưới an toàn bù cho việc đó: mọi push/PR đều được compile + chạy test tự động.

**Hệ quả thực tế:** với việc nhỏ, phạm vi hẹp thì ổn. Với việc đụng Compose UI hoặc AGSL
liquid glass thì nên yêu cầu agent mở PR để CI kiểm tra trước khi merge, thay vì commit thẳng
vào `master`.

---

## 5. CI hoạt động ra sao

`.github/workflows/android-ci.yml` chạy khi push hoặc mở PR vào `master`/`main`:

1. Checkout code
2. Cài JDK 17 (Temurin)
3. Cài Android SDK (`android-actions/setup-android@v3`)
4. Cài Gradle + cache
5. `chmod +x ./gradlew`
6. `./gradlew :app:compileDebugKotlin`
7. `./gradlew :app:testDebugUnitTest`
8. Nếu fail: upload báo cáo test

Xem kết quả tại **`https://github.com/nguyenthehuyhoang157tc-prog/GYM-TRACKER/actions`**.

Đã kiểm chứng chạy được: cả hai lệnh trên đều xanh ở local (468 test, 0 failure), và cũng
xanh trong một bản checkout sạch mô phỏng môi trường CI.

**Hạn chế:**
- Repo **Private** nên Actions tiêu tốn phút trả phí (gói Free: 2.000 phút/tháng; build
  Android khoảng 5–10 phút/lần, lần đầu lâu hơn vì phải tải dependency).
- Không build được APK release đã ký, vì keystore cố ý không nằm trong repo. Nếu cần, phải
  truyền keystore dạng **base64 qua GitHub Actions Secret** rồi giải mã trong workflow.
- Không có bước chạy trên thiết bị/emulator — CI chỉ compile và chạy unit test.

---

## 6. Những gì CỐ Ý không nằm trong repo

| Không có | Vì sao | Nếu cần thì sao |
|---|---|---|
| `local.properties` | Chứa đường dẫn SDK riêng của máy bạn | Đặt biến `ANDROID_HOME` |
| `gymate-release.jks`, `keystore.properties` | Khoá ký app — lọt vào git history là vĩnh viễn | Dùng GitHub Actions Secret |
| `_backup_*/` (2,7 GB) | Bản sao lịch sử theo từng task | Không cần |
| `build/`, `.gradle/`, `.gradle-user/` | Output build, sinh lại được | — |
| `Verdent/` (750 MB) | Electron app đã cài, không phải source | — |
| `.claude/` (477 MB) | Worktree + cache của agent | — |
| `graphify-out/cache/` + snapshot ngày (163 MB) | Chỉ commit `graph.json` + `GRAPH_REPORT.md` | — |
| `external_catalog_sources/` (97 MB) | Nguyên liệu thô; bản app dùng đã có ở `assets/catalog/` | — |
| `qa-*`, `qa_*` | Artifact QA | — |

---

## 7. Lưu ý kỹ thuật quan trọng (đã từng gây sự cố)

**1. Ký tự xuống dòng (CRLF) — đừng bao giờ băm byte thô.**
Máy bạn đặt `core.autocrlf = true`, nên checkout mới ghi CRLF trong khi blob trong git là LF.
Test `WearableProtectedBoundaryTest` từng đóng băng 3 file bằng SHA-256 byte thô, nên nó
**pass ở workspace nhưng fail ở mọi bản clone sạch**. Đã sửa để chuẩn hoá CRLF→LF trước khi
băm. Nếu viết guard kiểu này, phải chuẩn hoá trước.

**2. Object store của `.git` local rất mong manh.**
Một lần `git rebase --root` đã kích hoạt `gc.auto`, bị ngắt giữa chừng và **xoá mất 2 commit**
(khôi phục được nhờ `git fetch` từ GitHub). Quy tắc:
- Luôn thêm `-c gc.auto=0` khi viết lại lịch sử.
- Không chạy lệnh git dài ở foreground — dễ bị timeout ngắt giữa chừng.
- Ưu tiên dựng lại chuỗi commit bằng `git commit-tree` (chỉ ghi 1 object mỗi commit) thay vì
  rebase. Kiểm chứng `git diff --quiet <cũ> <mới>` phải rỗng trước khi force-push.

**3. Ref store có lỗi riêng.** `git update-ref`/`git fetch` **không tạo được** loose ref ở độ
sâu 4 tầng (`refs/remotes/origin/master`) — chúng báo thành công nhưng không ghi gì, khiến
`git status` hiện `[gone]`. Khắc phục bằng cách ghi file ref thủ công:

```bash
mkdir -p .git/refs/remotes/origin
printf '%s\n' "$(git rev-parse master)" > .git/refs/remotes/origin/master
printf 'ref: refs/remotes/origin/master\n' > .git/refs/remotes/origin/HEAD
```

**4. Luôn test build từ bản clone sạch**, không phải từ working tree. Working tree có đủ file
bị ignore nên che mất đúng loại lỗi mà mọi agent/CI sẽ gặp:

```bash
git clone . .tmp-freshclone
cd .tmp-freshclone
export JAVA_HOME='D:/jdk17/jdk-17.0.19+10'
export ANDROID_HOME='C:/Users/hoang/AppData/Local/Android/Sdk'
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
cd .. && rm -rf .tmp-freshclone
```

**5. Dòng "BUILD SUCCESSFUL" không chứng minh test đã chạy.** Gradle có thể báo
`up-to-date`. Muốn chắc, dùng `--rerun-tasks`, hoặc đếm kết quả trong
`app/build/test-results/testDebugUnitTest/*.xml`.

---

## 8. Những gì còn nằm ở local (không đẩy lên, không ảnh hưởng GitHub)

| Còn lại | Kích thước | Ghi chú |
|---|---|---|
| `.git` | ~380 MB | Phần lớn là checkpoint Codex. Không đẩy lên nếu chỉ push `master`. |
| `refs/codex/turn-diffs/checkpoints/...` | 277 MB dữ liệu riêng | Checkpoint 07/07/2026. Xoá được nhưng mất khả năng rollback về mốc đó; Codex vẫn dùng bình thường. |
| 15 nhánh `claude/*` | 0 byte riêng | Tất cả trỏ cùng commit với `master`. Chỉ để gọn danh sách. |
| 5 worktree trong `.claude/worktrees/` | — | Đã kiểm chứng: **nội dung cũ từ 11/06**, đã được tích hợp vào cây chính, không có gì bị mất. |

Muốn dọn `.git` local cho nhẹ, xem mục 4 của `docs/repo_github_hygiene.md`.
