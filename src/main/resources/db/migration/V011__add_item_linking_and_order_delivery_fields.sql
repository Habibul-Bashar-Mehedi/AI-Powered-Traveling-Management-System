-- =============================================================================
-- Migration V011:
--   1. Optional VendorService link on traditionalItems, mirroring the existing
--      traditional_foods.linked_service_id — lets an admin mark a traditional
--      item as orderable through the same VendorService/VendorBooking flow.
--   2. Optional delivery address / contact phone on vendor_booking, so
--      traditional food/item orders (which need to be delivered somewhere)
--      can capture where to send the order and who to contact.
-- =============================================================================

ALTER TABLE traditionalItems
    ADD COLUMN linked_service_id BINARY(16) NULL,
    ADD CONSTRAINT fk_traditional_item_linked_service FOREIGN KEY (linked_service_id) REFERENCES vendor_service(service_id),
    ADD INDEX idx_traditional_item_linked_service (linked_service_id);

ALTER TABLE vendor_booking
    ADD COLUMN delivery_address VARCHAR(500) NULL,
    ADD COLUMN contact_phone VARCHAR(30) NULL;
