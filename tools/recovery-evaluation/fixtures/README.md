# Synthetic fixtures

Run `python fixtures/generate_fixtures.py`. The generator creates deterministic, non-private
WEARABLE-1A-shaped exports for sufficient positive signal, no incremental signal, inverted signal,
insufficient data, payload-integrity failure, future leakage, heavy missingness, first-set ambiguity,
multiple exercises per workout, and incompatible model versions. Generated JSON and evaluator
outputs are ignored; the source generator is the committed fixture definition.
