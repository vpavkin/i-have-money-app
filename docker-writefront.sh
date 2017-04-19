#!/usr/bin/env bash
docker run -i -P \
-e ihavemoney_writeback_host=127.0.0.1 \
-e ihavemoney_writeback_port=9101 \
-e ihavemoney_writefront_host=127.0.0.1 \
-e ihavemoney_writefront_http_port=8101 \
-e ihavemoney_writefront_tcp_port=10101 \
-e ihavemoney_secret_key=changeit \
--name writefront -a stdin vpavkin/ihavemoney-write-frontend
