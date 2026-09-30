ALTER TABLE bulk_order_job_items
    ADD COLUMN request_payload TEXT;

UPDATE bulk_order_job_items
SET request_payload =
    '{"migration":"12F","message":"Historical item created before request payload persistence"}'
WHERE request_payload IS NULL;

ALTER TABLE bulk_order_job_items
    ALTER COLUMN request_payload SET NOT NULL;
