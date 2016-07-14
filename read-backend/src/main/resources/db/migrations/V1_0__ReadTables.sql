DROP TABLE IF EXISTS public.money;
CREATE TABLE IF NOT EXISTS public.money (
  fortune_id VARCHAR(255) NOT NULL,
  currency   VARCHAR(255) NOT NULL,
  amount     NUMERIC      NOT NULL,
  PRIMARY KEY (fortune_id, currency)
);

DROP TABLE IF EXISTS public.assets;
CREATE TABLE IF NOT EXISTS public.assets (
  asset_id   VARCHAR(255) NOT NULL PRIMARY KEY,
  fortune_id VARCHAR(255) NOT NULL,
  name       VARCHAR(255) NOT NULL,
  currency   VARCHAR(255) NOT NULL,
  price      NUMERIC      NOT NULL,
  count      NUMERIC      NOT NULL
);

DROP TABLE IF EXISTS public.debts;
CREATE TABLE IF NOT EXISTS public.debts (
  liability_id  VARCHAR(255) NOT NULL PRIMARY KEY,
  fortune_id    VARCHAR(255) NOT NULL,
  name          VARCHAR(255) NOT NULL,
  currency      VARCHAR(255) NOT NULL,
  amount        NUMERIC      NOT NULL,
  interest_rate NUMERIC
);
