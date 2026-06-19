-- Speed up user-dashboard booking lookups and service resolution for dashboard requests.
ALTER TABLE vendor_booking
    ADD INDEX idx_vbkg_user_created (user_id, created_at);

ALTER TABLE vendor_service
    ADD INDEX idx_vsvc_type_status_updated (service_type, status, updated_at);
