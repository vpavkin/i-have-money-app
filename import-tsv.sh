#!/usr/bin/env bash
curl --form "transactions=@test.tsv" \
-H "Authorization:Bearer [token]" \
-H "Content-Type: multipart/form-data; charset=UTF-8" \
http://localhost:8101/fortune/[fortune-id]/import
