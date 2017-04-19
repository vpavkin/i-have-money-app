#!/usr/bin/env bash
docker run -i -P \
-e ihavemoney_writeback_db_user=admin \
-e ihavemoney_writeback_db_password=changeit \
-e ihavemoney_writeback_db_host=127.0.0.1 \
-e ihavemoney_writeback_db_port=5432 \
-e ihavemoney_writeback_db_name=ihavemoney-write \
-e ihavemoney_writeback_host=127.0.0.1 \
-e ihavemoney_writeback_port=9101 \
-e ihavemoney_writefront_host=127.0.0.1 \
-e ihavemoney_writefront_http_port=8101 \
-e ihavemoney_writeback_smtp_user=example@gmail.com \
-e ihavemoney_writeback_smtp_password=changeit \
--name writeback -a stdin vpavkin/ihavemoney-write-backend
