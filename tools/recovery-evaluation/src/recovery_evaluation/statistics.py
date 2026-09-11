from __future__ import annotations

import math
import random
from collections import defaultdict
from typing import Callable, Iterable


def _sigmoid(value: float) -> float:
    value = max(-35.0, min(35.0, value))
    return 1.0 / (1.0 + math.exp(-value))


def _solve(matrix: list[list[float]], vector: list[float]) -> list[float]:
    n = len(vector)
    augmented = [matrix[i][:] + [vector[i]] for i in range(n)]
    for column in range(n):
        pivot = max(range(column, n), key=lambda row: abs(augmented[row][column]))
        if abs(augmented[pivot][column]) < 1e-12:
            augmented[pivot][column] += 1e-8
        augmented[column], augmented[pivot] = augmented[pivot], augmented[column]
        divisor = augmented[column][column]
        for item in range(column, n + 1):
            augmented[column][item] /= divisor
        for row in range(n):
            if row == column:
                continue
            factor = augmented[row][column]
            if factor == 0.0:
                continue
            for item in range(column, n + 1):
                augmented[row][item] -= factor * augmented[column][item]
    return [augmented[row][n] for row in range(n)]


class RidgeLogistic:
    def __init__(self, l2_penalty: float = 1.0, max_iterations: int = 200, tolerance: float = 1e-10):
        self.l2_penalty = l2_penalty
        self.max_iterations = max_iterations
        self.tolerance = tolerance
        self.weights: list[float] = []

    def fit(self, rows: list[list[float]], outcomes: list[int]) -> "RidgeLogistic":
        if not rows or len(rows) != len(outcomes):
            raise ValueError("Training rows and outcomes must be non-empty and aligned")
        width = len(rows[0]) + 1
        design = [[1.0] + row for row in rows]
        weights = [0.0] * width
        prior = (sum(outcomes) + 1.0) / (len(outcomes) + 2.0)
        weights[0] = math.log(prior / (1.0 - prior))
        for _ in range(self.max_iterations):
            probabilities = [_sigmoid(sum(w * x for w, x in zip(weights, row))) for row in design]
            gradient = [0.0] * width
            hessian = [[0.0] * width for _ in range(width)]
            for row, outcome, probability in zip(design, outcomes, probabilities):
                residual = outcome - probability
                variance = max(1e-8, probability * (1.0 - probability))
                for i in range(width):
                    gradient[i] += row[i] * residual
                    for j in range(width):
                        hessian[i][j] += row[i] * variance * row[j]
            for i in range(1, width):
                gradient[i] -= self.l2_penalty * weights[i]
                hessian[i][i] += self.l2_penalty
            hessian[0][0] += 1e-8
            delta = _solve(hessian, gradient)
            weights = [weight + change for weight, change in zip(weights, delta)]
            if max(abs(change) for change in delta) < self.tolerance:
                break
        self.weights = weights
        return self

    def predict(self, rows: list[list[float]]) -> list[float]:
        if not self.weights:
            raise ValueError("Model is not fitted")
        return [_sigmoid(self.weights[0] + sum(w * x for w, x in zip(self.weights[1:], row))) for row in rows]


class FoldPreprocessor:
    def __init__(self):
        self.means: list[float] = []
        self.scales: list[float] = []

    def fit(self, rows: list[list[float | None]]) -> "FoldPreprocessor":
        width = len(rows[0]) if rows else 0
        self.means = []
        self.scales = []
        for column in range(width):
            observed = [float(row[column]) for row in rows if row[column] is not None]
            mean = sum(observed) / len(observed) if observed else 0.0
            variance = sum((value - mean) ** 2 for value in observed) / len(observed) if observed else 0.0
            self.means.append(mean)
            self.scales.append(math.sqrt(variance) if variance > 1e-12 else 1.0)
        return self

    def transform(self, rows: list[list[float | None]]) -> list[list[float]]:
        transformed: list[list[float]] = []
        for row in rows:
            values: list[float] = []
            for index, value in enumerate(row):
                missing = value is None
                numeric = self.means[index] if missing else float(value)
                values.append((numeric - self.means[index]) / self.scales[index])
                values.append(1.0 if missing else 0.0)
            transformed.append(values)
        return transformed


def brier_score(outcomes: list[int], probabilities: list[float]) -> float | None:
    if not outcomes:
        return None
    return sum((outcome - probability) ** 2 for outcome, probability in zip(outcomes, probabilities)) / len(outcomes)


def log_loss(outcomes: list[int], probabilities: list[float]) -> float | None:
    if not outcomes:
        return None
    epsilon = 1e-15
    return -sum(
        outcome * math.log(max(epsilon, min(1.0 - epsilon, probability)))
        + (1 - outcome) * math.log(max(epsilon, min(1.0 - epsilon, 1.0 - probability)))
        for outcome, probability in zip(outcomes, probabilities)
    ) / len(outcomes)


def auroc(outcomes: list[int], scores: list[float]) -> float | None:
    positives = sum(outcomes)
    negatives = len(outcomes) - positives
    if positives == 0 or negatives == 0:
        return None
    ordered = sorted(zip(scores, outcomes), key=lambda item: item[0])
    rank_sum = 0.0
    index = 0
    while index < len(ordered):
        end = index + 1
        while end < len(ordered) and ordered[end][0] == ordered[index][0]:
            end += 1
        average_rank = (index + 1 + end) / 2.0
        rank_sum += average_rank * sum(outcome for _, outcome in ordered[index:end])
        index = end
    return (rank_sum - positives * (positives + 1) / 2.0) / (positives * negatives)


def pr_auc(outcomes: list[int], scores: list[float]) -> float | None:
    positives = sum(outcomes)
    if positives == 0:
        return None
    ordered = sorted(zip(scores, outcomes), key=lambda item: (-item[0], -item[1]))
    true_positives = 0
    false_positives = 0
    area = 0.0
    previous_recall = 0.0
    index = 0
    while index < len(ordered):
        end = index + 1
        while end < len(ordered) and ordered[end][0] == ordered[index][0]:
            end += 1
        for _, outcome in ordered[index:end]:
            if outcome:
                true_positives += 1
            else:
                false_positives += 1
        recall = true_positives / positives
        precision = true_positives / (true_positives + false_positives)
        area += (recall - previous_recall) * precision
        previous_recall = recall
        index = end
    return area


def calibration_summary(outcomes: list[int], probabilities: list[float]) -> dict[str, object]:
    if not outcomes or sum(outcomes) in (0, len(outcomes)):
        return {"intercept": None, "slope": None, "reason": "INSUFFICIENT_CLASSES"}
    logits = [[math.log(max(1e-12, min(1 - 1e-12, p)) / max(1e-12, 1 - min(1 - 1e-12, p)))] for p in probabilities]
    model = RidgeLogistic(l2_penalty=1e-8).fit(logits, outcomes)
    return {"intercept": model.weights[0], "slope": model.weights[1], "reason": None}


def reliability_table(outcomes: list[int], probabilities: list[float], bins: int = 10) -> list[dict[str, object]]:
    result = []
    for index in range(bins):
        lower = index / bins
        upper = (index + 1) / bins
        members = [
            (outcome, probability)
            for outcome, probability in zip(outcomes, probabilities)
            if lower <= probability < upper or (index == bins - 1 and probability == 1.0)
        ]
        result.append({
            "bin": index + 1,
            "lower": lower,
            "upper": upper,
            "count": len(members),
            "mean_probability": sum(p for _, p in members) / len(members) if members else None,
            "attainment_rate": sum(y for y, _ in members) / len(members) if members else None,
        })
    return result


def quantile(values: list[float], probability: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    position = (len(ordered) - 1) * probability
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    fraction = position - lower
    return ordered[lower] * (1.0 - fraction) + ordered[upper] * fraction


def moving_block_bootstrap(
    rows: list[dict[str, object]],
    metric: Callable[[list[dict[str, object]]], float | None],
    seed: int,
    replicates: int,
    block_length: int,
) -> dict[str, object]:
    by_workout: dict[str, list[dict[str, object]]] = defaultdict(list)
    ordered_workouts: list[str] = []
    for row in rows:
        workout = str(row["workout_id"])
        if workout not in by_workout:
            ordered_workouts.append(workout)
        by_workout[workout].append(row)
    point = metric(rows)
    if point is None or not ordered_workouts:
        return {"point": point, "lower": None, "upper": None, "replicates": 0}
    rng = random.Random(seed)
    values: list[float] = []
    n = len(ordered_workouts)
    length = min(max(1, block_length), n)
    for _ in range(replicates):
        selected: list[str] = []
        while len(selected) < n:
            start = rng.randrange(n)
            selected.extend(ordered_workouts[(start + offset) % n] for offset in range(length))
        sampled: list[dict[str, object]] = []
        for workout in selected[:n]:
            sampled.extend(by_workout[workout])
        value = metric(sampled)
        if value is not None and math.isfinite(value):
            values.append(value)
    return {
        "point": point,
        "lower": quantile(values, 0.025),
        "upper": quantile(values, 0.975),
        "replicates": len(values),
    }

