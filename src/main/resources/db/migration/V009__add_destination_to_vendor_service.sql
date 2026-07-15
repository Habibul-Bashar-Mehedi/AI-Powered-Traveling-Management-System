-- =============================================================================
-- Migration V009: Optional Destination link on vendor_service, so vendor
-- bookings (HOTEL_ROOM/TOUR_PACKAGE/TRANSPORT_ROUTE) can be grouped by
-- destination the same way direct hotel bookings already are via hotels.destination_id.
-- =============================================================================

ALTER TABLE vendor_service
    ADD COLUMN destination_id BIGINT NULL,
    ADD CONSTRAINT fk_vendor_service_destination FOREIGN KEY (destination_id) REFERENCES destination(id),
    ADD INDEX idx_vendor_service_destination (destination_id);
