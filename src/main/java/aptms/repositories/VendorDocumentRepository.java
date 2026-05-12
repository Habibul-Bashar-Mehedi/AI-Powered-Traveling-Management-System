package aptms.repositories;

import aptms.entities.Vendor;
import aptms.entities.VendorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {
    List<VendorDocument> findByVendor(Vendor vendor);
    List<VendorDocument> findByVendorVendorId(UUID vendorId);
}

