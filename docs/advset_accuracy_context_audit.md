# ADVSET-CONTEXT — Audit ngữ cảnh: menu "⋮" trong Log Workout vs Heat / Recovery / PO Engine

Mục đích: ghi nhận **chính xác những gì code đang làm** trước khi bàn tới việc làm cho heat map,
muscle recovery và PO engine phản ánh đúng dropset / rest-pause / warmup. Tài liệu này **không đề xuất
phương án thực hiện**.

Nguồn sự thật: mã nguồn trong `app/src/main/java/com/hoctin/gymtracker/`, đọc ngày 2026-09-08.

---

## 1. Dấu "⋮" trong Workout Log thực sự làm gì

Vị trí: `ui/log/LogWorkoutScreen.kt:1380` (`SetRowMenu`) → mở `AdvancedSetSheet`
(`LogWorkoutScreen.kt:1780-1843`). Một sheet, không phải dropdown.

Nội dung sheet:

| Khối | Giá trị | Ghi chú |
|---|---|---|
| Tiêu đề | `Set #N` hoặc `Continuation of Set #N` | |
| **Set Type** | `Straight Set` / `Warmup Set` / `Dropset` | enum `LogSetType { STRAIGHT, WARMUP, DROP }` (`LogWorkoutInteractionModels.kt:7`). **Bị disable trên continuation row.** |
| **Weight Distribution** | `Total` / `Each Side` / `Left Side` / `Right Side` | enum `LogWeightDistribution` (`:10`). **Bị disable trên continuation row.** |
| Footer — primary | `Continue this set` | Ẩn trên continuation row. |
| Footer — destructive | `Delete Set` | |

Hiển thị trên row: `SetTypeBadge` (`:1396`) vẽ badge **W** (WARMUP) hoặc **D** (DROP), STRAIGHT không
vẽ gì. `SetIndexBadge` (`:1346`) vẽ số thứ tự, hoặc icon mũi tên rẽ nhánh cho continuation row — tức
là continuation row **không có số set riêng**.

### "Continue this set" chính là cơ chế rest-pause / dropset

`LogWorkoutViewModel.continueSet()` (`:504-519`):

```kotlin
drafts.add(index + 1, SetDraft(weight = base.weight, rir = base.rir, continuationOfKey = base.researchRowKey))
```

- Chèn một draft con ngay sau base set.
- **Pre-fill `weight` và `rir` từ base, để `reps` trống** → nếu giữ nguyên tạ và nhập reps ít hơn
  (cluster ngắn) thì đó là **rest-pause**; nếu hạ tạ xuống thì đó là **dropset**.
- Mỗi base set chỉ có tối đa **1** continuation (`continueSet` return sớm nếu đã tồn tại).
- `continueLastSet()` (`:521`) tìm base set cuối cùng đủ điều kiện (không phải continuation, đã hoàn
  thành, chưa có con).

`continuationOfKey == null` là thứ phân biệt base / continuation ở khắp nơi: đếm `completedBaseSetCount`,
hiện/ẩn `Continue this set`, disable Set Type và Weight Distribution.

### Điều quan trọng nhất: **không có gì trong số này được lưu xuống DB**

| Layer | Trạng thái |
|---|---|
| `LoggedSetEntity` (`data/Entities.kt:141-165`) | **Không có** cột set type, không có parent/continuation key. |
| `SetInput` (`data/GymRepository.kt:13-25`) | **Không có** set type. Chỉ có `sourceDraftOrdinal` + `sourceDraftKey` (phục vụ audit/wearable). |
| `buildInputs()` (`LogWorkoutViewModel.kt:1027-1046`) | Chỉ copy `weight/reps/rir/duration/distance/notes` + 2 khoá audit. `setType` và `continuationOfKey` **bị bỏ lại**. |
| `GymRepository` (`:494-509`) | `setNumber = index + 1` — đánh số lại toàn bộ row đã parse. |

Hệ quả: warmup, dropset và rest-pause cluster khi lưu **trở thành các set thường, bình đẳng, được đánh
số tuần tự**. `README.md:205-209` nói rõ đây là chủ ý: *"intentionally transient: they do not yet change
progression, volume, History, recovery or saved set semantics."*

`setType` hiện chỉ ảnh hưởng **đúng một** thứ: cờ UI tạm thời
`repeatedDropSetCaution` (`LogWorkoutViewModel.kt:986-988`) — bật khi có ≥3 draft DROP trên bài
`COMPOUND_LOAD`. Cờ này cũng không được lưu.

---

## 2. Muscle Heat map đang đọc dữ liệu thế nào

Hai layer, theo `docs/muscle0_heatmap_contract_audit.md`.

**Layer A — `MuscleExposureCalculator` (`domain/muscleheat/MuscleExposure.kt`)**

```kotlin
val valid = snapshot.sets.filter { isValidSet(snapshot.trackingType, it) }   // :108
val completedSets = valid.size
val baseSetEquivalent = completedSets.toDouble()
```

- `isValidSet` (`:167`): rep-based → `reps > 0`; time/distance → `duration > 0 || distance > 0`.
  **Chỉ vậy.** Không có khái niệm warmup / drop / rest-pause.
- Mỗi set hợp lệ = **1.0 set-equivalent**. Không có hệ số nào cả.
- `setEquivalent = completedSets × completionRatio`, rồi nhân role weight: primary `1.0`,
  secondary `0.5`.
- `rawIntensityScore = primary + secondary` — "exposure ONLY", không có chất lượng.

**Layer B — `MuscleHeatEngine` (`domain/muscleheat/MuscleHeatEngine.kt`)**

- `finalIntensityScore = raw × qualityModifier` (`:78`). Bảng quality theo **avg RIR** (`:128`):
  RIR ≥ 4 → 0.40, 3–4 → 0.60, 0.5–3 → 0.95, < 0.5 → 0.70. SKILL_FORM cố định 0.60;
  time/distance cố định 0.50.
- `colorBand`: < 2.0 → LIGHT_YELLOW, < 4.0 → YELLOW_ORANGE, < 7.0 → ORANGE_RED, else DEEP_RED.
- `status`: raw < 2.0 → UNDERTRAINED; `grindShare ≥ 0.5 && raw ≥ 4.0` → RECOVERY_RISK;
  raw ≥ 7.0 → FATIGUED; quality ≥ 0.65 → PRODUCTIVE; else STIMULATED.
- `grindShare` (`:162`): tỷ trọng set-equivalent có `avgRir ≤ 0.5` trong các contributor rep-based.

---

## 3. Muscle Recovery đang đọc dữ liệu thế nào

`domain/muscleheat/MuscleRecovery.kt` — MUSCLE-4B, bi-exponential (Banister/Busso):

```
ReadinessCost(t) = A·e^(−k_fast·t) + B·e^(−k_slow·max(0, t − t_delay))
```

- **Seed chính là stimulus của session** (`MuscleStimulus.stimulus`) — tức là Layer-B pipeline ở §2.
  Engine này **không tự đọc set**, chỉ nhận seed.
- `A = stimulus × (1 − structuralFraction)`, `B = stimulus × structuralFraction × exerciseCost`,
  `slowHalfLife × 1.3` nếu `recoveryRisk`.
- Profile theo nhóm cơ (`:222`): nhóm lớn 12h/42h/delay 11h/0.55; vai-core 10/30/7/0.50;
  tay-bắp chân 8/20/4/0.45.
- Cost **cộng dồn** qua các session; `FULL_COST_REF = 6.5` = ~0% recovered.
- Runtime-only, không persist, không schema change.

→ Mọi sai lệch ở §2 được **nhân đôi** ở đây: seed sai → A và B cùng sai → cộng dồn nhiều ngày.

---

## 4. PO Engine đang đọc dữ liệu thế nào

`domain/ProgressiveOverload.kt` — **đang bị freeze theo README** (*"Do not refactor, simplify, rename,
optimize, or otherwise touch progression code … unless the task is specifically about the progression
engine."*). 152 unit test đang pass.

**`evalSession()` (`:2196-2217`)** — trái tim của việc chọn tạ:

```kotlin
val main = modeWeight(validSets.map { it.weight })          // tạ xuất hiện nhiều nhất, tie → nặng nhất
val working = validSets.filter { abs(set.weight - main) < WEIGHT_EPSILON }
// set nhẹ hơn main: trước firstMain → ramp++, sau lastMain → backoff++
```

- `modeWeight` (`:2285`): đếm tần suất, tie → lấy lớn nhất.
- Chỉ có đúng 2 nhóm bị loại: **ramp** (nhẹ hơn, đứng trước) và **back-off** (nhẹ hơn, đứng sau).
- **Set nặng hơn main thì không bị đếm vào đâu cả** — `if (!isMain(set) && set.weight < main)`.
  Một top single nặng hơn mode bị "bốc hơi" khỏi classification.

**`metrics()` (`:2238-2261`)**:

- `enoughSets = workingSets.size >= targetSets`
- `allSetsAtLeastMin = enough && working.all { it.reps >= minRep }`
- `allSetsAtTopRange = enough && working.all { it.reps >= maxRep }`
- `tooHeavy = avgReps < minRep × 0.70`, `severe = avgReps < minRep × 0.50`
- `mostlyFailed = failed × 2 >= working.size` với `failed = count { rir <= 0 }`

**`hypertrophyStimulus()` (`:1492`)**: avg effective reps = mean của `max(0, 5 − RIR)` trên
`workingSets`, cần ≥ `QUALITY_MIN_SETS = 3`. Chỉ ảnh hưởng wording/confidence, không phải action.

**Không tồn tại warmup flag.** README:720 và :753 thừa nhận: *"there is no warm-up/working-set flag, so
the first `targetSets` logged sets are treated as working sets"* và *"positional ramp/back-off
classification is a heuristic"*. Comment tại `ProgressiveOverload.kt:1377` nói *"already-warmup-filtered
working sets"* — đó là **kỳ vọng, không phải sự thật hiện tại**.

---

## 5. Khoảng cách chính xác thực tế (suy ra trực tiếp từ code trên)

Đánh dấu **[chắc chắn]** = đọc trực tiếp từ code; **[suy luận]** = hệ quả tất yếu nhưng chưa có test
chứng minh.

1. **[chắc chắn]** Warmup / dropset / rest-pause hoàn toàn vô hình với cả 3 engine. Không có cột DB,
   không có tham số truyền vào. `LoggedSetEntity` và `SetInput` đều không có chỗ chứa.

2. **[chắc chắn — nghiêm trọng nhất]** Rest-pause cùng mức tạ làm hỏng `evalSession`.
   Continuation row copy nguyên `weight` của base → lọt vào `workingSets`. Với target 3 set và một
   cluster rest-pause 8/4/3 reps: `workingSets.size` = 5 (thay vì 3) và `avgReps` rơi xuống
   ≈ 5 trong khi `minRep` = 8 → `tooHeavy = avgReps < minRep × 0.70` bật → PO trả
   **DECREASE_WEIGHT**. Một kỹ thuật chủ đích bị đọc thành "tạ quá nặng".

3. **[chắc chắn]** `modeWeight` có thể chọn sai tạ khi warmup hoặc drop đông hơn working set.
   - 3 warmup @20 + 2 working @60 → counts {20:3, 60:2} → `mainWeight = 20kg`; hai set 60kg **nặng
     hơn main nên không được xếp vào đâu**, biến mất khỏi phân tích.
   - 3 working @60 + 4 continuation drop @40 → `mainWeight = 40kg` → PO tư vấn trên mức tạ drop.

4. **[chắc chắn]** Heat map đếm mọi set `reps > 0` là 1.0 set-equivalent.
   3 warmup + 3 working + 2 drop = **8** set-equivalent thay vì 3. Không có hệ số giảm cho warmup,
   không có gộp cho rest-pause cluster.

5. **[chắc chắn]** `avgRir` trong Layer A lấy trung bình trên **mọi** set hợp lệ, nên warmup (RIR 4–5)
   kéo `qualityModifier` xuống (0.40) — khối lượng thật sự chất lượng bị pha loãng.

6. **[chắc chắn]** Dropset có bản chất là RIR 0 → kéo `grindShare` lên ≥ 0.5 rất nhanh →
   `RECOVERY_RISK` giả, và `recoveryRisk` lại kéo dài thêm 30% half-life cấu trúc trong
   Muscle Recovery. Hai sai lệch nhân nhau.

7. **[chắc chắn]** Volume (`weight × reps`, dùng cho trend label và hiển thị session) cộng cả warmup
   lẫn drop → con số volume/PR-ish trên màn hình vượt quá công việc thật sự.

8. **[chắc chắn — ít nghiêm trọng]** PR detection (`GymRepository.kt:499-503`) đi tuần tự từng row và
   cập nhật `bestWeight`/`bestReps` khi đi, nên warmup hay drop continuation thường không tạo PR giả
   (chúng nhẹ hơn/ít reps hơn). Không cần ưu tiên.

---

## 6. Ràng buộc phải nhớ khi động vào

- `domain/ProgressiveOverload.kt` + `ProgressiveOverloadEngineTest.kt` **đang freeze** (README:33-35).
  Mọi sửa đổi cần task mang tên rõ ràng là về progression engine.
- Mọi sửa đổi liên quan đến việc lưu set type sẽ chạm **Room schema** (hiện v11) — cần migration cộng
  thêm (additive ALTER) theo đúng pattern đã dùng cho `durationSeconds`/`canonicalExerciseKey`.
- Export/import JSON (`GymRepository.kt:60-90`) đang serialize/deserialize `LoggedSetEntity` bằng tay —
  thêm cột mới phải sửa cả 2 hàm `toJson`/`fromJson` nếu không sẽ mất dữ liệu khi round-trip.
- Draft persistence (`SetDraft`) **cố ý bỏ** `setType` (*"Draft persistence intentionally omits it"*,
  `LogWorkoutViewModel.kt:61`) — nên nếu persist set type trên DB thì vẫn cần quyết định riêng cho
  draft.
