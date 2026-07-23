-- Adds a single boolean flag to price_snapshot so listings backed by
-- a financed carrier installment (e.g. "$10.75/mo with $791 down for 24
-- months") can be tagged for the UI to display a "Financed" badge.
--
-- The headline numeric price column is unchanged; the LLM is responsible
-- for returning the total cost (down payment + monthly * term_months) as
-- the price value when financed = true.
--
-- Idempotent: safe to re-run on a database that already has this column.
--
-- NOTE: component fields (monthly_price, down_payment, term_months) were
-- intentionally deferred. Add them in a V2 migration only when the UI
-- actually needs to break the total apart for display.

ALTER TABLE price_snapshots
  ADD COLUMN IF NOT EXISTS financed boolean NOT NULL DEFAULT false;
