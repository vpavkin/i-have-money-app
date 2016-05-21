CREATE TABLE IF NOT EXISTS public.money (
  fortune_id VARCHAR(255) NOT NULL,
  currency   VARCHAR(255) NOT NULL,
  amount     NUMERIC      NOT NULL,
  PRIMARY KEY (fortune_id, currency)
);

CREATE TABLE IF NOT EXISTS public.stocks (
  asset_id   VARCHAR(255) NOT NULL PRIMARY KEY,
  fortune_id VARCHAR(255) NOT NULL,
  name       VARCHAR(255) NOT NULL,
  currency   VARCHAR(255) NOT NULL,
  price      NUMERIC      NOT NULL,
  count      NUMERIC      NOT NULL
);

CREATE TABLE IF NOT EXISTS public.realestate (
  asset_id   VARCHAR(255) NOT NULL PRIMARY KEY,
  fortune_id VARCHAR(255) NOT NULL,
  name       VARCHAR(255) NOT NULL,
  currency   VARCHAR(255) NOT NULL,
  price      NUMERIC      NOT NULL
);

CREATE TABLE IF NOT EXISTS public.debts (
  liability_id  VARCHAR(255) NOT NULL PRIMARY KEY,
  fortune_id    VARCHAR(255) NOT NULL,
  name          VARCHAR(255) NOT NULL,
  currency      VARCHAR(255) NOT NULL,
  amount        NUMERIC      NOT NULL,
  interest_rate NUMERIC
);
