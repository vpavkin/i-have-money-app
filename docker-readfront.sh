#!/usr/bin/env bash
docker run -i -P \
-e ihavemoney_readback_host=127.0.0.1 \
-e ihavemoney_readback_port=9201 \
-e ihavemoney_readfront_host=127.0.0.1 \
-e ihavemoney_readfront_http_port=8201 \
-e ihavemoney_readfront_tcp_port=10201 \
-e ihavemoney_writefront_host=127.0.0.1 \
-e ihavemoney_writefront_port=8101 \
--name readfront -a stdin vpavkin/ihavemoney-read-frontend
